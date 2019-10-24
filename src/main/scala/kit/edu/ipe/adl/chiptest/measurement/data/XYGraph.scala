package kit.edu.ipe.adl.chiptest.measurement.data

import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.XYChart
import javafx.stage.Stage
import javax.swing.SwingUtilities

import org.jfree.chart.ChartPanel
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.chart.plot.PlotOrientation
import javax.swing.JFrame

import org.jfree.ui.RefineryUtilities
import org.jfree.chart.ChartFactory
import org.jfree.data.xy.DefaultXYDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.DefaultTableXYDataset
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter
import java.io.File

import org.jfree.graphics2d.svg.SVGGraphics2D
import java.awt.Rectangle

import org.jfree.graphics2d.svg.SVGUtils
import javax.swing.JCheckBox
import java.awt.GridBagLayout
import javax.swing.JLabel
import java.awt.GridBagConstraints

import com.idyria.osi.ooxoo.core.buffers.datatypes.DoubleBinaryBuffer
import com.idyria.osi.ooxoo.core.buffers.structural.xelement
import com.idyria.osi.tea.io.TeaIOUtils
import org.odfi.indesign.core.module.jfx.JFXRun
import org.odfi.instruments.data.XWaveform
import java.text.DecimalFormat
import java.util.Locale
import java.text.DecimalFormatSymbols
import com.idyria.osi.ooxoo.core.buffers.structural.DataUnit
import com.idyria.osi.ooxoo.core.buffers.structural.XList
import scala.collection.mutable.ArraySeq
import org.odfi.instruments.data.XWaveform
import org.odfi.indesign.core.module.swing.SwingUtilsTrait
import biz.source_code.dsp.filter.IirFilterDesignFisher
import biz.source_code.dsp.filter.FilterPassType
import biz.source_code.dsp.filter.FilterCharacteristicsType
import biz.source_code.dsp.filter.IirFilter
import com.idyria.osi.tea.logging.TLogSource
import scala.util.Random
import com.idyria.osi.tea.SearchPredef
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYSplineRenderer
import org.jfree.chart.axis.NumberAxis

@xelement(name = "XYGraph")
class XYGraph extends XYGraphTrait with SwingUtilsTrait with SearchPredef {

  // Options
  //------------
  var enableParallel = false

  // Multi graph
  //------------------
  var multiGraph: Option[MultiXYGraph] = None

  // Waveform Support
  //---------------------------

  /**
   * Load values to rawArray from the waveform parameters
   */
  def loadValuesFromXWaveform(waveform: XWaveform, timex: Boolean = false) = {

    // Clean datapoints first
    this.points.clear()

    // println("Points in data: "+ waveform.data.data.size+" // expected: "+waveform.points)

    // Map to raw values or raw points
    timex match {
      case true =>

        sys.error("Not supported timex=true")

      case false =>

        this.rawValues = (waveform.yIncrement, waveform.yOrigin) match {

          case (yi, yo) if (yi != null && yo != null) =>

            waveform.data.data.map(value => value * waveform.yIncrement.data - -waveform.yOrigin.data.toDouble)

          case (yi, yo) if (yi != null) =>

            logWarn[XYGraph]("Loading waveform points, yincrement provided without yorigin, translatin possibly incorrect, assuming 0 origin!")

            waveform.data.data.map(value => value * waveform.yIncrement.data)

          case (yi, yo) => waveform.data.data.map(_.toDouble)
        }
    }

    // Import from waveform array
    // Adapt values based on Y increment
    /*this.rawValues = waveform.data.data.map {
      case value if (waveform.yIncrement != null && waveform.yOrigin != null) =>

        (value * waveform.yIncrement.data) - waveform.yOrigin.data.toDouble
      case value if (waveform.yIncrement != null) =>
        logWarn[XYGraph]("Loading waveform points, yincrement provided without yorigin, translatin possibly incorrect, assuming 0 origin!")
        (value * waveform.yIncrement.data)
      case value =>
        value
    }*/

  }

