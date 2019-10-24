package kit.edu.ipe.adl.chiptest.measurement.rudolf

import com.idyria.osi.ooxoo.model.ModelBuilder
import com.idyria.osi.ooxoo.model.producer
import com.idyria.osi.ooxoo.model.producers
import com.idyria.osi.ooxoo.model.out.markdown.MDProducer
import com.idyria.osi.ooxoo.model.out.scala.ScalaProducer
import com.idyria.osi.ooxoo.core.buffers.structural.io.sax.STAXSyncTrait

@producers(Array(
  new producer(value = classOf[ScalaProducer]),
  new producer(value = classOf[MDProducer])))
object RudolfDataModel extends ModelBuilder {

  // Data definition
  //----------------------------

  val meas = "Measurement" is {

    withTrait(classOf[STAXSyncTrait])
    elementsStack.top.makeTraitAndUseCustomImplementation

    "Name" ofType "string"
    "Sample" ofType "string"
    "MeasurementDevice" ofType "string"
    "Description" ofType "cdata"

    "Trigger" is {
      any

      /*( 'A' to 'Z').foreach {
        c => 
          s"Trigger$c" ofType "string"
          
      }*/
      "Configuration" is {
        any
      }

      // USe common SR configuration
      importElement("kit.edu.ipe.adl.chiptest.shiftregister.model.SRCommon")

    }

    // Events
    "Event" multiple {
      "WaveForm" multiple {

        attribute("Name")
         attribute("valid") ofType "boolean"
         
        attribute("BytesPerPoint") ofType "int"
        attribute("Timestep") ofType ("double")
        attribute("Scale") ofType ("double")
        attribute("YOffset") ofType "int"
        attribute("Bandwidth")
        attribute("PointData") ofType "boolean"


        "Point" multiple {
          attribute("x") ofType ("double")
          attribute("y") ofType ("double")
        }

      }

      "Histogram" multiple {
        attribute("Name")
        attribute("Type")
        attribute("FirstBinMean") ofType ("double")
        attribute("BinWidth") ofType ("double")
        attribute("valid") ofType ("boolean")
        
        // Histogram can contain string
        ofType("string")
        
      }

      "TextData" is {
        attribute("Name")
        attribute("valid") ofType ("boolean ")
        ofType("cdata")
        
      }
      
    }

  }

}