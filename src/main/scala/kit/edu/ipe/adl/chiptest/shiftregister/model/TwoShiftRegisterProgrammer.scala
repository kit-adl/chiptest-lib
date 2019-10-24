package kit.edu.ipe.adl.chiptest.shiftregister.model

import com.idyria.osi.ooxoo.core.buffers.structural.xelement
import com.idyria.osi.tea.listeners.ListeningSupport
import kit.ipe.adl.rfg3.language.RegisterFileHost
import kit.ipe.adl.rfg3.language.RFLanguage


/**
 * @author zm4632/mf1049#
 *
 */
@xelement(name = "ShiftRegister")
class TwoShiftRegisterProgrammer(val sh_reg1: ShiftRegister, val sh_reg2: ShiftRegister, val target: RegisterFileHost) extends SRCommon with RFLanguage with ListeningSupport {

   
  
  def sendBit(bit: Boolean, bit2: Boolean) = {
    
    var it = 0
   
    if (bit) {
      //si_chip.setMemory(1)
      sh_reg1.si.setMemory(1)
      
      
    } else {
      //si_chip.setMemory(0)
      sh_reg1.si.setMemory(0)
      
    }
    
    if (bit2) {
      //si_chip.setMemory(1)
      sh_reg2.si.setMemory(1)
      
    } else {
      //si_chip.setMemory(0)
      sh_reg2.si.setMemory(0)
    }
   
    
    for (it <- 0 to 3) {
      //si_chip.parentRegister.write
      sh_reg1.si.parentRegister.write
      sh_reg2.si.parentRegister.write
    }
    
  //   println(s"Sending bit: $bit, field value is ${si.value}")

    for (it <- 0 to 3) {
      sh_reg1.clk.setMemory(1)
      sh_reg1.clk.parentRegister.write
      sh_reg2.clk.setMemory(1)
      sh_reg2.clk.parentRegister.write
    }

    for (it <- 0 to 3) {
      sh_reg1.clk.setMemory(0)
      sh_reg1.clk.parentRegister.write
      sh_reg2.clk.setMemory(0)
      sh_reg2.clk.parentRegister.write
    }

  } //end of sendBit

  def LoadDAC() = {
    var i = 0; 
    
    sh_reg1.ld.setMemory(0)
    sh_reg1.ld.parentRegister.write
    sh_reg1.ld.setMemory(0)
    sh_reg1.ld.parentRegister.write
    sh_reg2.ld.setMemory(0)
    sh_reg2.ld.parentRegister.write
    sh_reg2.ld.setMemory(0)
    sh_reg2.ld.parentRegister.write
    
    for (i <- 0 to 3) {

      sh_reg1.ld.setMemory(1)
      sh_reg1.ld.parentRegister.write
      sh_reg2.ld.setMemory(1)
      sh_reg2.ld.parentRegister.write
    }
 
    sh_reg1.ld.setMemory(0)
    sh_reg1.ld.parentRegister.write
    sh_reg2.ld.setMemory(0)
    sh_reg2.ld.parentRegister.write

  } //end of LoadDAC

  def sendDACValue(value: Int, value2: Int) = {
    var a = 0;

    // for loop execution with a range
    for (a <- 0 to 13) {
      sendBit((value & (0x01 << (13 - a))) != 0x00, (value2 & (0x01 << (13 - a))) != 0x00)
    }
    
    sendBit(false, false)
    sendBit(false, false)
  } //end of SendDACValue

  def setAmplitude(amp: Double, amp2: Double) = {

    var volt = 0.0
    var volt2 = 0.0

    volt = (16383 * amp) / 3.3
    volt2 = (16383 * amp) / 3.3
    sendDACValue(volt.toInt, volt2.toInt)

  } //end of setAmplitude

 override def program = {
    
    onBuffering(target) {
      
    
     var total = sh_reg1.registers.count(_.findAttribute("sr.spare")==None)
     var total2 = sh_reg2.registers.count(_.findAttribute("sr.spare")==None)
      
      sh_reg1.registers.filter(_.findAttribute("sr.spare")==None).zipWithIndex.foreach {
  
        case (reg,i) =>
          setAmplitude(reg.getMemory, reg.getMemory)
          this.@->("progress", (i.toDouble/(total-1).toDouble))
          println(s"index i: $i" )
          println(s"total: $total")
      }
      sh_reg2.registers.filter(_.findAttribute("sr.spare")==None).zipWithIndex.foreach
        {
        case (reg,i) =>
          setAmplitude(reg.getMemory, reg.getMemory)
          this.@->("progress", (i.toDouble/(total2-1).toDouble))
          println(s"index i: $i" )
          println(s"total: $total2")
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

 
object  TwoShiftRegisterProgrammer{

  def apply(url: java.net.URL, sh_reg1: ShiftRegister, sh_reg2: ShiftRegister, target: RegisterFileHost) = {

    // Instanciate
    var res = new TwoShiftRegisterProgrammer(sh_reg1, sh_reg2, target)

    // Set Stax Parser and streamIn
    var io = com.idyria.osi.ooxoo.core.buffers.structural.io.sax.StAXIOBuffer(url)
    res.appendBuffer(io)
    io.streamIn

    // Return
    res
  }
}