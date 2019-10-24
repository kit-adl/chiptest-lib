package kit.edu.ipe.adl.chiptest.measurement.data

import java.io.File
import java.io.FileOutputStream

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.odfi.instruments.nivisa.keysight.waveform.Waveform

import com.idyria.osi.ooxoo.core.buffers.structural.xelement
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.odfi.instruments.compress.CompressModule
import org.odfi.instruments.data.XWaveform
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import java.io.FileInputStream
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.odfi.instruments.compress.MemoryArchiveEntry
import java.io.ByteArrayInputStream
import com.idyria.osi.tea.io.TeaIOUtils
import org.jfree.data.statistics.HistogramDataset
import org.jfree.chart.ChartFactory
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.JFreeChart
import kit.edu.ipe.adl.chiptest.measurement.jfreechart.JFreeChartUtils
import org.apache.commons.compress.archivers.zip.ZipFile
import com.idyria.osi.ooxoo.core.buffers.structural.ElementBuffer
import com.idyria.osi.ooxoo.core.buffers.structural.io.sax.STAXSyncTrait
import org.odfi.instruments.compress.ZipMemoryArchiveEntry
import com.idyria.osi.ooxoo.core.buffers.structural.DataUnit
import java.io.DataOutputStream
import org.odfi.instruments.data.WaveformParameters
import java.io.DataInputStream
import com.idyria.osi.tea.listeners.ListeningSupport
import com.idyria.osi.tea.progress.ProgressSupport
import java.util.Locale
import org.odfi.indesign.core.module.swing.SwingUtilsTrait
import kit.ipe.adl.rfg3.model.Register
import org.jfree.chart.plot.CombinedDomainXYPlot
import org.jfree.chart.plot.Plot
import javax.swing.JFrame
import org.jfree.chart.ChartPanel
import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.BoxLayout
import javax.swing.JButton
import org.jfree.ui.RefineryUtilities
import javax.swing.BorderFactory
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter
import java.awt.Rectangle
import org.jfree.graphics2d.svg.SVGGraphics2D
import org.jfree.graphics2d.svg.SVGUtils
import org.jfree.chart.renderer.xy.XYStepRenderer
import org.jfree.chart.plot.CombinedRangeXYPlot

@xelement(name = "MultiXYGraph")
class MultiXYGraph extends MultiXYGraphTrait with ProgressSupport with SwingUtilsTrait {

  // ReadIN
  //----------------

  /**
   * After the Multigraph is read in, set the reference to all XYGrah
   * This is essential to populate XY Graph waveforms from separated files
   */
  override def streamIn(du: DataUnit) = {
    super.streamIn(du)
    du.isHierarchyClose match {
      case true =>
        this.xYGraphs.foreach {
          g =>
            g.multiGraph = Some(this)
        }
      case false =>
    }
  }

  // Graph Mapping
  //---------------------

  /**
   * First Populate graphs
   * then map them
   */
  def mapGraphs(forgetSourcePoints: Boolean = false)(f: XYGraph => XYGraph) = {

    //populateAllXYGraph

    progressInit("Mapping Graphs")
    val newmgraph = new MultiXYGraph
    newmgraph.name = name + "(Mapped)"
    xYGraphs.zipWithIndex.foreach {
      case (g, i) =>

        g.populateFromWaveform()

        val newg = newmgraph.addXYGraph(g.name + "(Mapped)", f(g))

        progressUpdate(i * 100 / xYGraphs.size)

        if (forgetSourcePoints) {
          g.cleanPoints
        }

    }

    newmgraph

  }

  /**
   * Map A certain graph and plot both
   */
  def mapGraphAndPlotBoth(g: Int)(f: XYGraph => XYGraph) = {

    getGraphPopulated(g) match {
      case Some(g) => g.mapAndPlotBoth(f)
      case None    => sys.error(s"Cannot map and plot index $g")
    }
  }

  // Get
  //-------------
  def getGraphPopulated(index: Int) = {

    xYGraphs.get(index) match {
      case Some(g) =>
        g.getYValues
        Some(g)
      case None => None
    }

  }

  def getGraphPopulatedWithTime(index: Int) = {

    xYGraphs.get(index) match {
      case Some(g) =>
        g.getXYValues
        Some(g)
      case None => None
    }

  }

