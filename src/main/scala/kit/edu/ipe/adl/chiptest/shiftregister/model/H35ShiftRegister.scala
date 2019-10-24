package kit.edu.ipe.adl.chiptest.shiftregister.model

import com.idyria.osi.ooxoo.core.buffers.structural.xelement
import com.idyria.osi.tea.listeners.ListeningSupport
import kit.ipe.adl.rfg3.language.RegisterFileHost
import kit.ipe.adl.rfg3.language.RFLanguage
import kit.ipe.adl.rfg3.model.Field
import scala.language.dynamics

/**
 * @author zm4632/mf1049#
 *
 */
@xelement(name = "H35ShiftRegister")
class H35ShiftRegister(val si_chip: Field, val clk1_chip: Field, val clk2_chip: Field, val ld_chip: Field, val target: RegisterFileHost) extends SRCommon with RFLanguage with ListeningSupport with Dynamic {

  var repeatWrites = 3

  // Dynamic select
  //-------------
  def updateDynamic(name: String)(v: Int) : ShiftRegisterTraitRegister = {

    this.registers.find { r => r.name.toString == name } match {
      case None => throw new RuntimeException(s"Cannot udpate ShiftRegister reg $name, not in the XML description ")
      case Some(r) => 
        r.setMemory(v.toDouble)
        r
    }

  }
  
  def updateBoolean(name: String)(v: Boolean) : ShiftRegisterTraitRegister  = {

    this.registers.find { r => r.name.toString == name } match {
      case None => throw new RuntimeException(s"Cannot udpate ShiftRegister reg $name, not in the XML description ")
      case Some(r) if (v) => 
        r.setMemory(1.toDouble)
        r
      case Some(r) if (!v) =>
        r.setMemory(0.toDouble)
        r
    }

  }

  def sendBit(bit: Boolean) = {

    var it = 0

    if (bit) {
      si_chip.setMemory(1)
    } else {
      si_chip.setMemory(0)
    }

    for (it <- 0 to repeatWrites) {
      si_chip.parentRegister.write
    }

    //   println(s"Sending bit: $bit, field value is ${si.value}")

    for (it <- 0 to repeatWrites) {
      clk1_chip.setMemory(1)
      clk1_chip.parentRegister.write
      //clk2_chip.setMemory(1)
      //clk2_chip.parentRegister.write
    }

    for (it <- 0 to repeatWrites) {
      clk1_chip.setMemory(0)
      clk1_chip.parentRegister.write
      //clk2_chip.setMemory(0)
      //clk2_chip.parentRegister.write
    }

    for (it <- 0 to repeatWrites) {
      //clk1_chip.setMemory(1)
      //clk1_chip.parentRegister.write
      clk2_chip.setMemory(1)
      clk2_chip.parentRegister.write
    }

    for (it <- 0 to repeatWrites) {
      //clk1_chip.setMemory(0)
      //clk1_chip.parentRegister.write
      clk2_chip.setMemory(0)
      clk2_chip.parentRegister.write
    }

  } //end of sendBit

  def LoadDAC() = {
    var i = 0;
    ld_chip.setMemory(0)
    ld_chip.parentRegister.write
    ld_chip.setMemory(0)
    ld_chip.parentRegister.write
    for (i <- 0 to 7) {
      ld_chip.setMemory(1)
      ld_chip.parentRegister.write
    }
    ld_chip.setMemory(0)
    ld_chip.parentRegister.write
    ld_chip.setMemory(0)
    ld_chip.parentRegister.write

  } //end of LoadDAC

  def sendDACValue(DACvalue: Int, spare: Boolean) = {
    var it = 0;

    sendBit(spare)
    for (it <- 0 to 5) {
      sendBit((DACvalue & (0x01 << (it))) != 0x00)

    }

  } //end of SendDACValue

  // Outputs
  //--------------

  /**
   * Program the SR using the direct pin interface
   */
  override def program = {

    // Registers are either
    // First defined is the first one to be send, it should be the last one in the SR
    // First defined is the first one in SR, hence the last one to be send
    // attribute is called "reversed" to reverse registers vefore sending
    val targetRegisters = this.findAttribute("sr.reverse") match {
      case Some(v) => registers.reverse 
      case None => registers
    }
    
    
    
    onBuffering(target) {

      var total = targetRegisters.size

     targetRegisters.zipWithIndex.foreach {
        case (reg, i) =>
          
          // Send DAC if type DAC
          reg.findAttribute("sr.dac") match {
            case Some(isDAC) =>
              println("Sending dac : "+reg.name+" -> "+reg.getMemory)
              sendDACValue(reg.getMemory.toInt, false)
            case None => 

              //-- Per default, send MSB first
              //-- Get size from attribute or determine from fields
              //var regSize = reg.findAttributeLong("sr.size").get.toInt
              val regSize = reg.findAttributeLong("sr.size") match {
                case Some(size) => 
                  size.toLong
                case other => 
                  var s = reg.fields.map( f => f.width.data.toLong)
                  s.sum
              }
              
              
              for (it <- (regSize-1) to 0 by -1 ) {
                var v = ((reg.getMemory.toLong >> it) & (0x01))
                sendBit(v != 0x00)
                println(s"Non dac, sending for $it: "+v)
              }

              /*for (it <- 0 to (regSize-1)) {
                sendBit((reg.getMemory.toLong & (0x01 << (it))) != 0x00)
              
              }*/
              
              
          }
          
          
          this.@->("progress", (i.toDouble / (total - 1).toDouble))
          println(s"index i: $i")
          println(s"total: $total")

      }
      LoadDAC()

    }
  }

