package kit.edu.ipe.adl.chiptest.shiftregister.model

import com.idyria.osi.tea.listeners.ListeningSupport
import com.idyria.osi.ooxoo.core.buffers.structural.xelement

/**
  * @author mf1049
  */
@xelement(name = "ShiftRegister")
class SRCommon extends ListeningSupport with ShiftRegisterTrait {

  def setRegisterValue(name:String,v:Long) : Unit = {
    this.registers.find(_.name.toString == name) match {
      case Some(r) =>
        r.setMemory(v)
      case None =>
        sys.error("Cannot set register: "+name+", not defined, check name in XML and Code")
    }
  }

  def setRegisterValue(name:String,b:Boolean) : Unit = {
    this.registers.find(_.name.toString == name) match {
      case Some(r) if(b) =>
        r.setMemory(1)
      case Some(r) if(!b) =>
        r.setMemory(0)
      case None =>
        sys.error("Cannot set register: "+name+", not defined, check name in XML and Code")
    }
  }

  def program = {

  }

}