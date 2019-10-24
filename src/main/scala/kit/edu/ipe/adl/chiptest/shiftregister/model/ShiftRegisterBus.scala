package kit.edu.ipe.adl.chiptest.shiftregister.model

import com.idyria.osi.ooxoo.core.buffers.structural.xelement
import com.idyria.osi.tea.listeners.ListeningSupport
import kit.ipe.adl.rfg3.model.Register
import kit.ipe.adl.rfg3.language.RFLanguage
import kit.ipe.adl.rfg3.model.Field


/**
 * ShiftRegisterBus base class to drive a SR bus.
 * The constructor requires one Register which will contain a "clk","ld", and some "si_{i}" fields.
 * The number of required si_{i} fields is based on the number of ShiftRegister defined in the SRBus class
 */
@xelement(name = "ShiftRegisterBus")
class ShiftRegisterBus(val srbus: Register) extends  SRCommon with ShiftRegisterBusTrait with RFLanguage with ListeningSupport {

  val clk = srbus.clk
  val ld = srbus.ld

  
  /**
   * Search accross all SRs
   */
  def getRegisterValue(str:String) = {
    
    getRegister(str).getMemory
    
  }
  
  /**
   * 
   */
  def getRegister(str:String) = {
    
    // Get SR Name and reg name
    var (srName,regName) = str.trim.split("\\.") match {
      case arr if (arr.length==2) => (arr(0),arr(1))
      case _ =>  throw new IllegalArgumentException(s"SRBus register format must be ShiftRegister.Register: $str")
    }
    
    // get SR and then Register
    this.shiftRegisters.find(_.name.toString==srName) match {
      case Some(sr) => 
          sr.registers.find { _.name.toString==regName } match {
            case Some(reg) => reg
            case None => throw new IllegalArgumentException(s"Could not find register $regName in SR $srName, check names in code or XML")
          }
      case None => 
        throw new IllegalArgumentException(s"Could not find SR $srName in SRBus definition, check names in code or XML")
    }
    
    
  }
  
  /**
   * Checks:
   *  - The interface register must have enough si_{i} for each SR in the XML
   *  - The
   */
  def check = {

    // Check all the si_{i} are present
    //---------------
    (0 until this.shiftRegisters.size) foreach {
      i =>
        srbus.fields.find(_.name.equals(s"si_$i")) match {
          case None =>
            throw new IllegalArgumentException(s"SRBus contains ${this.shiftRegisters.size} SR, each must have an si_{i} field in the provided SRBus register interface. Could not find field named si_$i")
          case _ =>
        }
    }

    // Check all the ShiftRegister have the same size
    //----------
    this.shiftRegisters.map {
      sr => sr.registers.size
    }.distinct.size match {
      case 1 =>
      case _ => throw new IllegalArgumentException("Not All the ShiftRegisters defined in XML have the same number of registers")
    }

  }
  
  def toggleClock = {
    
  //   println(s"Sending bit: $bit, field value is ${si.value}")

    for (it <- 0 to 3) {
      clk.setMemory(1)
      clk.parentRegister.write
    }

    for (it <- 0 to 3) {
      clk.setMemory(0)
      clk.parentRegister.write
    }
  }
  
  def sendBit(bit: Boolean, si:Field) = {
    
    var it = 0
   
    if (bit) {
      si.setMemory(1)
      //si_1.setMemory(1)
      //si_2.setMemory(1)
    } else {
      si.setMemory(0)
      //si_1.setMemory(0)
      //si_2.setMemory(0)
    }
    /*if (bit2) {
      si_2.setMemory(1)
      //si_1.setMemory(1)
      //si_2.setMemory(1)
    } else {
      si_2.setMemory(0)
      //si_1.setMemory(0)
      //si_2.setMemory(0)
    }*/

    for (it <- 0 to 3) {
      si.parentRegister.write
      //si_1.parentRegister.write
     // si_2.parentRegister.write
    }
    
   
    // Clock toggling is done after all SI have been set
    

  } //end of sendBit
  