  def populateFromWaveform(timex: Boolean = false) = rawValuesOption match {

    case None =>

      // Load from Local Waveform
      //--------------------------
      this.xWaveformOption match {

        // External File
        case None if (this.externalFile != null && multiGraph.isEmpty) =>
          sys.error("Cannot load graph from file if no Multigraph archive is present")

        // External XWaveform File
        case None if (this.externalFile != null && this.externalType.toString == "xwaveform" && multiGraph.isDefined) =>

          println(s"Loading XWaveform file ${externalFile}...")

          var mg = multiGraph.get

          mg.loadXMLFileFromSource(this.externalFile.toString, new XWaveform) match {
            case Some(wf) =>
              loadValuesFromXWaveform(wf, timex)

            case None =>
              sys.error("Could not load entry " + this.externalFile.toString + ", nothing in archive")
          }

        // External Raw File
        case None if (this.externalFile != null && this.externalType.toString == "raw-values" && multiGraph.isDefined) =>

          var mg = multiGraph.get

          mg.loadDoubleArrayFromSource(this.externalFile.toString) match {
            case Some(arr) =>
              this.rawValues = arr
            case None =>
              sys.error("Could not load entry " + this.externalFile.toString + ", nothing in archive")
          }

        // Nothing
        case None =>

          //println(s"Cannot populate..." + this.externalFile)

        // Local WF
        case Some(waveform) =>

          loadValuesFromXWaveform(waveform, timex)

      }

    case other =>

  }

  /**
   * Convert XML points to Y array and save in graph
   */
  def pointsYToRawValues = {
    this.rawValues = this.getYValues
    this.points.clear
  }

  /**
   * Save Values to external bin file
   */
  def pointsYToExternalRawValues = this.multiGraph match {
    case Some(multigraph) =>

      multigraph.saveDoublesToArchive(name + "-values.bin", getYValues)
      externalFile = name + "-values.bin"
      externalType = "raw-values"
      this.points.clear
    case None =>
      sys.error("Cannot Save Points to an external Value, this feature is supported only if the Graph has been added to a MultiGraph instance")

  }

  /**
   * Delelte waveform information afterwards
   * WARNING -> XYGraph is clean afterwards
   */
  def waveFormToExternalRawValues = {

    if (this.xWaveform != null) {

      this.rawValues = this.xWaveform.getRealData
      this.pointsYToExternalRawValues
      this.cleanPoints
      this.xWaveform = null

    }

  }

  // Get Values{
  //----------------

  def cleanPoints = {
    this.points.clear()
    this.rawValues = null
    if (this.xWaveform != null) {
      //this.xWaveform.
    }
  }

  def getNumberOfPoints = getYValues.size
  def getYValuesInt = getYValues.map(_.toInt)

  /**
   * Return Y values depending on XML points usage of Raw data buffer
   */
  def getYValues = {
    populateFromWaveform()
    this.rawValuesOption match {
      case Some(defined) =>
        defined.data.toArray
      // USe points
      case other =>
        this.points.collect {
          case p if (p.Y.size > 0) =>
            p.Y.map { y => y.data }.max
        }.toArray

    }
  }

  def setYValues(arr: Array[Double]) = {
    this.rawValues = arr
  }

  /**
   * Get XY Values, with X points set to time if neeeded
   */
  def getXYValues = {
    populateFromWaveform(timex = true)
    hasXYValues match {
      case true =>
        this.rawPoints.data.grouped(2).map {
          xy => (xy(0), xy(1))
        }.toArray
      case false =>
        Array[(Double, Double)]()
    }
  }

  def setXYValues(arr: Array[Double]) = {
    this.rawPoints = arr
  }

  def hasXYValues = {
    this.rawPointsOption.isDefined
  }

  // Fill
  //------------

  /**
   * Fill with count (i,i) points
   * Used to test
   */
  def fillLinearPoints(count: Integer) = {

    (0 until count).foreach {
      i =>
        addPoint(i, i)
    }
  }

  /**
   * Fill Data from a Random Gaussian distribution (standard scala API)
   * Sets the data to the internal Double Array
   *
   */
  def fillRandomGaussian(count: Integer, max: Double) = {

    val r = new java.util.Random
    val res = (0 until count).map {
      i =>
        (r.nextGaussian() * max) % max
      //Random.nextGaussian() 

    }.toArray

    setYValues(res)

    this

  }

  // Main Points API
  //---------------------

  /**
   * Add a point in XML format
   * NOt very performant
   *
   */
  def addPoint(x: Double, y: Double) = {
    var p = this.points.add
    p.X = x
    p.Y.add.set(y)
  }

  def getMaxY = {
    hasXYValues match {
      case true =>
        getXYValues.maxBy{case (x,y) => y}._2
      case false =>
        getYValues.max
    }

  }

  def getMinY = {
     hasXYValues match {
      case true =>
        getXYValues.minBy{case (x,y) => y}._2
      case false =>
        getYValues.max
    }
  }

  // Windowing
  //------------------