  // Analysis
  //------------------
  def getMinOfAll = {

    populateAllXYGraph

    //-- Find Min
    progressInit("Getting Minimum Values...")
    val total = this.xYGraphs.size
    var allMins = this.xYGraphs.zipWithIndex.map {
      case (graph, i) =>
        progressUpdate(100.0 * i / total)
        graph.getMinY

    }

    //-- Create new graph
    //var g = new XYGraph()
    allMins.toArray
  }

  def getMaxOfAll = {

    populateAllXYGraph

    //-- Find Max
    progressInit("Getting Maximum Values...")
    val total = this.xYGraphs.size
    var allMaxs = this.xYGraphs.zipWithIndex.map {
      case (graph, i) =>

        progressUpdate(100 * i / total)

        graph.getMaxY

    }

    //-- Create new graph
    //var g = new XYGraph()
    allMaxs.toArray
  }

  def saveAllToCSVArchive(tfile: File, precision: Int = 6, parallel: Boolean = true) = {

    progressInit("Saving all to CSV...")

    //-- Prepare Archive
    //--------
    val mg = new MultiXYGraph
    mg.name = name

    mg.toArchive(tfile, erase = true)

    // Get collection
    val collection = parallel match {
      case true  => xYGraphs
      case false => xYGraphs.toParArray
    }
    //populateAllXYGraph
    val total = this.xYGraphs.size
    var current = 0
    collection.foreach {
      case g =>

        mg.updateArchiveWith(g.name + ".csv", g.toCSVString(sep = ";", precision = precision).getBytes)
        g.cleanPoints

        progressUpdate(100 * current / total)
        current += 1

    }

    mg.finishArchive

  }

  /**
   * Save all Graphs to a single YLine File, each file has one column
   * 
   * If graphs have different sizes, use the smaller one
   */
  def saveAllToYLinesFile(outputFile: File, precision: Int = 6,sep:String= ",",locale: Locale = Locale.getDefault) = {
    
    val graphsAsYStrings = xYGraphs.map {
      graph => 
        graph.toYTextLinesList(precision,locale)
    }
    
    // Map each point index to a list of the values for all the graphs at this index
    // (zip operation)
    val pointsCount = graphsAsYStrings.map(_.size).min
    
    val zippedLines = (0 until pointsCount).map {
      i => 
        // Take values for first line
        graphsAsYStrings.map {
          values => values(i)
        }.toList.mkString(sep)
    }
    
    val textLines = zippedLines.mkString("\n")
    
    // Save to File
    TeaIOUtils.writeToFile(outputFile,textLines)
    //Locale
    //graphsAsYStrings.zipAll
    
    
    
  }
  
  /**
   * Save All Graphs each to one YLines file, and all the files in an archive
   */
  def saveAllToYLinesArchive(tfile: File, precision: Int = 6, parallel: Boolean = true) = {

    progressInit("Saving all to Y value files...")

    //-- Prepare Archive
    //--------
    val mg = new MultiXYGraph
    mg.name = name
    mg.toArchive(tfile, erase = true)

    // Get collection
    //----------
    val collection = parallel match {
      case true  => xYGraphs
      case false => xYGraphs.toParArray
    }

    // Converstion loop with progress
    val total = this.xYGraphs.size
    var current = 0
    collection.foreach {
      case g =>

        mg.updateArchiveWith(g.name + ".csv", g.toYTextLinesString(precision).getBytes)
        g.cleanPoints

        progressUpdate(100 * current / total)
        current += 1

    }

    mg.finishArchive

  }

  /**
   * File with all MAX separated by a space
   */
  def maxOfAllToROOTFile(f: File) = {

    TeaIOUtils.writeToFile(f, getMaxOfAll.mkString(" "))
  }

  /**
   * File with all MAX separated by a space
   */
  def minOfAllToROOTFile(f: File) = {

    TeaIOUtils.writeToFile(f, getMinOfAll.mkString(" "))
  }

  // GUI Analysis
  //-----------------
  def plotMaxHistogram(bins: Int) = {

    plotHistogramForValues(s"Max (bins=$bins)", this.getMaxOfAll, bins)

  }

  def plotMinHistogram(bins: Int) = {

    plotHistogramForValues(s"Min (bins=$bins)", this.getMinOfAll, bins)

  }

