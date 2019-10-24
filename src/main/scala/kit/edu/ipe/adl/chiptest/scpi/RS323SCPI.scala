package kit.edu.ipe.adl.chiptest.scpi

import jssc.SerialPort

/**
 * @author zm4632
 */
class RS323SCPI(var serialPort : SerialPort) extends SCPIInterface {
  
  
  def sendCommand(c:String) : String = {
    
    ""
  }
  
}