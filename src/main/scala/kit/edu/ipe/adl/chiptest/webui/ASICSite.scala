package kit.edu.ipe.adl.chiptest.webui

import org.odfi.wsb.fwapp.Site
import org.odfi.wsb.fwapp.assets.AssetsResolver
import org.odfi.wsb.fwapp.assets.ResourcesAssetSource

abstract class ASICSite extends Site("/asic") {
  
  this.onStart {
    this.parentResource match {
      case Some(parent) => 
      case None => 
        this.listen(8585)
    }
  }
  
   
  // Default views
  //-------------------
  
  "/" is {
    view(classOf[ASICSiteWelcome])
  }
  
  val assetsManager = ("/assets" uses new AssetsResolver).addAssetsSource("assets", new ResourcesAssetSource)
    
  
  // Stores Interface
  //------------------
  
}