  def plotHistogramForValues(name: String, values: Array[Double], bins: Int) = {

    //-- Create dataset
    var histoDataset = new HistogramDataset
    histoDataset.addSeries(name, values, bins)

    //-- Create chart
    var histoChart = ChartFactory.createHistogram(name, "Amplitude", "Frequency", histoDataset, PlotOrientation.VERTICAL, true, true, true)

    //-- PLot
    JFreeChartUtils.plotChart(histoChart)
  }

  /**
   * Plots all graphs on one
   */
  def plotJFreeChartAllGraphs = {

    xYGraphs.headOption match {
      case Some(first) =>

        val dataset = first.toJFreeChart

        xYGraphs.drop(1).foreach {
          g =>
           
        
            onSwingThreadLater {
              dataset.addSeries(g.name,g.toJFreeChartSeriesValues)
            }
            
            /*var mappedData = Array[Array[Double]]((0 until g.getYValues.size).map(_.toDouble).toArray, g.getYValues.toArray)

            onSwingThreadLater {
              dataset.addSeries(g.name, mappedData)
            }*/
        }

      case None =>
    }

  }

  /**
   * Plot all the graphs as sub plots
   */
  def plotJFreeChartAllGraphsSubplots(binary:Boolean=false) = {

    // Map all graphs to XYPlot
    val plots = xYGraphs.sortBy(g => g.name.toString).map {
      g =>
        g.toJreeChartXYPlot match {
          case p if (binary) => 
            
            p.setRenderer(new XYStepRenderer)
            p
          case p => p 
        }
    }

    // Create combined plot
    var combined = new CombinedDomainXYPlot
    plots.foreach {
      p =>
        combined.add(p)
    }

    plotJFreeCharPlot(combined)

  }

  def plotJFreeCharPlot(plot: Plot) = {
    var chart = new JFreeChart(plot)

    var frame = new JFrame
    frame.setSize(800, 600)

    // Add Chart panel to North of frame
    var chartPanel = new ChartPanel(chart);
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
            chart.draw(svgGraphic, new Rectangle(frame.getWidth, frame.getHeight));

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

    // Display
    //----------------
    RefineryUtilities.centerFrameOnScreen(frame)
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.setVisible(true)

  }

  // RFG interface
  //----------------

  /**
   * Import all the values as values of the single fields of the register
   */
  def importRegisterFieldsValues(register: Register, values: Array[Long]) = {

    // Map values
    val mappedByField = values.map {
      value =>

        register.setMemory(value)
        register.fields.map {
          f =>

            (f.name, f.memoryValue)
        }

    }.flatten

    val groupedByField = mappedByField.groupBy(_._1).map { case (name, values) => (name, values.map(e => e._2)) }

    //val groupedByField = mappedByField.toMap

    // Create graphs
    groupedByField.foreach {
      case (name, values) =>

        val g = this.createXYGraph(name)
        g.setYValues(values.map(_.toDouble))

    }

  }

  // XY Graph
  //-------------

  def populateAllXYGraph = {

    progressInit("Populating Graphs")
    //println(s"Populating...")
    var i = 0
    var total = this.xYGraphs.size
    this.xYGraphs.foreach {
      g =>
        //println(s"Doing $i")
        i += 1
        g.populateFromWaveform()
        progressUpdate(100.0 * i / total)

      //Thread.sleep(2000)
    }
    // println(s"Done")
  }

  def addXYGraph(name: String, g: XYGraph) = {

    this.xYGraphs += g
    g.multiGraph = Some(this)
    g.name = name
    g

  }

  def createXYGraph(name: String) = addXYGraph(name, new XYGraph)

  def getXYGraph(name: String) = {
    this.xYGraphs.find(_.name.toString == name)
  }
  
  def clearAllXYGraphs = {
    this.xYGraphs.clear()
  }

  // Output Product
  //---------------------

  /**
   * Generates XY Graphs CSV and save the to local archive
   *
   *
   *
   */
  def archiveXYGraphsCSV(sep: String = ",") = targetArchiveInfo match {
    case None =>
      sys.error("Cannot Archive Data if the MUltiXY Graph has no archive defined, don't forget to setup using toArchive function")

    case Some(infos) =>

      this.xYGraphs.foreach {
        graph =>

          var csv = graph.toCSVString(sep)

          // Set output product to local graph
          var outputProduct = graph.outputProducts.add
          outputProduct.file = graph.name + "/" + graph.name + ".csv"
          outputProduct.fileType = "csv"

          // Save to archive
          updateArchiveWith(outputProduct.file, csv.getBytes)

      }

  }