  def sendDACValue(values : Iterable[(Int,Field)]) = {
    
   /* values.foreach {
       case (value,si) =>
         println(s"Value for ${si.name} -> ${value}")
    }*/
    
    // send 14-Bit to PCBDAC
    for (a <- 0 to 13 ) {
      
      // Send bit for each SI
      values.foreach {
        case (value,si) =>
          sendBit((value & (0x01 << (13 - a))) != 0x00,si)
      }
      
      // Toggle clock for all
      toggleClock
    }
    
    // Send Spare bits
    for (a <- 0 until 2 ) {
      values.foreach {
        case (value,si) =>
          sendBit(false,si) //send spare bit
      }
      // Toggle clock for all
      toggleClock
    }
    
    
    /*
    values.foreach {
      case (value,si) => 
        
         var a = 0;

        // send 14-Bit to PCBDAC
        for (a <- 0 to 13 ) {
          //sendBit((value & (0x01 << (13 - a))) != 0x00, (value2 & (0x01 << (13 - a))) != 0x00)
          sendBit((value & (0x01 << (13 - a))) != 0x00,si)
        }
        sendBit(false,si) //send spare bit
        sendBit(false,si) // send spare bit
        
        
    }
    
    toggleClock*/
    
   
  } //end of SendDACValue

  def setAmplitude(values : Iterable[(Double,Field)]) = {

    var converted = values.map {
      case (value,si) => ( ((16383 * value) / 3.3).toInt,si)
    }
    
    sendDACValue(converted)
    
    
   /* var volt = 0.0
    volt = (16383 * amp) / 3.3
    sendDACValue(volt.toInt, si)*/
  } //end of setAmplitude

  def LoadDAC() = {
    var i = 0;
    ld.setMemory(0)
    srbus.write
    ld.setMemory(0)
    srbus.write
    for (i <- 0 to 3) {
      ld.setMemory(1)
      srbus.write
    }
    ld.setMemory(0)
    srbus.write

  } //end of LoadDAC

  override def program = {

    onBuffering {

      // Find all Registers to make a total
      //---------------
      var total = this.shiftRegisters(0).registers.size
      
      // For each Register in all SR, set Amplitude and then toggle clock and such once
      //-------------
      (0 until total) foreach {
        i => 
          
          // Create a list of pairs: (value,field) for all SR
          // call setAmplitudes which sets all the bits and toggles the clock after bits have been set on all si wires
          var valuesAndSI = this.shiftRegisters.zipWithIndex.map {
            case (sr,si) => (sr.registers(i).getMemory.toDouble, srbus.field(s"si_$si"))
          }
          
          setAmplitude(valuesAndSI.toIterable)
          
      }
      
      // Finish with LOAD DAC
      LoadDAC()
      
      /*
      this.registers.foreach {
        reg =>
          reg.findAttribute("sr.spare")
      }

      var total = this.registers.count(_.findAttribute("sr.spare") == None)

      this.registers.filter(_.findAttribute("sr.name") == None).zipWithIndex.foreach {
        case (reg, i) =>
          //setAmplitude(reg.getMemory)
          println("Memory:" + reg.getMemory)

          setAmplitude(reg.getMemory) //DAC1
          setAmplitude(reg.getMemory) //DAC2
          setAmplitude(reg.getMemory) //DAC3
          setAmplitude(reg.getMemory) //DAC4
          setAmplitude(reg.getMemory) //DAC5
          //setAmplitude(reg.getMemory,reg.getMemory) //DAC1
          //setAmplitude(reg.getMemory,reg.getMemory) //DAC2
          //setAmplitude(reg.getMemory,reg.getMemory) //DAC3
          //setAmplitude(reg.getMemory,reg.getMemory) //DAC4
          //setAmplitude(reg.getMemory,reg.getMemory) //DAC5
          this.@->("progress", (i.toDouble / (total - 1).toDouble))
          println(s"index i: $i")
          println(s"total: $total")

      }*/

      //Voltage set from VoltageSource board 1/2
      /*setAmplitude(1.5) //DAC1
        setAmplitude(1.5) //DAC2
        setAmplitude(1.5) //DAC3
        setAmplitude(1.5) //DAC4
        setAmplitude(1.5) //DAC5*/

      

    }

  }

}

object ShiftRegisterBus {

  def apply(url: java.net.URL, srbus: Register) = {

    // Instanciate
    var res = new ShiftRegisterBus(srbus)

    // Set Stax Parser and streamIn
    var io = com.idyria.osi.ooxoo.core.buffers.structural.io.sax.StAXIOBuffer(url)
    res.appendBuffer(io)
    io.streamIn

    // Check
    res.check

    // Return
    res

  }

}