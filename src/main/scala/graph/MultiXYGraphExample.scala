package graph

import kit.edu.ipe.adl.chiptest.measurement.data.MultiXYGraph
import scala.util.Random
import java.io.File

/**
 * Example to show basic usage of Multi XY graph
 */
object MultiXYGraphExample extends App {

  //-- Create MultiXY GRaph
  var multiXY = new MultiXYGraph

  //-- Create some XY Graph from Random Datas
  var graphCount = 4
  var pointsCount = 256
  (0 until graphCount) foreach {
    i =>

      var graph = multiXY.createXYGraph(s"Random Graph: $i")

      (0 until pointsCount) foreach {
        pointI =>
          var point = graph.points.add
          point.X = pointI
          point.Y.add.set(Random.nextGaussian())
      }

  }

  //-- Now the MultiXYGraph has "graphCount" Graphs
  //-- It can be saved as plain XML or as compressed archive directly
  var outFolder = new File("target/test-out/MultiXYGraphExample/")
  outFolder.mkdirs()

  //-- Save examples
  multiXY.toFile(new File(outFolder, "multixxygraph-example.xml"))
  multiXY.toFile(new File(outFolder, "multixxygraph-example.xml.zip"))

  //multiXY.addXWaveformGraph(name, wf)

  // MUltiXY Graph also supports saving as an archive
  // This means some companion files can be saved with it
  // Here is fast example on how to generate CSV companion files for all XY Graphs
  //------------
  
  //-- First set the Archive to be saved to
  multiXY.toArchive(new File(outFolder,"multixxygraph-example-arch.zip"), erase =true)
  
  //-- Call the utility function
  multiXY.archiveXYGraphsCSV(sep = ",") // You can use sep=";" too depending on the system
  
  //-- Save the Archive with the generated XML
  multiXY.finishArchive
  
}