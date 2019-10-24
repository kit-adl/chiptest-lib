package owncloud

import java.net.URL
import java.net.HttpURLConnection
import java.util.Base64
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.HttpClient
import org.apache.http.auth.AuthScope
import org.apache.http.auth.Credentials
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.jackrabbit.webdav.DavConstants
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind
import org.apache.jackrabbit.webdav.header.Header
import org.apache.http.impl.client.BasicCredentialsProvider
import kit.edu.ipe.adl.chiptest.measurement.store.DAVDataStore
import java.io.File
import kit.edu.ipe.adl.chiptest.measurement.data.MultiXYGraph

object TryOwncloud extends App {

  var addressBase = "http://ipe-iperic-srv1.ipe.kit.edu/owncloud/remote.php/webdav"

  
  var ds = new DAVDataStore(new File("target/test-out/dav-ds"),"http://ipe-iperic-srv1.ipe.kit.edu/owncloud/remote.php/webdav")
  
  // Will ask for login if necessary
  ds.login()
  
  //-- Get Path to tristan measurement folder
  var tv2File = ds.getFile("/measurement/TristanV2")
  
  println("File: "+tv2File.path+" -> "+tv2File.getPathInStore)
  
  //-- List available uploaded files
  tv2File.listFiles.foreach {
    f => 
      println("Available File: "+f.getPathInStore)
  }
  

 
  
}