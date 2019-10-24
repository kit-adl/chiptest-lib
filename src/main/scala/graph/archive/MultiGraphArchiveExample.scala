package graph.archive

import java.io.File
import java.io.FileInputStream

import com.idyria.osi.tea.io.TeaIOUtils

import kit.edu.ipe.adl.chiptest.measurement.data.MultiXYGraph

object MultiGraphArchiveExample extends App {
  
    var outFolder = new File("target/test-out")
  outFolder.mkdirs()
  
  //-- Normal Graph
  var multigraph = new MultiXYGraph
  multigraph.name = "Test"
 
  multigraph.toArchive(new File(outFolder,"multigrapharchive.tar"), erase=true)
  
  
  multigraph.name = "TestUpdate"
  
  multigraph.updateArchiveWith("wf", TeaIOUtils.swallow(new FileInputStream(new File("src/examples/resources/waveform/testwf.wf"))))
  
  
  multigraph.finishArchive
  
  //-- BZ2 Graph
  var multigraphbz2 = new MultiXYGraph
  multigraphbz2.name = "Test"
 
  multigraphbz2.toArchive(new File(outFolder,"multigrapharchive.tar.bz2"), erase=true)
  
  
  multigraphbz2.name = "TestUpdate"
  multigraphbz2.updateArchiveWith("wf", TeaIOUtils.swallow(new FileInputStream(new File("src/examples/resources/waveform/testwf.wf"))))
  
  
  multigraphbz2.finishArchive
  
  
}