  /**
   * Returns Graph with points truncated left and right
   * Returns in XML Data format, don't forget to use optimisation if saving back to an archive
   */
  def windowed(dropLeft: Int, dropRight: Int) = {

    val graph = new XYGraph
    getYValues.drop(dropLeft).dropRight(dropRight).zipWithIndex.foreach { case (y, x) => graph.addPoint(x, y) }
    graph
  }

  /**
   * Map Each point on the graph with a function of "i" and leftCount points before and rightCount points after
   */
  def mapRollingWindow(K: Int)(f: ((Int, Double, Array[Double]) => Double)) = {

    val graph = new XYGraph
    val values = getYValues

    values.zipWithIndex.map {
      case (y, x) =>

        // Get left and right slices
        val leftIndex = if ((x - K) < 0) 0 else x - K
        val rightIndex = if ((x + K + 1) >= values.size) values.size - 1 else x + K + 1

        val localValues = values.slice(leftIndex, rightIndex)

        f(x, y, localValues)
    }
    //values.sliding(size)

  }

  /**
   * Map Each point on the graph with a function of "i" and leftCount points before and rightCount points after
   *
   * @parameter cutoff means it will remove the points at the beginning and end for which the full X-K <-> X+K window can be respected
   */
  def mapCenteredRollingWindow(K: Int, fullWindow: Boolean = false)(f: ((Int, Double, Map[Int, Double]) => Double)) = {

    val graph = new XYGraph
    val values = getYValues
    val valuesWithIndexes = fullWindow match {
      case true  => values.zipWithIndex.filter { case (y, x) => x > K && x < (values.size - K) }
      case false => values.zipWithIndex
    }

    val mappedValues = valuesWithIndexes.par.map {
      case (y, x) =>

        // Get left and right slices
        val leftIndex = if ((x - K) < 0) 0 else x - K
        val rightIndex = if ((x + K + 1) >= values.size) values.size - 1 else x + K + 1

        // Create left and right indexes
        val leftIndexes = (leftIndex until x).map(i => i - x)
        val rightIndexes = (x until x + rightIndex).map(i => i - x)

        // Get local values
        val localValues = values.slice(leftIndex, rightIndex)

        val allIndexes = leftIndexes ++ rightIndexes

        // Center Local Values indexes in reference to K
        // Just substact K to index produce -K/+K
        // val indexes = (0 until localValues.size).reverse.map(i => i - K)

        // Create Value + Index
        val valuesAndIndexes = allIndexes.zip(localValues).toMap

        f(x, y, valuesAndIndexes)
    }

    graph.setYValues(mappedValues.toArray)
    graph
    //values.sliding(size)

  }

  // Merge Map and so on
  //-----------------------
  /**
   * Use is XML "Points" is available
   * If multiple separated (X,Y) with same X exist, group then into one X + Multiple Y
   */
  def mergeX = {
    var graph = new XYGraph

    var grouped = this.points.groupBy { p => p.X.data }.toList.sortBy(f => f._1)
    grouped.foreach {
      case (x, commonPoints) =>
        var newPoint = graph.points.add
        newPoint.X = x

        // merge all grouped points values to the new point
        commonPoints.foreach {
          point =>
            point.Y.foreach {
              ypoint =>
                var value = newPoint.Y.add
                value.data = ypoint.data;
            }
        }
    }

    graph
  }

  /**
   * Maps X,(Y...) values
   * Y could have multiple points, in that case use this function
   * Result is saved in XML Values
   */
  def mapValues(f: (Double, List[Double]) => List[Double]) = {

    var graph = new XYGraph
    getYValues.zipWithIndex.foreach {
      case (y, x) =>
        var newPoint = graph.points.add
        newPoint.X = x
        f(x, List(y)).foreach {
          res =>
            newPoint.Y.add.set(res)
        }

    }
    graph

    //graph.rawValues = 
    /* this.points.foreach {
      point =>
        var newPoint = graph.points.add
        var newValues = f(point.X.data, point.Y.map { v => v.data }.toList)
        newPoint.X = point.X
        newValues.foreach {
          v =>
            var value = newPoint.Y.add
            value.data = v;
        }
    }*/

  }