  // Waveform add utils
  //--------------
  def splitXYWafevorms = {

    var i = 0
    this.xYGraphs.foreach {
      xyGraph =>

        xyGraph.xWaveform match {
          case null =>

          case wf =>

            println(s"Splitting $i....")

            // Save XML in archive
            this.saveXMLToArchive(xyGraph.name + "-wf.xml", wf)

            // Remove from graph
            xyGraph.xWaveform = null

            xyGraph.externalFile = xyGraph.name + "-wf.xml"
            xyGraph.externalType = "xwaveform"

            i += 1
        }
    }

  }

  def splitXYGraphsToBinaryValues = {

    progressInit("Converting XY Graph data to file in archive...")
    this.xYGraphs.zipWithIndex.foreach {
      case (g, i) =>

        g.pointsYToExternalRawValues
        g.cleanPoints
        progressUpdate(i * 100 / xYGraphs.size)
    }

  }

  /**
   * Removes the XWaveform from XYGraph and saves points as real values to an external binary file
   */
  def splitXYWafevormsToBinaryValues = {

    var i = 0

    //-- Take first graph with waveforms and save parameters
    this.xYGraphs.find(_.xWaveform != null) match {
      case Some(xygraph) =>

        //-- Save parameters
        xygraph.xWaveform.waveformParameters match {
          case null =>

            this.waveformParameters = new WaveformParameters
            this.waveformParameters.xReference = xygraph.xWaveform.xReference
            this.waveformParameters.xIncrement = xygraph.xWaveform.xIncrement
            this.waveformParameters.xOrigin = xygraph.xWaveform.xOrigin
            this.waveformParameters.xUnit = xygraph.xWaveform.xUnit
            this.waveformParameters.yReference = xygraph.xWaveform.yReference
            this.waveformParameters.yIncrement = xygraph.xWaveform.yIncrement
            this.waveformParameters.yOrigin = xygraph.xWaveform.yOrigin
            this.waveformParameters.yUnit = xygraph.xWaveform.yUnit

          case params =>
            this.waveformParameters = params
        }
      case other =>

        //-- Split
        this.xYGraphs.foreach {
          xyGraph =>

            xyGraph.xWaveform match {
              case null =>

              case wf =>

                println(s"Splitting $i....")
                xyGraph.externalFile = xyGraph.name + ".raw"
                xyGraph.externalType = "raw-values"

                // Save XML in archive
                this.saveDoublesToArchive(xyGraph.externalFile, wf.getRealData)
                // this.saveXMLToArchive(xyGraph.name+"-wf.xml", wf)

                // Remove from graph
                xyGraph.xWaveform = null

                i += 1
            }
        }

      //-- If none , splitting does not make sense
      case None =>

    }

  }

  /**
   * If the MG has no saved XWaveform parameters, save from this WF
   */
  def addXWaveformGraph(name: String, wf: XWaveform) = {

    //-- Save parameters
    this.waveformParameters match {
      case null =>

        this.waveformParameters = new WaveformParameters
        this.waveformParameters.xReference = wf.xReference
        this.waveformParameters.xIncrement = wf.xIncrement
        this.waveformParameters.xOrigin = wf.xOrigin
        this.waveformParameters.xUnit = wf.xUnit
        this.waveformParameters.yReference = wf.yReference
        this.waveformParameters.yIncrement = wf.yIncrement
        this.waveformParameters.yOrigin = wf.yOrigin
        this.waveformParameters.yUnit = wf.yUnit

      case other =>
    }

    //-- Save XWaveform
    var graph = this.xYGraphs.add
    graph.name = name
    graph.multiGraph = Some(this)

    graph.xWaveform = wf

    graph
  }

  // Archive Store
  //-----------------
  var targetArchiveInfo: Option[(File, ArchiveOutputStream)] = None
  var sourceArchiveInfo: Option[(File, ArchiveInputStream)] = None
  var sourceEntries = List[MemoryArchiveEntry]()

