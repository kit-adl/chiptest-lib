package kit.edu.ipe.adl.chiptest.shiftregister.model

import com.idyria.osi.ooxoo.core.buffers.structural.xelement

import com.idyria.osi.tea.listeners.ListeningSupport
import kit.ipe.adl.rfg3.language.RegisterFileHost
import kit.ipe.adl.rfg3.language.RFLanguage
import kit.ipe.adl.rfg3.model.Field

/**
 * @author zm4632/mf1049#
 *
 */
@xelement(name = "ShiftRegister")
class ShiftRegister(val si: Field,  val clk : Field, val ld : Field,  val target: RegisterFileHost) extends SRCommon with RFLanguage with ListeningSupport {
 
  
  //si_1.setMemory(0)
  //si_1.parentRegister.write
  //clk.VHDCIctrl5 = 1
  //    clk.write
  //ld.VHDCIctrl4 = 0
   // ld.write
  
  def sendBit(bit: Boolean) = {
    
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
    
  //   println(s"Sending bit: $bit, field value is ${si.value}")

    for (it <- 0 to 3) {
      clk.setMemory(1)
      clk.parentRegister.write
    }

    for (it <- 0 to 3) {
      clk.setMemory(0)
      clk.parentRegister.write
    }

  } //end of sendBit

  def LoadDAC() = {
    var i = 0; 
    ld.setMemory(0)
    ld.parentRegister.write
    ld.setMemory(0) 
    ld.parentRegister.write
    for (i <- 0 to 3) {
      ld.setMemory(1)
      ld.parentRegister.write
    }
     ld.setMemory(0)
    ld.parentRegister.write

  } //end of LoadDAC

  def sendDACValue(value: Int) = {
    var a = 0;

    // send 14-Bit to PCBDAC
    for (a <- 0 to 13 ) {
      //sendBit((value & (0x01 << (13 - a))) != 0x00, (value2 & (0x01 << (13 - a))) != 0x00)
      sendBit((value & (0x01 << (13 - a))) != 0x00)
    }
    sendBit(false) //send spare bit
    sendBit(false) // send spare bit
  } //end of SendDACValue

  def setAmplitude(amp: Double) = {

    var volt = 0.0
    //var volt2 = 0.0
    
    volt = (16383 * amp) / 3.3
   // volt2 = (16383 * amp2) / 3.3
    //sendDACValue(volt.toInt, volt2.toInt)
    sendDACValue(volt.toInt)
  } //end of setAmplitude

  override def program = {
   
    onBuffering(target) {
    
    
      this.registers. foreach {
        reg => 
          reg.findAttribute("sr.spare")
      }
      
      var total = this.registers.count(_.findAttribute("sr.spare")==None)
      
      setAmplitude(0.4)
      setAmplitude(0)
      setAmplitude(0)
      setAmplitude(0)
      setAmplitude(0)
      
      this.registers.filter(_.findAttribute("sr.name")==None).zipWithIndex.foreach {
        case (reg,i) =>  
          //setAmplitude(reg.getMemory)
          println("Memory:" + reg.getMemory)
           
          //setAmplitude(reg.getMemory.toDouble) //DAC1
          //setAmplitude(1.0) //DAC1
          
          //setAmplitude(reg.getMemory) //DAC2
          //setAmplitude(reg.getMemory) //DAC3
          //setAmplitude(reg.getMemory) //DAC4
          //setAmplitude(reg.getMemory) //DAC5
          //setAmplitude(reg.getMemory,reg.getMemory) //DAC1
          //setAmplitude(reg.getMemory,reg.getMemory) //DAC2
          //setAmplitude(reg.getMemory,reg.getMemory) //DAC3
          //setAmplitude(reg.getMemory,reg.getMemory) //DAC4
          //setAmplitude(reg.getMemory,reg.getMemory) //DAC5
          this.@->("progress", (i.toDouble/(total-1).toDouble))
          println(s"index i: $i" )
          println(s"total: $total")
     
         
      }

      //Voltage set from VoltageSource board 1/2
      /*setAmplitude(1.5) //DAC1
        setAmplitude(1.5) //DAC2
        setAmplitude(1.5) //DAC3
        setAmplitude(1.5) //DAC4
        setAmplitude(1.5) //DAC5*/
      LoadDAC()

    }
  }
}

object ShiftRegister {

  def apply(url: java.net.URL, si : Field, clk : Field, ld : Field, target: RegisterFileHost) = {

    // Instanciate
    var res = new ShiftRegister(si, clk, ld, target)

    // Set Stax Parser and streamIn
    var io = com.idyria.osi.ooxoo.core.buffers.structural.io.sax.StAXIOBuffer(url)
    res.appendBuffer(io)
    io.streamIn

    // Return
    res

  }

}