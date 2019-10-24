package kit.edu.ipe.adl.chiptest.webui

import org.odfi.wsb.fwapp.views.FWappView
import org.odfi.wsb.fwapp.framework.FWAppFrameworkView
import org.odfi.wsb.fwapp.module.semantic.SemanticView
import org.w3c.dom.html.HTMLElement
import com.idyria.osi.vui.html.HTMLNode
import org.odfi.wsb.fwapp.views.FWAppViewIntermediary
import org.odfi.wsb.fwapp.FWappIntermediary

trait ASICPage extends FWAppFrameworkView with SemanticView {

  
  def definePage(cl: => HTMLNode[HTMLElement,_]) = {
    definePart("page") {
      cl
    }
  }
  
  this.viewContent {
    html {
      head {
        placeLibraries
      }
      body {

        "ui container" :: div {

          // Header
          //--------------
          div {
            h1("ASIC Test Environment") {

            }
            
            "ui menu" :: div {
              
              // Views
              //--------------
              getApp match {
                case Some(site) =>
                  site.findChildOfType[FWAppViewIntermediary].foreach {
                    view => 
                      "ui item" :: a(view.fullURLPath)(text(view.parentIntermediary.asInstanceOf[FWappIntermediary].basePath)) 
                  }
                case None => 
              }
              
              // Stores
              //--------------
              
              
            }
          }

          // Page
          //--------
          "#page" :: div {
            placePart("page")
          }

          // Footer
          //-------------
        }

      }
    }
  }
}