  def getTargetArchiveStream = targetArchiveInfo match {
    case Some((file, stream)) => stream
    case None                 =>
  }

  def openArchive(archiveFile: File) = {

    //-- Create Archive
    archiveFile.getName.endsWith("zip") match {
      case true =>

        var zf = new ZipFile(archiveFile)

        //-- Save
        var archive = new ArchiveStreamFactory().createArchiveInputStream("zip", new FileInputStream(archiveFile))
        this.sourceArchiveInfo = Some(archiveFile, archive)
        archive.close()

        //-- Load entries
        var entries = zf.getEntriesInPhysicalOrder
        var currentEntry: Option[ZipArchiveEntry] = Some(entries.nextElement())
        while (currentEntry.isDefined) {

          //currentEntry.get

          //println("Can read entry: " + archive.canReadEntryData(currentEntry))

          // println("Entry: " + currentEntry.get.getName + ", size=" + currentEntry.get.getSize)

          /*var is = zf.getInputStream(currentEntry.get)
          var data = TeaIOUtils.swallowBytes(is, currentEntry.get.getSize.toInt)
          is.close()*/

          //var data = TeaIOUtils.swallow(is)
          var memoryEntry = new ZipMemoryArchiveEntry(zf, currentEntry.get)

          sourceEntries = sourceEntries :+ memoryEntry

          //sourceEntries = sourceEntries :+ memoryEntry
          entries.hasMoreElements() match {
            case true =>
              currentEntry = Some(entries.nextElement())
            case false =>
              currentEntry = None
          }

        }
      // EOF Loop

      //zf.close()

      case false =>
        var archive = new ArchiveStreamFactory().createArchiveInputStream("tar", CompressModule.getFileCompressInputStream(archiveFile))

        //-- Save
        this.sourceArchiveInfo = Some(archiveFile, archive)

        //-- Load entries
        var currentEntry = archive.getNextEntry
        while (currentEntry != null) {

          //currentEntry.get

          //println("Can read entry: " + archive.canReadEntryData(currentEntry))

          //println("Entry: " + currentEntry.getName + ", size=" + currentEntry.getSize + ", available=" + archive.available())

          var zi = currentEntry.asInstanceOf[ZipArchiveEntry]
          zi.getExtraFields.foreach {
            f =>
            //println("Field: " + f.getLocalFileDataLength)
          }

          /*var memoryEntry = currentEntry.getSize match {
            case -1 =>

              //var data = new Array[Byte](0)

              var data = TeaIOUtils.swallowStream(archive)
              println(s"Read ${data.length} bytes from archive: " + archive.read().toChar)

              new MemoryArchiveEntry(currentEntry, data)
            case other =>
              var data = new Array[Byte](currentEntry.getSize.toInt)
              archive.read(data)
              new MemoryArchiveEntry(currentEntry, data)
          }

          //var memoryEntry = new MemoryArchiveEntry(currentEntry, data)
          sourceEntries = sourceEntries :+ memoryEntry*/

          currentEntry = archive.getNextEntry.asInstanceOf[ZipArchiveEntry]
        }

        //println("Done reading archive")
        archive.close()

    }

  }

  def loadXMLFileFromSource[ET <: STAXSyncTrait](entryName: String, elt: ET): Option[ET] = {

    sourceEntries.find {
      entry => entry.getName == entryName
    } match {
      case Some(entry) =>

        elt.fromInputStream(new ByteArrayInputStream(entry.getData))
        entry.clear

        Some(elt)

      case None =>
        None
    }

  }

  def loadDoubleArrayFromSource(entryName: String): Option[Array[Double]] = {

    sourceEntries.find {
      entry => entry.getName == entryName
    } match {
      case Some(entry) =>

        //-- Read
        var is = new DataInputStream(new ByteArrayInputStream(entry.getData))
        var data = Vector[Double]()
        while (is.available() > 0) {
          data = data :+ is.readDouble
        }

        is.close
        entry.clear

        Some(data.toArray)

      case None =>
        None
    }
  }

