package kit.edu.ipe.adl.chiptest.measurement.data


import com.idyria.osi.ooxoo.model.{Element, ModelBuilder, producer, producers}
import com.idyria.osi.ooxoo.model.out.markdown.MDProducer
import com.idyria.osi.ooxoo.model.out.scala.ScalaProducer
import com.idyria.osi.ooxoo.core.buffers.structural.io.sax.STAXSyncTrait
import org.odfi.instruments.compress.XMLCompressOutput
import org.odfi.instruments.data.XWaveform

@producers(Array(
  new producer(value = classOf[ScalaProducer]),
  new producer(value = classOf[MDProducer])))
object MeasurementDataModel extends ModelBuilder {
 
 
  // Data definition
  //----------------------------
  
  val graph = "Graph" is {
    isTrait
    withTrait(classOf[XMLCompressOutput])
    attribute("name")
    attribute("creationDate") ofType "datetime"
    attribute("display") ofType "string" default "line"
    
    //-- External attributes
    attribute("externalFile") ofType "string"
    attribute("externalType") ofType "string"
    
    //-- Generated attribute to ease cleaning
    attribute("generated") default "false"
    
  } 
  
  //-- XY
  val xyGraph = "XYGraph" is {
    withTrait(graph)
    elementsStack.top.makeTraitAndUseCustomImplementation
    
     // Statistics
    "Statistics" is {
      "Stat" multiple {
        ofType("string")
        attribute("name")
      }
    }
    
    // Output Products
    //------------
    "OutputProduct" multiple {
      
      attribute("file")
      attribute("fileType")
      attribute("generatorFunction")
      
      
    }
    
    
    // Data sources
    //----------------
    
    //-- Raw Data
    "RawValues"  ofType "doublebinary"
    "RawPoints"  ofType "doublebinary"
    
    //-- Points
    "Point" multiple {
      
      "X" ofType "double"
      "Y" multiple {
        ofType("double")
      }
    }
    
    //-- Waveform
    importElement(classOf[XWaveform].getCanonicalName)
    
   
  }


  // Multi Graph
  //-----------------
  val multiGraph = "MultiXYGraph" is {
    
    // Compress interface to easily save to zip for example
    withTrait(classOf[XMLCompressOutput])
    elementsStack.top.makeTraitAndUseCustomImplementation
    
    // XYGrapgs
    importElement(xyGraph).setMultiple

    attribute("name")
    attribute("creationDate") ofType "datetime"

    // Waveform parameters
    // Saved here in case a measurement from OSCI, used to always know what was the OSCI setup like
    importElement(classOf[org.odfi.instruments.data.WaveformParameters].getCanonicalName)
    
    // Shift Register configuration state
    importElement("kit.edu.ipe.adl.chiptest.shiftregister.model.SRCommon").name = "ShiftRegister"

  }
  
 
  
  
  
}