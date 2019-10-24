package graph


import java.io.File
import kit.edu.ipe.adl.chiptest.measurement.data.XYGraph

object GraphDataExample extends App {
  
  // Open a graph data
  // - use new File then Convert to URL
  // - URL is used there so the XML source can be a file or a resource embedded in the application or something online ....
  var graph = XYGraph(new File("src/examples/resources/graph/example-graphpoints.xy.xml").toURI.toURL())
  
  // "graph" is now the same data structure used when gathering data
  // It can be used to plot if you are in a webpage, or perform operations
  
  //-----------------------
  // merge X values
  //----------------------
  /*
   * The loaded Graph has multiple Y values for X, but it is represented as multiple (X,Y) pairs where X is the same
   * It is easier to use the data structure if it is represents as (X,(Y0,Y1....,YN)), in other words one X point holding multiple Y values
   * 
   * The mergeX methods performs this operation and returns a new graph
   * 
   */
  var mergedGraph = graph.mergeX
  mergedGraph.name = "Merged Base data"
 
  // Use To Free Chart to use JFree Chart instead of JFX Chart
  mergedGraph.toJFreeChart
  Console.readLine()
 
  
  //-------------------
  // Access Values using Loop
  //-----------
  
  // grap.points contains the points elements, wich each have an X value and some Y values
  mergedGraph.points.foreach {
    point => 
      //println("Point x: "+point.X)
      
      // Y is a list of Float points, in case you have more than one point per X
      //println("Point y: "+point.Y.toList)
      
      // You can go over the Lists of Y values
      point.Y.foreach {
        yValue => 
         var yFloat =  (yValue.data + 256) * 2
          
      }
  }
  
  mergedGraph.toJFreeChart
  
  //------------------------------
  // Transform the values in case of multiple Y points
  //---------------------------
  println("-------- Transform Multiple -----------")
  // Map
  // With map you can get a new XY graph transformed by provided function
  // - The function takes as input argument the X position and the list of values
  // - It should return a new list of Y values for the new graph
  
  var y_zero_avg2  = mergedGraph.points(0).Y.map { yValue => yValue.data.toFloat}.sum / mergedGraph.points(0).Y.size
  
  //-- The example below sets all the values to 0 below a certain threshold and 1 above it
  //var y_zero_avg = mergedGraph.points(0).Y.map(_.data).sum// yValues.size
  var y_zero_avg  = 0.0
  mergedGraph.points(0).Y.foreach {
    yValue =>
    y_zero_avg = y_zero_avg + yValue.data
  }
    y_zero_avg /= mergedGraph.points(0).Y.size
   // var x_zero : Float = mergedGraph.points(0).X first value in list is not the smallest one!
    var x_min :Float = 0 //TO DO: set real value!!
    
   println("Point y_zero_avg: " +y_zero_avg)
  var filteredMultiple = mergedGraph.mapValues {
    case (x , yValues) => 
      
      /*yValues.find { v => v > 142 } match {
        case Some(_) => List(1)
        case None => List(0)
      }*/
     
      /* def avg[T](x: Iterable[T])(implicit num: Numeric[T]) ={
    num.toDouble(x.sum)/ x.size
  }*/
      var v_lsb  = 1.0
      var y_avg  = yValues.sum / yValues.size
      var y_avg_norm = (((y_avg + 254) * 2)/1024)*1000     
      var y_zero_avg_norm = (((y_zero_avg + 254) * 2)/1024)*1000
      //Let us calculate the INL:  INL = | [(VD - VZERO)/VLSB-IDEAL] - D 
      //var inl = ((x - x_min) - norm)/512
      println("y_avg_norm: " +  y_avg_norm)
       println("x value: " +  x)
      var inl = ((y_avg_norm - 7 ) - x)
      List(inl)
   }
  filteredMultiple.name = "First AVG Test"
  filteredMultiple.debugPrint
  filteredMultiple.toJFreeChart
  
  //------------------------------
  // Transform the values in case of single Y points
  //---------------------------
  println("-------- Transform Multiple -----------")
  
  // If the Data only has (X,Y) pairs, the mapValue (singular) just operates on (X,Y) pair and needs a new Y value
  // It is easier to use for simple filtering
   var filteredSingle = mergedGraph.mapValue {
    case (x , y) => 
      
     y match {
        case other => (((y + 256) * 2)/1024)*1000
      }
      
      
      
     
     
      
  }
  filteredSingle.name = "Filtered Single"
  
  filteredSingle.debugPrint
  filteredSingle.toJFreeChart
  
  
  
}