  /**
   * Sliding Windows means it maps the actual point with a function of the slidingSize next points
   * if last point doesn't have the complete sliding window, ignore
   * (List[(Double,List[Double])])
   *
   * Reads data using getYValues
   * Returns Graph with XML Points
   */
  def mapValueSliding(slidingsize: Int)(f: (List[(Double, List[Double])]) => Double) = {

    var graph = new XYGraph
    getYValues.zipWithIndex.toList.sliding(slidingsize).foreach {
      // case (y,x) => 
      case points if (points.size == slidingsize) =>

        var newPoint = graph.points.add
        newPoint.X = points(0)._2

        var newValue = f(points.toList.map { case (y, x) => (x.toDouble, List(y)) })

        newPoint.Y.add.set(newValue)

      case other =>
    }

    graph

  }

  /**
   * Sliding Windows means it maps the actual point with a function of the slidingSize next points
   * if last point doesn't have the complete sliding window, ignore
   * (List[(Double,List[Double])])
   *
   * Reads data using getYValues
   * Returns Graph with XML Points
   */
  def mapYValueForwardSliding(slidingsize: Int)(f: (Double, List[Double]) => Double) = {

    var graph = new XYGraph
    getYValues.zipWithIndex.toList.sliding(slidingsize).foreach {
      // case (y,x) => 
      case points if (points.size == slidingsize) =>

        var newPoint = graph.points.add
        newPoint.X = points(0)._2

        var newValue = f(points(0)._1, points.drop(1).map { p => p._1 })

        newPoint.Y.add.set(newValue)

      case other =>
    }

    graph

  }

  def mapValueArr(f: (Double, Double) => Double) = {

    var graph = new XYGraph
    enableParallel match {
      case true =>

        val res = getYValues.zipWithIndex.par.map {
          case (v, i) =>
            f(i, v)

        }.toArray
        graph.setYValues(res)

      case false =>

        val res = getYValues.zipWithIndex.map {
          case (v, i) =>
            f(i, v)

        }.toArray
        graph.setYValues(res)

    }

    graph

  }

  /**
   * Returns Graph with XML POints
   * f(x,y) => newY
   */
  def mapValue(f: (Double, Double) => Double) = {

    var graph = new XYGraph
    enableParallel match {
      case true =>

        getYValues.zipWithIndex.par.foreach {
          case (v, i) =>
            var newPoint = graph.points.add
            newPoint.X = i
            newPoint.Y.add.set(f(i, v))
        }

      case false =>

        getYValues.zipWithIndex.foreach {
          case (v, i) =>
            var newPoint = graph.points.add
            newPoint.X = i
            newPoint.Y.add.set(f(i, v))
        }

    }

    graph

  }

  /**
   * Maps this current graph using provided values and plot both on same graph
   * f(this => result)
   */
  def mapAndPlotBoth(f: XYGraph => XYGraph) = {

    val newGraph = f(this)

    //-- Plot this and add dataset being other
    val ds = this.toJFreeChart

    var mappedData = Array[Array[Double]]((0 until newGraph.getYValues.size).map(_.toDouble).toArray, newGraph.getYValues.toArray)

    onSwingThreadLater {
      ds.addSeries("Mapped", mappedData)
    }

  }

  // Special Functions
  //------------------------

  def INL(bits: Int, signed: Boolean) = {

    var y_zero_avg = this.getYValues(0)

    val fullScaleValueCount = (Math.pow(2, bits))
    val ymaxvalue = this.getMaxY
    val yminvalue = this.getMinY
    val unsignedMaxValue = (Math.pow(2, bits) - 1)
    val maxValue = signed match {
      case true  => (Math.pow(2, bits - 1) - 1)
      case false => (Math.pow(2, bits) - 1)
    }
    val offset = signed match {
      case true  => (y_zero_avg - maxValue)
      case false => (-(y_zero_avg))
    }

    //-- Offset removal
    var noOffsetGraph = this.mapValues {
      case (x, yvalues) =>
        yvalues.map { yval => yval - y_zero_avg }

    }

    //-- INL 
    var inlgraph = this.mapValues {
      case (x, yValues) =>

        /*yValues.find { v => v > 142 } match {
        case Some(v) => List(1)
        case None => List(0)
      }*/

        /* def avg[T](x: Iterable[T])(implicit num: Numeric[T]) ={
    num.toDouble(x.sum)/ x.size
  }*/
        var y_avg: Double = getYValues.sum / getYValues.size

        var yValues_mean = getYValues.map(y => y - y_avg).toArray
        var xValues = (0 until getNumberOfPoints).toArray
        var x_avg: Double = xValues.sum / xValues.size
        var xValues_mean = xValues.map(x => x - x_avg).toArray
        var xValues_sigma = xValues.map(x => (x - x_avg) * (x - x_avg)).toArray
        var xyValues = xValues_mean.zip(yValues_mean).map { case (x, y) => x * y }
        //Linear regression
        var beta2 = xyValues.sum / (xValues_sigma.sum)
        var beta1 = y_avg - beta2 * x_avg

        var inl = yValues(0) - beta1 - beta2 * x

        //var v_lsbideal = 1.0

        List(inl.toFloat)
    }

    inlgraph.name = "INL"
    inlgraph

  }

