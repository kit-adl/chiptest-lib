package kit.edu.ipe.adl.chiptest.scpi.toellner

import kit.edu.ipe.adl.chiptest.scpi.SCPIInterface

/**
 * @author zm4632
 */
class TOE884232(var interface : SCPIInterface) {
  
  // Configure
  //------------------
  var maxV1 = 32.0F
  
  /**
   * This method checks the interface matches with this device ID
   */
  def check = {
    
    // Get ID from Device
    interface.sendCommand("*IDN?") match {
      case id if(id.contains("TOE8842-32")) => 
      case id => throw new RuntimeException(s"Class TOE884232 can only be used with a matching interface. Currently connected to : $id")
    }
    
    
    
  }
  
  def setV1(v:Float) = {
    require(v < maxV1)
  }
  
}