  /**
   * Returns the total number of bits in the SR
   */
  def getNumberOfBits = {

    this.registers.map {

      case reg if(reg.findAttribute("sr.firstSpare").isDefined) =>

        reg.findAttributeLong("sr.size").get.toInt +1

      case reg =>

        reg.findAttributeLong("sr.size").get.toInt

    }.sum
  }
  
  /**
   * Add all the bits to a set of bytes and create an HEX String for it
   */
  def createByteString = {

    var bytes = List[Byte]()

    //-- First
    var currentByte: Byte = 0
    var currentBit = 0

    //-- Got through registers (first one is the last one in chain)
    this.registers.zipWithIndex.foreach {
      case (reg, i) =>

        // Get reg value
        var valDouble = reg.getMemory
        var value = valDouble.toByte

        var regSize = reg.findAttributeLong("sr.size").get.toInt

        // Convert Value to list of bits padded to the size of register
        var valueBits = value.toBinaryString.toCharArray().toList
        var padding = (0 until (regSize - valueBits.size)).toList.map {
          i => '0'
        }.toList
        var valueBitsPaddedLeft = (padding ::: valueBits).map { c => Integer.parseInt(c.toString)}.toList

        
        
        // add SPare bit if needed
        reg.findAttribute("sr.firstSpare") match {
          case Some(_) =>
            valueBitsPaddedLeft = valueBitsPaddedLeft :+  0
          case None =>

        }
        
        //-- The string has as first element the MSB
        //-- valueBitsPaddedLeft is thus MSB First, reverse to LSB first if no MSBFirst is specified
        valueBitsPaddedLeft = reg.findAttribute("sr.MSBFirst") match {
          // Found means we keep data because already MSB first
          case Some(found) => valueBitsPaddedLeft
          
          // None means we have to use LSB first
          case None => valueBitsPaddedLeft.reverse
        }

        //println("Writing reg: " + reg.name + " value: " + reg.getMemory + " -> " + valueBitsPaddedLeft)


        // MSB/LSB first is solved above in code
        valueBitsPaddedLeft.foreach {
          newBit =>

            // Change byte if necessary
            currentBit match {

              // 8 -> Change byte
              case 8 =>
                
                // Save the byte
                bytes = bytes :+ currentByte
                
                // Create new byte
                currentByte = newBit.toByte

                currentBit = 1

              // 7, set bit and if one, reverse bits to fix sign change
              /*case 7 =>
              
              ((value >> bitIndex) & 0x1) match {
                case 1 => 
                   currentByte = ((currentByte | (1 <<7)).toByte & 0xff).toByte
                case 0 => 
                   
              }
              
              currentBit += 1*/

              // Others: normal operation
              case _ =>
                
                //println("Shifting "+newBit.toByte+" to "+currentBit)
                currentByte = (currentByte | (newBit.toByte << currentBit) ).toByte

                // println(s"-- Current bit: $currentBit -> ${(value >> bitIndex) & 0x1}, byte now $currentByte")

                currentBit += 1

            }

        }

    }

    // Save last currentByte
    bytes = bytes :+ currentByte

    // Merge all to hex, don't reverse bytes to have the first registers on the left of the string (LSB first)
    var mappedByte = bytes.map { b => b.toInt.toHexString.takeRight(2) }.map {
      case str if (str.length() == 1) => s"0$str"
      case str => str
    }

    // Output reversed or not to match the byte ordering on the target
    mappedByte.mkString
  }

}

object H35ShiftRegister {

  def apply(url: java.net.URL, si_chip: Field, clk1_chip: Field, clk2_chip: Field, ld_chip: Field, target: RegisterFileHost) = {

    // Instanciate
    var res = new H35ShiftRegister(si_chip, clk1_chip, clk2_chip, ld_chip, target)

    // Set Stax Parser and streamIn
    var io = com.idyria.osi.ooxoo.core.buffers.structural.io.sax.StAXIOBuffer(url)
    res.appendBuffer(io)
    io.streamIn

    // Return
    res

  }

  def apply(url: java.net.URL) = {

    // Instanciate
    var res = new H35ShiftRegister(null, null, null, null, null)

    // Set Stax Parser and streamIn
    var io = com.idyria.osi.ooxoo.core.buffers.structural.io.sax.StAXIOBuffer(url)
    res.appendBuffer(io)
    io.streamIn

    // Return
    res

  }

}