  // -- DNL Calculation
  def DNL(bits: Int, signed: Boolean) = {

    var y_zero_avg = this.getYValues(0)
    val fullScaleValueCount = (Math.pow(2, bits))
    val unsignedMaxValue = (Math.pow(2, bits) - 1).toInt
    val maxValue = signed match {
      case true  => (Math.pow(2, bits - 1) - 1)
      case false => (Math.pow(2, bits) - 1)
    }
    val offset = signed match {
      case true  => (y_zero_avg - maxValue)
      case false => (-(y_zero_avg))
    }

    //-- Offset removal and average
    var noOffsetAvgGraph = this.mapValues {
      case (x, yvalues) =>
        var noOffset = yvalues.map { yval => yval - y_zero_avg }.sum
        List(noOffset / yvalues.size)
    }

    //--Average for X steps
    var xstepgraph = new XYGraph()
    var remainingGraph = noOffsetAvgGraph.points.toList
    (1 until unsignedMaxValue).map {
      x =>

        // Partition remaining graph between points under the actual step and above
        // Remaining graph is then the points above the actual step for the next search
        var (foundPoints, remainingGraphNew) = remainingGraph.partition { point => point.Y(0).data < (-255 + x) }
        remainingGraph = remainingGraphNew.toList

        var xaverage = (foundPoints.map { point => point.X.data }.sum / foundPoints.size).toInt

        var newX = xstepgraph.points.add
        newX.X = xaverage

    }

    //var xstepgraph = noOffsetAvgGraph

    //-- DNL 
    var dnlgraph = xstepgraph.mapValueSliding(2) {
      case xandy =>

        val (x0, yValues) = xandy(0)
        val (x1, yValues1) = xandy(1)
        var v_lsb = (fullScaleValueCount / getNumberOfPoints)
        var v_lsbideal = 1
        println("LSB: " + v_lsb);
        println("Maxvalue: " + maxValue)
        // Averqge of y values for this X to get one point for INL
        var y_avg: Double = yValues.sum / yValues.size

        //var y_avg_norm = (((y_avg + maxValue) * 2)/1024)*1000  
        var y_avg_norm = (((y_avg + maxValue)))

        //var y_zero_avg_norm = (((y_zero_avg + 254) * 2)/1024)*1000
        var y_zero_avg_norm = (((y_zero_avg + unsignedMaxValue)) / unsignedMaxValue) * getNumberOfPoints
        //Let us calculate the INL:  INL = | [(VD+1 - VD)/VLSB-IDEAL] - 1 
        //var inl = ((x - x_min) - norm)/512
        // var xnorm = ((x0 * (v_lsb))).floor.toInt
        // var xnorm_1 = ((x1 * (v_lsb))).round.toInt

        println("y_avg_norm: " + y_avg_norm)
        println("y_zero_avg_norm: " + y_zero_avg_norm)
        //println("x value: " + xnorm)
        //println("x value: " + xnorm_1)
        //-- INL calculation
        var dnl = (((x1 - x0) / v_lsbideal) - 1) // maxValue
        dnl
    }

    dnlgraph
  }

  def getSamplingFrequency = {
    this.xWaveform match {

      case null if (multiGraph.isDefined && multiGraph.get.waveformParameters != null) =>

        println("Found X increment on waveform")
        1 / multiGraph.get.waveformParameters.xIncrement.data

      case null =>
        0

      case wf =>
        1 / wf.xIncrement

    }
  }

