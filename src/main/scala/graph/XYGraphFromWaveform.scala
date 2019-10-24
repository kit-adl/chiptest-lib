package graph

import kit.edu.ipe.adl.chiptest.measurement.data.XYGraph
import java.io.File

object XYGraphFromWaveform extends App {
  
  // Load XYGraph with Waveform as data
  //---------
 // var xygraph = XYGraph(new File("src/examples/resources/graph/xygraph-waveform.xml"))
  var xygraph = XYGraph(getClass.getClassLoader.getResource("graph/xygraph-waveform.xml"))
  
  // Populate data from Waveform...manual operation for now
  //----------
  xygraph.populateFromWaveform()
 
  // Plot
  //------------
  xygraph.toJFreeChart
  
  // Filter example
  // keep values between +/-100
  var filteredGraph = xygraph.mapValue {
    case (x,y) if (x%500==0) => y
    case other => 0
  }
  
  filteredGraph.toJFreeChart
  
  // Output to CSV
  //--------------------
  var out = new File("target/output/xygraph-waveform.csv")
  xygraph.toCSV(out)
  
  
}