package kit.edu.ipe.adl.chiptest.measurement.session


import com.idyria.osi.ooxoo.model.ModelBuilder
import com.idyria.osi.ooxoo.model.producer
import com.idyria.osi.ooxoo.model.producers
import com.idyria.osi.ooxoo.model.out.markdown.MDProducer
import com.idyria.osi.ooxoo.model.out.scala.ScalaProducer

@producers(Array(
  new producer(value = classOf[ScalaProducer]),
  new producer(value = classOf[MDProducer])))
object MeasurementSessionModel extends ModelBuilder {
 
  "Session" is {
    
    elementsStack.head.makeTraitAndUseCustomImplementation
   
    // Date
    //-------------
    attribute("creationDate") ofType("datetime")
    attribute("lastUpdate") ofType("datetime")
    
    //importElement(getClass.getPackage.get)
  }
  
  
}