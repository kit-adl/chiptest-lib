package kit.edu.ipe.adl.chiptest.shiftregister.model

import com.idyria.osi.tea.listeners.ListeningSupport

import kit.ipe.adl.rfg3.language.RFLanguage
import kit.ipe.adl.rfg3.model.Register

/**
 * @author zm4632/mf1049#
 *
 */

class H35Sequencer(val ampControl: Register) extends RFLanguage with ListeningSupport {

  
  def toggle_chopper  = {
    var it = 0
    // send sequence to ADC
    onBuffering {
    for (it <- 0 to 80){
      ampControl.chopper = 1
      ampControl.write
    }
      ampControl.chopper = 0
      ampControl.write
      /*ampControl.chopper = 1
      ampControl.write
      ampControl.chopper = 0
      ampControl.write
      */
    } 
  }
  
  def toggle_ampBL = {
    onBuffering {    
      
      ampControl.ampBL = 1
      ampControl.write
      ampControl.ampBL = 0
      ampControl.write
      ampControl.ampBL = 1
      ampControl.write
      ampControl.ampBL = 0
      ampControl.write
    }
  }
  
  def toggle_ampBR = {
    onBuffering {      
      ampControl.ampBR = 1
      ampControl.write
      ampControl.ampBR = 0
      ampControl.write
      ampControl.ampBR = 1
      ampControl.write
      ampControl.ampBR = 0
      ampControl.write
    }
  }
  
  def toggle_ampL = {
    onBuffering {
      ampControl.ampL = 1
      ampControl.write
      ampControl.ampL = 0
      ampControl.write
      ampControl.ampL = 1
      ampControl.write
      ampControl.ampL = 0
      ampControl.write
    }
    
  }
  
  def toggle_ampR = {
    onBuffering {      
      ampControl.ampR = 1
      ampControl.write
      ampControl.ampR = 0
      ampControl.write
      ampControl.ampR = 1
      ampControl.write
      ampControl.ampR = 0
      ampControl.write
    }
  }
  
  def toggle_resL = {
    onBuffering {    
      ampControl.resL = 1
      ampControl.write
      ampControl.resL = 0
      ampControl.write
      ampControl.resL = 1
      ampControl.write
      ampControl.resL = 0
      ampControl.write
    }
  }
  
  def toggle_resR = {
    onBuffering {
      ampControl.resR = 1
      ampControl.write
      ampControl.resR = 0
      ampControl.write
      ampControl.resR = 1
      ampControl.write
      ampControl.resR = 0
      ampControl.write
    }
  }
  
  def toggle_sel = {
    onBuffering {
      ampControl.sel = 1
      ampControl.write
      ampControl.sel = 0
      ampControl.write
      ampControl.sel = 1
      ampControl.write
      ampControl.sel = 0
      ampControl.write
    }
  }
  
  
  def adc_startsample = {
    onBuffering {
      ampControl.adc_start = 1
      ampControl.write
    }
  }
  
  def adc_stopsample = {
    onBuffering {
      ampControl.adc_start = 0
      ampControl.write
    }
  
}  
  
  
}  
  
  
 /* ampControl.chopper = 1
 //ampControl.write()
  ampControl.chopper = 0
  ampControl.chopper = 1
  ampControl.chopper = 0*/
  




   
  //}
  
  /*
  def sendBit(bit: Boolean) = {
    
    var it = 0
   
    if (bit) {
      si_chip.setMemory(1)
    } else {
      si_chip.setMemory(0)
    }

   
    
    for (it <- 0 to 3) {
      si_chip.parentRegister.write
    }
    
  //   println(s"Sending bit: $bit, field value is ${si.value}")

    for (it <- 0 to 3) {
      clk1_chip.setMemory(1)
      clk1_chip.parentRegister.write
      clk2_chip.setMemory(1)
      clk2_chip.parentRegister.write
    }

    for (it <- 0 to 3) {
      clk1_chip.setMemory(0)
      clk1_chip.parentRegister.write
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
    for (i <- 0 to 3) {
      ld_chip.setMemory(1)
      ld_chip.parentRegister.write
    }
    ld_chip.setMemory(0)
    ld_chip.parentRegister.write

  } //end of LoadDAC

  def sendDACValue(value: Int) = {
    var a = 0;

    // for loop execution with a range
    for (a <- 0 to 13) {
      sendBit((value & (0x01 << (13 - a))) != 0x00)
    }
    sendBit(false)
    sendBit(false)
  } //end of SendDACValue

  def setAmplitude(amp: Double) = {

    var volt = 0.0;

    volt = (16383 * amp) / 3.3
    sendDACValue(volt.toInt)

  } //end of setAmplitude

  def program = {
    
    onBuffering(target) {
      
    
     var total = this.registers.count(_.findAttribute("sr.spare")==None)
      
      this.registers.filter(_.findAttribute("sr.spare")==None).zipWithIndex.foreach {
        case (reg,i) =>  
          setAmplitude(reg.value.toDouble)
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
*/
 
object  H35Sequencer{

  

}