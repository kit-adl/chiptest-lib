package kit.edu.ipe.adl.chiptest.shiftregister.ui.www

import org.odfi.indesign.core.module.ui.www.IndesignUIView
import kit.edu.ipe.adl.chiptest.shiftregister.model.H35ShiftRegister
import java.util.prefs.Preferences
import org.odfi.wsb.fwapp.framework.FWAppPreferencesBinding
import org.odfi.wsb.fwapp.module.semantic.SemanticView

class SRTable(val shiftRegister: H35ShiftRegister, val preferences: Preferences) extends FWAppPreferencesBinding with SemanticView {

  var prefNode = preferences.node(shiftRegister.name.toString())

  this.viewContent {

    div {
      "ui table" :: table {
        thead {

          tr {

            th("SR: " + shiftRegister.name) {
              +@("colspan", 2.toString())
            }

          }
          tr {
            th("Field") {

            }

            th("Value") {

            }
          }

        }
        tbody {

          shiftRegister.registers.foreach {
            reg =>

              tr {
                td(reg.name) {

                }

                td("") {

                  // Register Case:
                  // If fields are present, use single enable boxes
                  reg.fields.size match {

                    //--- No Fields, just a value box
                    case 0 =>

                      "inline field" :: div {
                        this.inputToPreference(prefNode, s"${reg.name}.value", 0L) {
                          v: Long =>
                            reg.setMemory(v)
                          //prefNode.putLong(s"${reg.name}.value", v)
                        }

                        reg.findAttributeLong("sr.size") match {
                          case Some(size) =>
                            span {
                              textContent(s"(Max: ${Math.pow(2, reg.findAttributeLong("sr.size").get.toDouble).toInt - 1})")
                            }
                          case None =>
                        }

                      }

                    //-- Fields, make gui with selection boxes and so on
                    case _ =>

                      // Update from prefs
                      var regVal = prefNode.getLong(s"${reg.name}.value", 0L)
                      reg.setMemory(regVal)

                      // Find Bits which are within a bit select group
                      // Get others as well
                      var groupFields = reg.fields.filter { f => f.findAttribute("sw.group").isDefined }
                      var otherFields = reg.fields.filter { f => f.findAttribute("sw.group").isEmpty }

                      form {

                        // Value Field to set by hand
                        //-------------
                        "inline field" :: div {
                          label("Register Value: ")()
                          "@reload" :: this.inputToPreference(prefNode, s"${reg.name}.value", 0L) {
                            v: Long =>
                              reg.setMemory(v)
                          }
                          text("(Update this value to update all fields)")
                        }

                        // Grouped selections -> buttons
                        //--------

                        groupFields.groupBy(_.findAttribute("sw.group").get).foreach {
                          case (groupName, fields) =>
                            "field" :: div {
                              label(groupName)()

                              //-- Check fields are all width 1
                              fields.forall(_.width.data == 1) match {
                                case false =>

                                  "ui error message" :: s"Field Group ${groupName} contains fields with width > 1, this feature is only available to select between bits, so width MUST be 1"

                                case true =>

                                  //-- Buttons Group
                                  "ui buttons" :: div {

                                    fields.foreach {
                                      field =>

                                        //-- Button for field
                                        "ui button" :: div {
                                          textContent(field.name.toString)

                                          //-- Active if 1 
                                          field.memoryValue match {
                                            case 1 =>
                                              ++@("class" -> "active")
                                            case _ =>
                                          }

                                          //-- On Click
                                          onClickReload {

                                            //-- Toggle value, so click on 1 will reset to 0
                                            val newValue = field.memoryValue match {
                                              case 0 => 1
                                              case 1 => 0
                                            }
                                            fields.foreach(_.setMemory(0))
                                            field.setMemory(newValue)

                                            prefNode.putLong(s"${reg.name}.value", reg.getMemory.toLong)

                                            //++@("class"->"active")
                                          }

                                        }
                                    }
                                    // EOF Fields buttons

                                  }
                                  // EOF Buttons

                                  //-- Description?
                                  //println(s"Looking in reg for ${groupName}.description")
                                  reg.findAttribute(s"${groupName}.description") match {
                                    case Some(d) =>
                                      text(d)
                                      println("Found: " + d)
                                    case None =>
                                      println("Not found")
                                  }
                              }

                            }
                          case other =>
                        }
                        // EOF Groups

                        // Other Fields
                        //--------------
                        otherFields.foreach {

                          //-- Width 1, make a selection box
                          case field if (field.width.data == 1) =>
                            "inline field" :: div {
                              label(field.name) {

                              }
                              "@reload" :: input {
                                attributeIf(field.memoryValue == 1)("checked", "true")
                                bindValue {
                                  b: Boolean =>
                                    b match {
                                      case true  => field.setMemory(1)
                                      case false => field.setMemory(0)
                                    }
                                    prefNode.putLong(s"${reg.name}.value", reg.getMemory.toLong)
                                }

                              }
                            }
                          //-- Normal Field Value
                          case field =>
                            "inline field" :: div {

                              label(field.name) {

                              }
                              input {
                                +@("value" -> field.memoryValue)
                                bindValue {
                                  v: Long =>
                                    field.setMemory(v)
                                    prefNode.putLong(s"${reg.name}.value", reg.getMemory.toLong)
                                }

                              }
                            }
                        }

                      }
                    // EOF FORM

                  }

                }

              }
          }

        }
        // EOF tbody

        tfoot {
          tr {
            td("") {
              +@("colspan" -> 2.toString)
              "ui hidden error message" :: div {

              }
              "ui primary button" :: button("Program") {

                onClick {
                  this.@->("program")
                }
              }
            }
          }
        }

      }
    }

  }

  def onProgram(cl: => Unit) = {
    this.on("program") {
      cl
    }
  }

}