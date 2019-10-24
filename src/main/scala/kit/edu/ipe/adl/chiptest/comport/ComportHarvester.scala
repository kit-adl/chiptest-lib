package kit.edu.ipe.adl.chiptest.comport

import dk.thibaut.serial.SerialPort
import scala.collection.JavaConversions._
import dk.thibaut.serial.enums.StopBits
import dk.thibaut.serial.enums.BaudRate
import dk.thibaut.serial.enums.DataBits
import dk.thibaut.serial.enums.Parity
import java.io.BufferedReader
import java.io.InputStreamReader
import dk.thibaut.serial.SerialException
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import java.io.PrintWriter
import org.odfi.indesign.core.harvest.Harvester
import org.odfi.indesign.core.harvest.HarvestedResource
import java.io.IOException

object ComportHarvester extends Harvester {

  
  def getPortForDevice(deviceInfo:String,portName:String) = {
    getResourceById[COMPort](portName) match {
      case Some(p) =>
        p
      case None =>
        sys.error(s"Requested PORT $portName for $deviceInfo is not available")
    }
  } 
  
  override def doHarvest = {

    try {
      println(s"Listing ports")
      SerialPort.getAvailablePortsNames().foreach {
        port =>
          println(s"Found: " + port)
          gather(new COMPort(port))
      }

      println(s"Done")
    } catch {
      case e: NullPointerException =>
    }

  }

}


/**
 * Default timeout of 200ms
 * This time is used by all the commands, tweak it to improve performance!
 */
class COMPort(var comPortName: String) extends HarvestedResource {

  var timeout = 200
  
  def getId = comPortName

  this.onGathered {
    case h if (h == ComportHarvester) =>

    //comPort = SerialPort.open(comPortName)

  }

  var opened = false
  def open = opened match {
    case false =>
      comPort = SerialPort.open(comPortName)
      comPort.setTimeout(timeout)
      opened = true
    case true =>
  }

  var comPort: SerialPort = null
  var configured = false

  var ignoreLineAfterOpen = false

  def configure(bauds: BaudRate) = {
    (configured, comPort) match {
      case (true, _) => true
      case (false, p) if (p == null) =>

        this.synchronized {

          open
          comPort.setConfig(bauds, Parity.NONE, StopBits.ONE, DataBits.D8);
          comPort.getOutputStream.flush()
          if (ignoreLineAfterOpen) {
            clearReceivedLineInBuffer
          }

          configured = true
        }
        true
      case _ =>
        false
    }

  }

  // Send Receive
  //----------------

  var lineIgnorePrefix: Option[String] = None
  /**
   * If LineIgnore is set, ignore all lines starting with prefix
   * This is used so that info/debugging lines and can be prefixed with "#" for example, but the software will still react on relevant lines
   */
  def setLineIgnorePrefix(prefix: String) = {
    this.lineIgnorePrefix = Some(prefix)
  }

  def clearReceivedLineInBuffer: String = {

    try {
      /*this.comPort.getInputStream.available() match {
        case 0 => ""
        case _ => this.receiveLine()
      }*/
      this.receiveLine()
    } catch {
      case e: Throwable =>
        ""
    }
  }

  def isExceptionRemoved(e: SerialException) = {
    e.getLocalizedMessage match {
      case m if (m.contains("The device does not recognize the command")) =>
        try {
          this.comPort.close()
        } catch {
          case e: Throwable =>
        }
        true
      case m => false
    }

  }

  def sendLine(command: String): Unit = {
    this.synchronized {
      try {
        //open
        //comPort.getOutputStream.flush()
        //clearReceivedLineInBuffer
        var writer = new PrintWriter(new OutputStreamWriter(this.comPort.getOutputStream))
        writer.println(s"${command}")
        writer.flush()
        //this.comPort.getOutputStream.flush
      } catch {

        // Board has been removed!
        case e: SerialException if (isExceptionRemoved(e)) =>
          e.printStackTrace()
        case e: Throwable => throw e
      }
    }
  }

  def sendLineReceiveLine(command: String, localPrefix: String = ""): String = {
    this.synchronized {
      try {
        sendLine(command)
        this.receiveLine(localPrefix).trim
      } catch {
        // Board has been removed!
        case e: SerialException if (isExceptionRemoved(e)) =>
          ""

        case e: Throwable => throw e
      }
    }
  }

  def send1ZeroByte = {
    this.synchronized {
      try {
        this.comPort.getOutputStream.write(0)
        this.comPort.getOutputStream.flush
      } catch {
        // Board has been removed!
        case e: SerialException if (isExceptionRemoved(e)) =>
          ""

        case e: Throwable => throw e
      }
    }
  }

  def receiveLine(localPrefix: String = "") = {
    this.synchronized {
      try {
        open
        var ignorePrefix = localPrefix.trim match {
          case "" => this.lineIgnorePrefix
          case _  => Some(localPrefix)
        }

        // println("Waiting for line: ")
        var br = new BufferedReader(new InputStreamReader(comPort.getInputStream, "US-ASCII"))
        var eofLoop = false
        var line = ""
        while (!eofLoop) {
          try {
          line = br.readLine()

          //  println("Got line: "+line)
          ignorePrefix match {

            // Ignore line if starting with prefix
            case Some(prefix) if (line.startsWith(prefix)) =>
              br.reset
            case _ =>
              eofLoop = true
          }
          } catch {
            case e : IOException if (e.getLocalizedMessage.contains("zero"))=>
              
            case e : Throwable => throw e
          }
        }

        line

      } catch {
        // Board has been removed!
        case e: SerialException if (isExceptionRemoved(e)) =>
          ""

        case e: Throwable => throw e
      }
    }
  }

}