  /**
   *
   */
  def filterBandPass(samplingRate: Long, lowfreqStr: String, highfreqStr: String) = {

    // Convert strings
    val lowfreq = XYGraph.stringToDouble(lowfreqStr)
    val highfreq = XYGraph.stringToDouble(highfreqStr)

    val sampling = this.xWaveform match {

      case null if (multiGraph.isDefined && multiGraph.get.waveformParameters != null) =>

        println("Found X increment on waveform")
        1 / multiGraph.get.waveformParameters.xIncrement.data

      case null => samplingRate

      case wf =>
        1 / wf.xIncrement

    }
    println("Sampling rate is: " + sampling)

    val lfeq = lowfreq * 0.5 / sampling
    val hfeq = highfreq * 0.5 / sampling

    var bandcoeffs = IirFilterDesignFisher.design(
      FilterPassType.bandpass,
      FilterCharacteristicsType.butterworth,
      2,
      -1, // ignore
      lfeq,
      hfeq) // ignore

    var bandfilter = new IirFilter(bandcoeffs)

    var lpcoeffs = IirFilterDesignFisher.design(
      FilterPassType.lowpass,
      FilterCharacteristicsType.butterworth,
      2,
      -1, // ignore
      lfeq,
      0) // ignore

    var hpcoeffs = IirFilterDesignFisher.design(
      FilterPassType.highpass,
      FilterCharacteristicsType.butterworth,
      2,
      -1, // ignore
      hfeq,
      0) // ignore

    var lpfilter = new IirFilter(lpcoeffs)
    var hpfilter = new IirFilter(hpcoeffs)

    this.mapValueArr {
      case (x, y) =>

        lpfilter.step(y)
      // hpfilter.step(lpfilter.step(y))
      //bandfilter.step(y)
    }
  }

  // Debug
  //--------------------

  def debugPrint = {

    points.foreach {
      point =>
        println("Point x: " + point.X)

        // Y is a list of Double points, in case you have more than one point per X
        println("Point y: " + point.Y.toList)

        // You can go over the Lists of Y values
        point.Y.foreach {
          yValue =>
            var yFloat = yValue.data

        }
    }

  }

  def toJFreeChartSeriesValues = {
    this.hasXYValues match {
      case true =>
        var values = getXYValues
        Array[Array[Double]](values.map { case (x, y) => x }.toArray, values.map { case (x, y) => y }.toArray)

      case false =>
        Array[Array[Double]]((0 until getYValues.size).map(_.toDouble).toArray, getYValues.toArray)

    }
  }

  def toJreeChartXYDataSet = {
    var xydataset = new DefaultXYDataset()
    xydataset.addSeries(name, toJFreeChartSeriesValues)

    /*this.hasXYValues match {
      case true =>
        var values = getXYValues
        var data = Array[Array[Double]](values.map{case (x,y) => x}.toArray,values.map{case (x,y) => y}.toArray)
       
        
      case false =>
        var data = Array[Array[Double]]((0 until getYValues.size).map(_.toDouble).toArray, getYValues.toArray)
        xydataset.addSeries(name, data)
        
    }*/

    xydataset

  }
  def toJreeChartXYPlot = {

    val p = new XYPlot(toJreeChartXYDataSet, new NumberAxis(name), new NumberAxis(name), new XYSplineRenderer)
    p
  }

