package shiftregister

import kit.edu.ipe.adl.chiptest.shiftregister.model.H35ShiftRegister
import kit.edu.ipe.adl.chiptest.measurement.data.XYGraph
import kit.edu.ipe.adl.chiptest.measurement.data.MultiXYGraph

object ShiftRegisterOut extends App {
  
  var sr = H35ShiftRegister(getClass.getClassLoader.getResource("sr/example_sr.xml"))
  
  sr.VPADC = 10
  
  println("Res: "+sr.toXMLString)
  
  //-- SHift Register in Grap
  var g = new MultiXYGraph
  g.shiftRegister = sr
  
  println("MG: "+g.toXMLString)
  
}