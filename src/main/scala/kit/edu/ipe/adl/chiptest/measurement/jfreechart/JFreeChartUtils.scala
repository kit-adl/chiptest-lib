package kit.edu.ipe.adl.chiptest.measurement.jfreechart

import org.jfree.chart.JFreeChart
import javax.swing.JFrame
import org.jfree.chart.ChartPanel
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JPanel
import java.io.File
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFileChooser
import java.awt.Rectangle
import org.jfree.graphics2d.svg.SVGGraphics2D
import org.jfree.graphics2d.svg.SVGUtils
import java.awt.event.ActionEvent
import javax.swing.filechooser.FileFilter
import java.awt.event.ActionListener
import org.jfree.ui.RefineryUtilities

object JFreeChartUtils {

  def plotChart(chart: JFreeChart) = {

    //-- Create Frame
    var frame = new JFrame
    frame.setSize(800, 600)

    //-- Add Chart in middle
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
      new File("C:\\Program Files (x86)\\Inkscape\\inkscape.exe"))
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
    }) // EOF SVG

    // Display
    //----------------
    RefineryUtilities.centerFrameOnScreen(frame)
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.setVisible(true)

  }

}