  def toJFreeChart = {

    var xydataset = new DefaultXYDataset()

    SwingUtilities.invokeLater(new Runnable {
      def run = {
        var frame = new JFrame
        frame.setSize(800, 600)

        // Create Data
        //--------------
        var data = Array[Array[Double]]((0 until getYValues.size).map(_.toDouble).toArray, getYValues.toArray)
        xydataset.addSeries("Values", data)

        // Create Graph
        //------------------
        var graphName = name match {
          case name if (name == null || name.toString == "") => "Graph Values"
          case other                                         => other.toString()
        }

        /*var lineChart = ChartFactory.createLineChart(
          graphName,
          "X", "Y",
          dataset,
          PlotOrientation.VERTICAL,
          true, true, false);*/
        var lineChart = ChartFactory.createXYLineChart(
          graphName,
          "X", "Y",
          xydataset,
          PlotOrientation.VERTICAL,
          true, true, false);

        //var chart =  new JFreeChart

        //lineChart.

        // Add Chart panel to North of frame
        var chartPanel = new ChartPanel(lineChart);
        frame.getContentPane.add(chartPanel, BorderLayout.CENTER)

        // Output Buttons
        //----------------------
        var buttonPane = new JPanel();
        frame.getContentPane.add(buttonPane, BorderLayout.SOUTH)
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Find Inkscape
        //-----------------
        var inkscapeLocations = List(
          new File("C:\\Program Files (x86)\\Inkscape\\inkscape.exe"),
          new File("C:\\Program Files\\Inkscape\\inkscape.exe"))
        var inkscape = inkscapeLocations.find(_.exists())

        //-- Add checkbox for PDF Save
        var pdfSaveBox = inkscape match {
          case None =>
            var b = new JCheckBox("Cannot Save as SVG+PDF, no inkscape", false)
            b.setEnabled(false)
            b
          case Some(_) => new JCheckBox("Save as SVG+PDF", true)
        }
        buttonPane.add(pdfSaveBox)

        //-- SVG
        //--------------
        var svgButton = new JButton("Save SVG")
        buttonPane.add(svgButton)
        svgButton.addActionListener(new ActionListener {
          def actionPerformed(e: ActionEvent) = {

            //-- Ask Wehere to save
            var fc = new JFileChooser();
            fc.setCurrentDirectory(new File("").getCanonicalFile)
            fc.addChoosableFileFilter(new FileFilter {
              def accept(f: File) = f.getName.endsWith(".svg")
              def getDescription = "SVG Filter"
            })
            fc.showSaveDialog(frame) match {
              case JFileChooser.APPROVE_OPTION =>
                var saveFile = fc.getSelectedFile

                //-- Draw chart in SVG
                var svgGraphic = new SVGGraphics2D(frame.getWidth, frame.getHeight);
                //lineChart.setElementHinting(true);
                lineChart.draw(svgGraphic, new Rectangle(frame.getWidth, frame.getHeight));

                SVGUtils.writeToSVG(saveFile, svgGraphic.getSVGElement)

                //-- Convert to PDF if Inkscape is found
                inkscape match {
                  case Some(is) if (pdfSaveBox.isSelected()) =>

                    //-- PDF File preparation
                    var pdfFile = new File(saveFile.getParentFile, saveFile.getName.replace(".svg", ".pdf"))

                    var isProcessBuilder = new ProcessBuilder
                    isProcessBuilder.directory(saveFile.getParentFile)
                    isProcessBuilder.inheritIO()
                    isProcessBuilder.command(is.getCanonicalPath, "-f", saveFile.getCanonicalPath, "-A", pdfFile.getCanonicalPath)

                    //-- Run
                    var process = isProcessBuilder.start()
                    process.waitFor()

                  case other =>
                }
              case _ =>

            }

          }
        })

        // Infos Panel
        //-----------------
        var infoPanel = new JPanel
        infoPanel.setBorder(BorderFactory.createTitledBorder("Statistics"))
        infoPanel.setLayout(new GridBagLayout)
        frame.getContentPane.add(infoPanel, BorderLayout.EAST)

        // set GUi components in a list and loop over it to build the grid automatically
        List(
          (new JLabel("Max:"), new JLabel(getMaxY.toString())),
          (new JLabel("Min:"), new JLabel(getMinY.toString())),
          (new JLabel("Peak-Peak:"), new JLabel((getMaxY - getMinY).toString()))).zipWithIndex.foreach {
            case ((left, right), i) =>

              var cstr = new GridBagConstraints
              cstr.gridy = i
              cstr.gridx = 0
              infoPanel.add(left, cstr)

              cstr = new GridBagConstraints
              cstr.gridy = i
              cstr.gridx = 1
              infoPanel.add(right, cstr)
          }

        // Display
        //----------------
        RefineryUtilities.centerFrameOnScreen(frame)
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        frame.setVisible(true)
      }
    })

    xydataset

    // frame.show()
    //frame.setTitle(arg0)

  }

  // Outputs
  //---------------
  def toYTextLines(outputFile: File, precision: Int = 4, locale: Locale = Locale.getDefault) = {

    //-- Create dir
    outputFile.getCanonicalFile.getParentFile.mkdirs()

    //-- TeaIOUtils is an old utility class
    //-- Use it to write CSV
    TeaIOUtils.writeToFile(outputFile, toYTextLinesString(precision, locale))

  }
  def toYTextLinesString(precision: Int = 4, locale: Locale = Locale.getDefault) = {

    toYTextLinesList(precision, locale).mkString("\n")
  }

