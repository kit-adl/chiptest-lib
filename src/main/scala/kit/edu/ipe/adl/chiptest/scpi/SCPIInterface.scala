package kit.edu.ipe.adl.chiptest.scpi

/**
 * @author zm4632
 */
trait SCPIInterface {
  
  
  /**
   * This simple method sends a String command, and returns the string result
   */
  def sendCommand(c:String) : String
  
}