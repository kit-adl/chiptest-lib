package graph.archive

import kit.edu.ipe.adl.chiptest.measurement.data.MultiXYGraph
import java.io.File
import org.odfi.instruments.data.XWaveform

object MultiGraphWaveformExample extends App {

  var outFolder = new File("target/test-out")
  outFolder.mkdirs()

  //-- Normal Graph
  var multigraph = new MultiXYGraph
  multigraph.name = "Test"

  //multigraph.toArchive(new File(outFolder, "multigraph-waveform-archive.tar"), erase = true)
  multigraph.addXWaveformGraph("test", XWaveform(new File("src/examples/resources/waveform/waveform-50000-simple.xml").toURI().toURL()))
  multigraph.addXWaveformGraph("test2", XWaveform(new File("src/examples/resources/waveform/waveform-50000-simple.xml").toURI().toURL()))
  
  multigraph.toFile(new File(outFolder,"multigraph-xwaveform-single.xml"))
  multigraph.toFile(new File(outFolder,"multigraph-xwaveform-single.xml.bz2"))
}