package graph


import java.io.File
import kit.edu.ipe.adl.chiptest.measurement.data.XYGraph

object INLexample extends App {
  
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
  
  // Create ideal points
  var maxbits = 9
  var maxvalue = 512.0
  var totalpoints = 1024
  
  var idealGraphpoints = (0 until totalpoints) map {
    i =>
      -255+(i*(maxvalue/totalpoints)).toInt
  }
  
  var idealGraph = XYGraph(idealGraphpoints)
  idealGraph.name = "Ideal"
   idealGraph.toJFreeChart

 
   //Roberto: ADC testpoints with offset (plateau) not used
   var ADCTestValues = (-totalpoints until totalpoints) map {
     i => 
     (i*(maxvalue/totalpoints)).toInt match { 
      case yValues if(yValues < -255) => -255
      case yValues if(yValues > 255) => 255
      case yValues => yValues
    }
  }
  
  /*var TestADCValuesGraph = XYGraph(ADCTestValues)*/
  var TestADCValuesGraph = XYGraph(idealGraphpoints)
  TestADCValuesGraph.name = "ADC Test Values + Plateau"
  TestADCValuesGraph.toJFreeChart

   // INL Test
   var INLGraph = mergedGraph.INL(9,signed=true)
   INLGraph.name = "INL Func Ideal Data (no offset)" 
   var maxINL = INLGraph.points.map{yValue => yValue.Y(0).toFloat}.max
   var minINL = INLGraph.points.map{yValue => yValue.Y(0).toFloat}.min
   var INLpktopk = maxINL - minINL
   println("Maximum value of INL:" + maxINL)
    println("Minimum value of INL:" + minINL)
    println("Peak to peak INL:" + INLpktopk)
    
  
   /*var y_zero_avg2  = mergedGraph.points(0).Y.map { yValue => yValue.data.toFloat}.sum / mergedGraph.points(0).Y.size*/
   // DNL Test
   var DNLGraph = idealGraph.DNL(9,signed=true)
   DNLGraph.name = "DNL Func Ideal Data (no offset)" 
  
 // INLGraph.debugPrint
 //  INLGraph.toJFreeChart
 //  Console.readLine()
 // sys.exit
 
   // With real data
   // var INLGraphReal = mergedGraph.INL(9,signed=true)
   //INLGraphReal.name = "INL Func Real data" 
   
   //DNLGraph.debugPrint
   
   INLGraph.toJFreeChart
   //DNLGraph.toJFreeChart
   Console.readLine()
  sys.exit
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
  var y_zero_avg = 0.0
  mergedGraph.points(0).Y.foreach {
    yValue =>
    y_zero_avg = y_zero_avg + yValue.data
  }
    y_zero_avg /= mergedGraph.points(0).Y.size
   // var x_zero : Float = mergedGraph.points(0).X first value in list is not the smallest one!
    var x_min :Float = 0 //TO DO: set real value!!
    
   println("Point y_zero_avg: " +y_zero_avg)
    val bits = 9
    val signed = true
  var filteredMultiple = mergedGraph.mapValues {
    case (x , yValues) => 
      
      /*yValues.find { v => v > 142 } match {
        case Some(_) => List(1)
        case None => List(0)
      }*/
     
      /* def avg[T](x: Iterable[T])(implicit num: Numeric[T]) ={
    num.toDouble(x.sum)/ x.size
  }*/
      var unsignedMaxValue = (Math.pow(2, bits)-1) 
      var maxValue = signed match {
        case true => (Math.pow(2, bits-1)-1)
        case false => (Math.pow(2, bits)-1) 
      }
      var v_lsb  = maxValue / mergedGraph.points.size
    
      println("LSB: "+v_lsb);
      
      // Averqge of y values for this X to get one point for INL
      var y_avg = yValues.sum / yValues.size
      
      //var y_avg_norm = (((y_avg + maxValue) * 2)/1024)*1000  
      var y_avg_norm = (((y_avg + maxValue) * 2)/mergedGraph.points.size)*mergedGraph.points.size
      
      var y_zero_avg_norm = (((y_zero_avg + 254) * 2)/1024)*1000
      //var y_zero_avg_norm = (((y_zero_avg + maxValue) * 2)/mergedGraph.points.size)*mergedGraph.points.size
      //Let us calculate the INL:  INL = | [(VD - VZERO)/VLSB-IDEAL] - D 
   //  var inl = ((x - x_min) - norm)/512
      println("y_avg_norm: " +  y_avg_norm)
       println("x value: " +  x)
     var inl = ((y_avg_norm - 7 ) - x)
       
      List(inl.toFloat)
   }
  filteredMultiple.name = "INL"
  //filteredMultiple.debugPrint
  filteredMultiple.toJFreeChart
  filteredMultiple.toJFreeChart
  
  /*
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
  */
  
  
}