  /**
   * Returns list of each value formatted as text
   */
  def toYTextLinesList(precision: Int = 4, locale: Locale = Locale.getDefault) = {
    val f = new DecimalFormat
    f.setMaximumFractionDigits(precision)
    f.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale))
    f.setGroupingUsed(false)

    getYValues.map {
      y =>
        s"""${f.format(y)}"""
    }.toList
  }

  def toCSVString(sep: String = ",", precision: Int = 12, xtime: Boolean = false, locale: Locale = Locale.getDefault) = {

    val f = new DecimalFormat
    f.setMaximumFractionDigits(precision)
    f.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(locale))
    f.setGroupingUsed(false)

    // XMultiplier
    //----------
    val xmultiplierOption = findByExceptionNotNull(List(() => xWaveform.xIncrement, () => multiGraph.get.waveformParameters.xIncrement))
    val xmultiplier = xtime match {
      case true if (xmultiplierOption.isEmpty) =>
        sys.error("Cannot Create X values if no Xincrement can be found")
      case true =>
        println("X Increment: " + xmultiplierOption.get)
        xmultiplierOption.get.data
      case false => 1.0
    }

    /*val csvSeparator = DecimalFormatSymbols.getInstance.getDecimalSeparator match {
      case ',' => ';'
      case other => sep
    }*/

    getYValues.zipWithIndex.map {
      case (y, x) =>
        s""" ${f.format(x.toDouble * xmultiplier)}${sep}${f.format(y)} """.trim()
    }.mkString("\n")
  }

  /**
   * Only support X/Y for now, no multiple Y
   */
  def toCSV(outputFile: File, sep: String = ",", precision: Int = 4, xtime: Boolean = false, locale: Locale = Locale.getDefault) = {

    //-- Create dir
    outputFile.getCanonicalFile.getParentFile.mkdirs()

    //-- TeaIOUtils is an old utility class
    //-- Use it to write CSV
    TeaIOUtils.writeToFile(outputFile, toCSVString(sep, precision, xtime, locale))

  }

  // Data save reconciliation
  //----------------

  /**
   * If external values are set, don't output raw values or points
   */
  override def streamOut(du: DataUnit) = {

    if (this.externalFile != null && this.externalType != null) {
      val p = this.rawPoints
      val v = this.rawValues
      val point = this.points.toList
      this.rawPoints = null
      this.rawValues = null
      this.points.clear
      try {
        super.streamOut(du)
      } finally {
        this.rawPoints = p
        this.rawValues = v
        this.points ++= point
      }

    } else {
      super.streamOut(du)
    }

  }

}

object XYGraph extends TLogSource {

  def apply() = new XYGraph
  def apply(values: Iterable[Int]): XYGraph = {
    var graph = new XYGraph()
    values.zipWithIndex.foreach {
      case (value, index) =>
        var xp = graph.points.add
        xp.X = index
        xp.Y.add.data = value
    }

    graph
  }

  def apply(values: Array[Double]): XYGraph = {
    var graph = new XYGraph()
    graph.setYValues(values)

    graph
  }

  def withName(values: Iterable[Int], name: String) = {
    val g = XYGraph(values)
    g.name = name
    g
  }

  def withName(values: Array[Double], name: String) = {
    val g = XYGraph(values)
    g.name = name
    g
  }

  def apply(wf: XWaveform) = {
    val graph = new XYGraph
    graph.xWaveform = wf

    graph
  }

  def apply(f: java.io.File): XYGraph = {
    apply(f.toURI().toURL())
  }
  def apply(url: java.net.URL): XYGraph = {

    // Instanciate
    var res = new XYGraph

    // Set Stax Parser and streamIn
    var io = com.idyria.osi.ooxoo.core.buffers.structural.io.sax.StAXIOBuffer(url)
    res.appendBuffer(io)
    io.streamIn

    // Return
    res

  }

  def apply(xml: String) = {

    // Instanciate
    var res = new XYGraph

    // Set Stax Parser and streamIn
    var io = com.idyria.osi.ooxoo.core.buffers.structural.io.sax.StAXIOBuffer(xml)
    res.appendBuffer(io)
    io.streamIn

    // Return
    res

  }

  /**
   * Converts string like 2Mhz to a double
   */
  def stringToDouble(str: String) = {

    //-- Normalize to lower case and remove spaces
    var normalized = str.trim.toLowerCase().filter(_ != ' ').filter(_ != '\t')

    //-- Take first numbers
    val result = normalized.span(c => c.isDigit || c == '.' || c == ',') match {

      case (numberStr, "hz") =>

        numberStr.toDouble

      case (numberStr, "khz") =>

        numberStr.toDouble * Math.pow(10, 3)

      case (numberStr, "mhz") =>

        numberStr.toDouble * Math.pow(10, 6)
      case (numberStr, "ghz") =>

        numberStr.toDouble * Math.pow(10, 9)

      case (numberStr, "thz") =>

        numberStr.toDouble * Math.pow(10, 12)

      case (numberStr, "") =>

        numberStr.toDouble

      case (numberStr, marker) if (marker != "") =>

        logWarn[XYGraph](s"""Marker not recognised: $marker""")

        numberStr.toDouble

    }

    // println(s"Number=$number, Marker=$marker")

    //-- 
    // println(s"$normalized -> $result")
    result

  }

}