package kit.edu.ipe.adl.chiptest.measurement.store

import java.io.File

import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
import org.odfi.indesign.core.harvest.HarvestedResource
import org.odfi.indesign.core.harvest.fs.HarvestedFile
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind
import org.apache.jackrabbit.webdav.DavConstants
import org.apache.jackrabbit.webdav.MultiStatusResponse
import org.apache.http.client.methods.HttpGet
import java.io.FileOutputStream
import com.idyria.osi.tea.io.TeaIOUtils
import javax.swing.JOptionPane
import javax.swing.JDialog
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.GridLayout
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import javax.swing.JTextField
import javax.swing.JPasswordField
import org.odfi.indesign.core.module.swing.SwingUtilsTrait
import javax.swing.JButton
import javax.swing.JFrame
import org.jfree.ui.RefineryUtilities
import java.util.prefs.Preferences
import java.net.URL

class DataStore(filePath: String) {

}

class DAVDataStore(val localFolder: File, val remotePath: String) extends DAVFile("/") with SwingUtilsTrait {

  localFolder.mkdirs()

  val hostPrefs = Preferences.userRoot().node(s"adl/owncloud/" + new URL(remotePath).getHost)

  // Low Level HTTP Interface
  //-------------------------
  var credentials: Option[UsernamePasswordCredentials] = None
  var clientBuilder: Option[HttpClientBuilder] = None

  //-- Credentials 
  
  /**
   * Login means look for credentials and ask for some using a little simple GUI window
   */
  def login(force: Boolean = false) = credentials match {

    case None =>

      //-- Get Back login from pref
      var pLogin = hostPrefs.get("login", "")
      var pPassword = hostPrefs.get("password", "")

      ((pLogin + pPassword).trim()) match {

        //-- Defined and not force -> ignore; otherwise show UI
        case lp if (lp.length() > 0 && !force) =>

          setCredentials(pLogin, pPassword)
          
        case other =>

          //-- Ask for login
          var d = new JFrame("Login to Ownclould")
          d.setSize(400, 250)
          //d.setModal(true)

          // Message
          d.getContentPane.add(new JLabel("Login to Owncloud"), BorderLayout.NORTH)

          // Login and password
          var loginpassword = new JPanel(new GridBagLayout)
          d.getContentPane.add(loginpassword, BorderLayout.CENTER)
          var constraint = new GridBagConstraints
          constraint.fill = GridBagConstraints.BOTH

          constraint.gridx = 0
          constraint.gridy = 0
          constraint.weightx = 0
          loginpassword.add(new JLabel("Login:"), constraint)

          var loginText = new JTextField(pLogin)
          constraint.gridx = 1
          constraint.gridy = 0
          constraint.weightx = 1
          loginpassword.add(loginText, constraint)

          var passwordText = new JPasswordField(pPassword)
          constraint.gridx = 1
          constraint.gridy = 1
          constraint.weightx = 1
          loginpassword.add(passwordText, constraint)

          constraint.gridx = 0
          constraint.gridy = 1
          constraint.weightx = 0
          loginpassword.add(new JLabel("Password:"), constraint)

          // Ok Button
          var okButton = new JButton("Ok")
          onSwingClick(okButton) {

            hostPrefs.put("login", loginText.getText)
            hostPrefs.put("password", passwordText.getPassword.mkString)

            setCredentials(loginText.getText, passwordText.getPassword.mkString)

            //-- Quit and unlock frame
            d.dispose()
            d.synchronized {
              d.notify()
            }
          }
          d.getContentPane.add(okButton, BorderLayout.SOUTH)

          d.setAlwaysOnTop(true)
          d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
          d.setVisible(true)
          RefineryUtilities.centerFrameOnScreen(d)

          // Block Until Frame is finished
          d.synchronized {
            d.wait()
          }
      }

    case other =>

  }

  def getClientBuilder = clientBuilder match {
    case Some(b) =>
      b

    case None =>
      clientBuilder = Some(HttpClientBuilder.create())
      credentials match {
        case Some(c) =>
          var provider = new BasicCredentialsProvider();
          provider.setCredentials(AuthScope.ANY, c)
          clientBuilder.get.setDefaultCredentialsProvider(provider)
          clientBuilder.get
        case None =>
          clientBuilder.get
      }

  }

