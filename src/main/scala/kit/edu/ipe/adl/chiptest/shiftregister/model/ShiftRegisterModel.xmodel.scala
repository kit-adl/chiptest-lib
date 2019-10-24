package kit.edu.ipe.adl.chiptest.shiftregister.model

import com.idyria.osi.ooxoo.model.producers
import com.idyria.osi.ooxoo.model.producer
import com.idyria.osi.ooxoo.model.out.markdown.MDProducer
import com.idyria.osi.ooxoo.model.out.scala.ScalaProducer
import com.idyria.osi.ooxoo.model.ModelBuilder
import kit.ipe.adl.rfg3.model.Register
import org.odfi.instruments.compress.XMLCompressOutput
import kit.ipe.adl.rfg3.model.AttributesContainer


/**
 * @author zm4632
 */
@producers(Array(
  new producer(value = classOf[ScalaProducer]),
  new producer(value = classOf[MDProducer])))
object ShiftRegisterModel extends ModelBuilder {
  
  
  // Shift Register Bus
  //------------------
  "ShiftRegisterBusTrait" is {
    isTrait
  
    
    var sr = importElement("ShiftRegisterSimple")
    sr.setMultiple
    sr.name = "ShiftRegister"
  }
  
  // Single Shift Register
  //-----------
  
  "ShiftRegisterTrait" is {
    isTrait
    withTrait(classOf[XMLCompressOutput])
    withTrait(classOf[AttributesContainer])
    

    
    // Register Definition
    "Register" multiple {
      classType(classOf[Register].getCanonicalName)
      attribute("hide") ofType("boolean") default "false"
    }
    
    //importElement(classOf[Register].getCanonicalName).setMultiple
   
    /*"Register" multiple {
      //isTrait
      
      attribute("name")
      attribute("spare") ofType("boolean") default("false") is("Spare is used to define an unused register")
      
      attribute("size") ofType("int") default("1")
      
      attribute("value") ofType("float") default("0")
      
      
      "BitSelect" is {
        withDescription("A list of bit indexes")
        ofType("string")
      }
    }*/
    
    // Spare are used as unused bits
    //"Spare" multiple
    
    
  } 
  
  "ShiftRegisterSimple" is {
    withTrait("ShiftRegisterTrait")
  }
  
  
  
}