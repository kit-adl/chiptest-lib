package comport

import kit.edu.ipe.adl.chiptest.comport.ComportHarvester
import kit.edu.ipe.adl.chiptest.comport.COMPort
import kit.edu.ipe.adl.chiptest.comport.COMPort
import dk.thibaut.serial.enums.BaudRate

object ComPortExample extends App {
  
  // Use ComPort Harvester
  ComportHarvester.harvest
  
  // List comports
  ComportHarvester.onResources[COMPort] {
    case comPort => 
      println("Found Comport: "+comPort.getId)
      
  }
  
  var com = ComportHarvester.getResourcesOfType[COMPort].find {cp => cp.getId == "COM3"}.get
  com.configure(BaudRate.B9600)
  
  var result = com.sendLineReceiveLine(":WriteReg 06 0a 00")
  println("receiveline")
  println(result)
  
  
  
  
}