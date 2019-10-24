package kit.edu.ipe.adl.chiptest.rf.fifo

import kit.ipe.adl.rfg3.model.Register
import kit.ipe.adl.rfg3.language.RFLanguage


class Fifov3(val dataReg:Register) extends RFLanguage {
  
  
  // There must be a field with a FIFO marker
  var depth = dataReg.findAttribute("hw.depth") match {
    
    case Some(attr) => attr.toInt
    case None => 
      
      
      throw new IllegalArgumentException(s"Cannot create Fifo object with register ${dataReg.name}, the FIFO attribute must be provided")
    
  }
  
  /**
   * Read @size entries from the Fifo accessible at provided Register
   */
  def read(size:Int) : Array[Long] = {
    
    dataReg.value(size)
    dataReg.valueBuffer.buffer
    
  }
  
  /**
   * Reads the full content of the Fifo, based on extracted depth from XML
   */
  def readFull = this.read(depth)
  
  def writeFull(vals:Array[Long]) = {
    
    dataReg.value(vals)
  }
  
}

class Fifov2(val dataReg:Register) extends RFLanguage {
  
  
  // There must be a field with a FIFO marker
  var depth = dataReg.field("value").findAttribute("hardware.osys::rfg::fifo") match {
    
    case Some(attr) => attr.toInt
    case None => 
      
      
      throw new IllegalArgumentException(s"Cannot create Fifo object with register ${dataReg.name}, the FIFO attribute must be provided")
    
  }
  
  /**
   * Read @size entries from the Fifo accessible at provided Register
   */
  def read(size:Int) : Array[Long] = {
    
    dataReg.value(size)
    dataReg.valueBuffer.buffer
    
  }
  
  /**
   * Reads the full content of the Fifo, based on extracted depth from XML
   */
  def readFull = this.read(depth)
  
  def writeFull(vals:Array[Long]) = {
    
    dataReg.value(vals)
  }
  
}

class Fifo(val dataReg:Register) extends RFLanguage {
  
  
  // There must be a field with a FIFO marker
  var depth = dataReg.findAttribute("::odfi::rfg::stdlib::fifo.depth") match {
    
    case Some(attr) => attr.toInt
    case None => 
      
      
      throw new IllegalArgumentException(s"Cannot create Fifo object with register ${dataReg.name}, the FIFO attribute must be provided")
    
  }
  
  /**
   * Read @size entries from the Fifo accessible at provided Register
   */
  def read(size:Int) : Array[Long] = {
    
    dataReg.value(size)
    dataReg.valueBuffer.buffer
    
  }
  
  /**
   * Reads the full content of the Fifo, based on extracted depth from XML
   */
  def readFull = this.read(depth)
  
  def writeFull(vals:Array[Long]) = {
    
    dataReg.value(vals)
  }
  
}