package ee.icefire.clobcopy;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.netbeans.lib.cvsclient.commandLine.CVSCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 10/2/12
 * Time: 6:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class CVSClient {

  private static Logger LOG = Logger.getLogger(CVSClient.class);

  public List<File> getFilesList(List<File> returnFiles, File file){
    File[] checkoutDirList = file.listFiles();

    if(checkoutDirList!= null){
      for (File aFilesList : checkoutDirList) {
        if(aFilesList.isFile()){
          returnFiles.add(aFilesList);
          System.out.println(aFilesList.getAbsolutePath());
        } else if(aFilesList.isDirectory()){
          getFilesList(returnFiles, aFilesList);
        }
      }
    }

    return returnFiles;
  }

  public List<File> checkout(String localPath, String tag) throws Exception {

    File file = new File(localPath);
    if(file.exists()){
      if(file.isFile()){
        throw new CVSClientException("localPath can't be a file, it should be a directory");
      }
    } else {
      if(file.mkdir()){
        LOG.info("local directory created");
      } else {
        throw new CVSClientException("Unable to create localPath directory");
      }
    }

    File checkoutDir = new File(localPath+"/"+tag);
    if(checkoutDir.exists()){
      FileUtils.forceDelete(checkoutDir);
      LOG.info("remove local directory");
    }

    File[] files = null;
    System.out.println("Provide CVS password...");
    CVSCommand.processCommand(new String[]{"login"}, files, localPath, System.out, System.out);

    String[] argc = {"export", "-r" + tag, "-P", "-d" + tag , "DB"};
    List<File> returnFiles = new ArrayList<File>();


    if(CVSCommand.processCommand(argc, files, localPath, System.out, System.out)){
      returnFiles = getFilesList(returnFiles, checkoutDir);
    }
    return returnFiles;
  }
}
