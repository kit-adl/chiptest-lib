package kit.edu.ipe.adl.chiptest.measurement.prefs

import com.idyria.osi.ooxoo.model.ModelBuilder
import com.idyria.osi.ooxoo.model.producer
import com.idyria.osi.ooxoo.model.producers
import com.idyria.osi.ooxoo.model.out.markdown.MDProducer
import com.idyria.osi.ooxoo.model.out.scala.ScalaProducer

@producers(Array(
  new producer(value = classOf[ScalaProducer]),
  new producer(value = classOf[MDProducer])))
object PreferencesModel extends ModelBuilder {
 
  "preferences" is {
    
    elementsStack.head.makeTraitAndUseCustomImplementation
    
    "root" is {
      attribute("type") default "user"
      
      val map = "map" is {
        "entry" multiple {
           attribute("key")
           attribute("value")
        }
      }
      "node" multiple {
        importElement(map)
      }
    }
  }
  
  
}