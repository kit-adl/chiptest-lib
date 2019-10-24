package waveform

import org.odfi.instruments.nivisa.keysight.waveform.Waveform
import java.io.File

object WaveformZipOutExample extends App {
  
  //--
  println("Reading Waveform")
  var waveform = Waveform.fromBinaryFile(new File("src/examples/resources/waveform/testwf.wf"))
  
  //-- Save as BF Zipped
  var outFolder = new File("target/test-out")
  outFolder.mkdirs()
  
  waveform.toBinaryFile(new File(outFolder,"testwf.wf.zip"))
  waveform.toBinaryFile(new File(outFolder,"testwf.wf.gzip"))
  waveform.toBinaryFile(new File(outFolder,"testwf.wf.bz2"))
  
  
  Waveform.fromBinaryFile(new File(outFolder,"testwf.wf.zip"))
  Waveform.fromBinaryFile(new File(outFolder,"testwf.wf.gzip"))
  Waveform.fromBinaryFile(new File(outFolder,"testwf.wf.bz2"))
  
}