  /**
   * Loads the XML file for theis multigraph from input archive
   * Name of XML is same as base file name (without extension) + .xml
   */
  def loadXMLFromArchive = sourceArchiveInfo match {
    case Some((file, inputStream)) =>

      var xmlName = file.getName.takeWhile(_ != '.') + ".xml"

      //println("Searching for Entry: " + xmlName)

      this.sourceEntries.find {
        archEntry => archEntry.getName == xmlName
      } match {
        case Some(entry) =>

          this.fromInputStream(new ByteArrayInputStream(entry.getData))

        case None =>
          sys.error("Cannot find an entry in archive with same name as file + .xml, check the format is correct or load explicitely")
      }

    case None => sys.error("Cannot load XML from archive, no Archive was opened")
  }

  /**
   * Write to archive
   * Use erase input param to recreate the archive
   */
  def toArchive(archiveFile: File, erase: Boolean = false) = {

    if (archiveFile.exists() && erase) {
      archiveFile.delete()
    }

    //-- Create Archive
    var archive = archiveFile.getName.endsWith("zip") match {
      case true =>
        new ArchiveStreamFactory().createArchiveOutputStream("zip", new FileOutputStream(archiveFile))
      case false =>
        new ArchiveStreamFactory().createArchiveOutputStream("tar", CompressModule.getFileCompressOutputStream(archiveFile))
    }

    this.targetArchiveInfo = Some(archiveFile, archive)

  }

  private def createEntry(name: String, size: Long) = this.targetArchiveInfo.get._1.getName.endsWith("zip") match {
    case true =>
      var e = new ZipArchiveEntry(name)
      e.setSize(size)
      e
    case false =>
      var e = new TarArchiveEntry(name)
      e.setSize(size)
      e
  }

  private def createEntryNoLength(name: String) = this.targetArchiveInfo.get._1.getName.endsWith("zip") match {
    case true =>
      var e = new ZipArchiveEntry(name)
      e
    case false =>
      var e = new TarArchiveEntry(name)
      e
  }

  def saveXMLToArchive(name: String, elt: STAXSyncTrait) = {

    var archive = this.targetArchiveInfo.get._2

    var entry = this.createEntryNoLength(s"$name")

    //-- Put XML
    archive.putArchiveEntry(entry)
    elt.toOutputStream(archive)

    //-- Close entry
    archive.closeArchiveEntry()

  }

  def saveDoublesToArchive(name: String, arr: Array[Double]) = {

    var archive = this.targetArchiveInfo.get._2
    var entry = this.createEntryNoLength(s"$name")

    //-- Put archive
    archive.putArchiveEntry(entry)

    //-- Save data to archive
    var os = new DataOutputStream(archive)
    arr.foreach(os.writeDouble(_))

  }

  /**
   * Synchronised to enable parallel
   */
  def updateArchiveWith(entryName: String, bytes: Array[Byte]) = this.synchronized {

    var archive = this.targetArchiveInfo.get._2

    //-- Create entry
    var newEntry = createEntry(entryName, bytes.length)
    archive.putArchiveEntry(newEntry)
    archive.write(bytes)
    archive.closeArchiveEntry()

  }

  /**
   * Save XML to archive and finalize archive by compressing it if necessary
   */
  def finishArchive = {

    var xmlName = this.targetArchiveInfo.get._1.getName.takeWhile(_ != '.') + ".xml"

    //-- Create XML
    //var str = this.toXMLString

    // println("STR Length: "+str.getBytes.length)

    //-- Put XML
    var archive = this.targetArchiveInfo.get._2

    //-- Create Entry
    //var entry  = new ZipArchiveEntry(xmlName)
    //entry.setSize(str.getBytes.length)
    var entry = createEntryNoLength(xmlName)

    //-- Put entry
    archive.putArchiveEntry(entry)

    //-- Write XML
    this.toOutputStream(archive)
    //entry.setSize(archive.getBytesWritten)

    //println("Bytes written"+archive.getBytesWritten)

    //archive.write(str.getBytes)

    archive.closeArchiveEntry()

    //-- Finish Archive
    archive.close()

    //-- Compress if necessary

  }

}

object MultiXYGraph {

  def fromArchive(f: File) = {

    var graph = new MultiXYGraph

    graph.openArchive(f)
    graph.loadXMLFromArchive

    graph

  }

  def apply(f: File) = {

    CompressModule.isArchive(f) match {
      case true =>
        fromArchive(f)
      case false =>
        var graph = new MultiXYGraph
        graph.fromFile(f)
        graph

    }
  }

}
