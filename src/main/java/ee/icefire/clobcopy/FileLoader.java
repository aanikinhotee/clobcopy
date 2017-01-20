package ee.icefire.clobcopy;


import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 10/2/12
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileLoader {

  public static final String ALLP_ID_KEY = "allpId";
  public static final String ALDL_KOOD_KEY = "aldlKood";
  public static final String TYYP_KEY = "tyyp";

  ClobUpdater clobUpdater;

  public ClobUpdater getClobUpdater() {
    return clobUpdater;
  }

  public void setClobUpdater(ClobUpdater clobUpdater) {
    this.clobUpdater = clobUpdater;
  }

  public static final ArrayList<String> TEMPLATE_TYPES = new ArrayList<String>(){{
    add("PDF");
    add("HTML");
  }};



  private Map<String, String> getParsedFileName(String filename) throws FileLoaderException {
    int point = filename.indexOf(".");
    String filenameWithoutExtention = filename.substring(0, point);

    Map<String, String> parsed = new HashMap<String, String>();

    int first_ = filenameWithoutExtention.indexOf("_");
    int last_ = filenameWithoutExtention.lastIndexOf("_");

    String firstToken = filenameWithoutExtention.substring(0, first_);
    String secondToken = filenameWithoutExtention.substring(first_+1, last_);
    String lastToken = filenameWithoutExtention.substring(last_+1);

    try{
      Long allpId = new Long(firstToken);
    } catch (java.lang.NumberFormatException e){
      throw  new FileLoaderException("First token should be ALLP_ID. \"" + firstToken + "\" is not a valid number.");
    }

    if(!TEMPLATE_TYPES.contains(lastToken)){
      throw  new FileLoaderException("Last token should be PDF or HTML. \"" + lastToken + "\" is not a PDF or HTML.");
    }

    parsed.put(ALLP_ID_KEY, firstToken);
    parsed.put(ALDL_KOOD_KEY, secondToken);
    parsed.put(TYYP_KEY, lastToken);

    return parsed;
  }

  private void saveFile(File file, String content) throws IOException, FileLoaderException {


    // if file doesnt exists, then create it
    if (!file.exists()) {
      if(!file.createNewFile()){
        throw new FileLoaderException("Unable to create new file with name " + file.getAbsolutePath());
      }
    }

    FileWriter fw = new FileWriter(file.getAbsoluteFile());
    BufferedWriter bw = new BufferedWriter(fw);
    bw.write(content);
    bw.close();

  }


  private String readFile(File file) throws IOException, FileLoaderException {

    byte[] bytes = Files.readAllBytes(file.toPath());

    if((bytes[0] == (byte) 0xEF) && (bytes[1] == (byte) 0xBB) && (bytes[2] == (byte) 0xBF)){
      throw new FileLoaderException("File contains BOM, please remove it first");
    }

    return new String(bytes);
  }
  private static Logger LOG = Logger.getLogger(FileLoader.class);

  private void checkTempDir(String localPath) throws FileLoaderException {
    File file = new File(localPath);
    if(file.exists()){
      if(file.isFile()){
        throw new FileLoaderException("localPath can't be a file, it should be a directory");
      }
    } else {
      if(file.mkdir()){
        LOG.info("local directory created");
      } else {
        throw new FileLoaderException("Unable to create localPath directory");
      }
    }
  }

  public static void main(String[] argc) throws Exception {
    Properties pr = SysGlobal.getProperties(argc);

    ClassPathResource classPathResource = new ClassPathResource("loader.xml");
    XmlBeanFactory beanFactory = new XmlBeanFactory(classPathResource);

    PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
    cfg.setProperties(pr);
    cfg.postProcessBeanFactory(beanFactory);

    FileLoader fileLoader = (FileLoader) beanFactory.getBean("fileLoader");

    System.out.println("Installing with following DB URL: "  + pr.getProperty(SysGlobal.DB_URL));


    if(pr.getProperty(SysGlobal.MODE).equals(SysGlobal.MODE_FILE)){

      String filename = pr.getProperty(SysGlobal.FILE_PATH);
      LOG.info("Processing file = " + filename);
      File file = new File(filename);
      Map<String, String> f = fileLoader.getParsedFileName(file.getName());
      String data = fileLoader.readFile(file);


      fileLoader.getClobUpdater().processClob(data, f.get(ALLP_ID_KEY), f.get(TYYP_KEY), f.get(ALDL_KOOD_KEY));
    } else if (pr.getProperty(SysGlobal.MODE).equals(SysGlobal.MODE_CVS)) {
      CVSClient cvsClient = new CVSClient();
      List<File> files = cvsClient.checkout(pr.getProperty(SysGlobal.LOCAL_PATH), pr.getProperty(SysGlobal.CVS_TAG));

      for(File file:files){
        LOG.info("Processing file = " + file.getName());
        Map<String, String> f = fileLoader.getParsedFileName(file.getName());
        String data = fileLoader.readFile(file);
        LOG.info("length = " + data.getBytes().length);

        fileLoader.getClobUpdater().processClob(data, f.get(ALLP_ID_KEY), f.get(TYYP_KEY), f.get(ALDL_KOOD_KEY));
      }
    } else if(pr.getProperty(SysGlobal.MODE).equals(SysGlobal.MODE_EXPORT)) {
      List<Template> clobs = fileLoader.getClobUpdater().exportAll();

      System.out.println(clobs.size());

      fileLoader.checkTempDir(pr.getProperty(SysGlobal.LOCAL_PATH));

      for(Template t : clobs){
        String newFilename;
        if((newFilename = t.getFilenameHtml()) != null){
          File file = new File(pr.getProperty(SysGlobal.LOCAL_PATH)+"/"+newFilename);
          fileLoader.saveFile(file, t.getHtmlPohi());
        }

        if((newFilename = t.getFilenamePdf()) != null){
          File file = new File(pr.getProperty(SysGlobal.LOCAL_PATH)+"/"+newFilename);
          fileLoader.saveFile(file, t.getPdfPohi());
        }

      }
    }
  }

}