  def setCredentials(username: String, password: String) = {
    credentials = Some(new UsernamePasswordCredentials(username, password))
    clientBuilder = None
  }

  def getClient = getClientBuilder.build()

  // Local Files Stuf
  //-------------------

  def getLocalFile(f: DAVFile) = {
    new File(localFolder, f.getPathInStore.replace("/", File.separator))
  }

}

class DAVFile(val path: String) extends HarvestedResource {

  def getId = "DAVFile:" + path

  // get store
  def getStore = this.findTopMostResource[DAVDataStore].get

  def getPathInStore = this.mapUpResources[DAVFile, String] { r => r.path }.reverse.mkString("/") + "/" + path

  def getRemotePath = (this.getStore.remotePath.stripSuffix("/") + "/" + getPathInStore)

  def getFileNameNoExtension = path.takeWhile(_ != '.')

  // Remove
  //---------------------
  def listFiles: List[RemoteDAVFile] = {

    // Create Prop Find
    //---------------
    var client = getStore.getClient
    var pf = new HttpPropfind(s"${getStore.remotePath}/${getPathInStore}", DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
    //pf.addHeader(new Header(""))

    // Execute
    //------------
    this.cleanDerivedResourcesOfType[RemoteDAVFile]

    var response = client.execute(pf)

    pf.succeeded(response) match {
      case true =>

        var multistatus = pf.getResponseBodyAsMultiStatus(response)

        var res = multistatus.getResponses.map {
          multistatusResponse =>

            //-- Use only last path component for file path
            var remoteFullPath = multistatusResponse.getHref

            var fileLastPath = remoteFullPath.split("/").filter(_.length() > 0).last

            (fileLastPath == path) match {
              case true =>
                None
              case false =>

                var file = new RemoteDAVFile(fileLastPath, multistatusResponse)
                addDerivedResource(file)
                Some(file)
            }

        }

        res.filter(_.isDefined).map(_.get).toList
      case false =>
        

        getStore.hostPrefs.put("login", "")
        getStore.hostPrefs.put("password", "")
        
        sys.error("Cannot List files: " + response.getStatusLine)

        List()
    }

    /*response.getAllHeaders.foreach {
      h =>
        println("Header: " + h.getName + " -> " + h.getValue)

    }*/

  }

  // Local Files utils
  //--------------

  def getStoreLocalFile = getStore.getLocalFile(this)

  // Get files
  //-----------
  def getFile(path: String): DAVFile = {

    var latestFile = this
    var allSubPaths = path.split("/").filter(_.length() > 0)
    allSubPaths.size match {
      case 0 => this
      case 1 =>

        this.getDerivedResources[DAVFile].find(_.path == allSubPaths(0)) match {
          case Some(found) => found
          case other =>
            var newFile = new DAVFile(allSubPaths(0))
            newFile.deriveFrom(this)
            newFile
        }

      case other =>

        var lastFile = allSubPaths.map {
          localPath =>

            var path = latestFile.getFile(localPath)
            //path.deriveFrom(latestFile)
            latestFile = path
            path
        }.last

        lastFile
    }

  }

  // Remote Utils
  //----------------
  def syncToLocal = {

    // Lookup Local
    //-----------
    var localFile = getStoreLocalFile

    // If Exists, do nothing
    //---------
    localFile.exists() match {
      case true =>
        println(s"File $path exists, not downloading....")
      case false =>
        println(s"File $path does not exist, downloading...." + getRemotePath)

        // Make sure folder exists
        localFile.getParentFile.mkdirs()

        // Create Method
        var client = getStore.getClient
        var getMethod = new HttpGet(getRemotePath)

        // Execute
        var response = client.execute(getMethod)

        var length = response.getEntity.getContentLength
        // var out = new FileOutputStream(localFile)
        try {

          TeaIOUtils.writeToFile(localFile, response.getEntity.getContent)

        } finally {
          // out.close()
          getMethod.releaseConnection()
        }

    }
  }

}

class RemoteDAVFile(path: String, val props: MultiStatusResponse) extends DAVFile(path) {

}