package app;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

/**
 * $Source: /export/cvsroot/icefiresrc/sepa/sepatransfer/src/fi/hansa/bank/sepa/main/SysGlobal.java,v $
 * $Author: aniki $
 * $Date: 2011/02/16 13:55:18 $
 * $Revision: 1.11 $
 */


public class ExtractProperties {
  private static Properties cfg = new Properties();
  private static Logger LOG = Logger.getLogger(ExtractProperties.class);

  private static final String CONFIG_FILE = "cfgFile";

  public static final String LOCAL_PATH = "localPath";
  public static final String CVS_TAG = "cvsTag";
  public static final String MODE = "mode";
  public static final String MODE_CVS = "cvs";
  public static final String MODE_FILE = "file";
  public static final String MODE_EXPORT = "export";

  public static final String FILE_PATH = "filePath";

  public static final String DB_URL = "dburl";
  public static final String DB_USER = "dbuser";
  public static final String DB_PASS = "dbpass";

//--------------------------------------------------------------//

  public static Logger getLOG() {
    return LOG;
  }

//--------------------------------------------------------------//

  public static Properties getProperties(String[] argv) {
    // Load properties from commandline
    getFromCLine(argv);

    // Load properties from config file
    getFromFile();

    // check properties
    if(!checkProperties()){
      programExit(33);
    }


    return getCfg();
  }

//--------------------------------------------------------------//

  private static void getFromFile() {
    if (isPropertySet(CONFIG_FILE, cfg)) {
      getFromFile(cfg.getProperty(CONFIG_FILE));
    }
  }

//--------------------------------------------------------------//

  private static void getFromFile(String cfgFile) {
    Properties pr = new Properties();
    LOG.info("Config file is : " + cfgFile);
    // IO error is fatal. If it fails then terminate program
    try {
      // Open inputStream from properties file
      FileInputStream fis = new FileInputStream(cfgFile);
      // load default properties from file
      pr.load(fis);
      // Close inputStream from properties file
      fis.close();
    } catch (IOException e) {
      // print error message
      LOG.warn("Cannot open config file: " + cfgFile);
      LOG.warn(e.getMessage());
    }


    Enumeration eList = pr.keys();

    // loop over properties file values first
    while (eList.hasMoreElements()) {
      // get the key and value.
      String cKey = (String) eList.nextElement();
      String cValue = (String) pr.get(cKey);
      decodeProperty(cKey + "=" + cValue);
    }

  }

//--------------------------------------------------------------//

  private static void getFromCLine(String[] s) {
    for (String value : s) {
      decodeProperty(value);
    }
  }

//--------------------------------------------------------------//

  private static Properties getCfg() {
    return cfg;
  }

//--------------------------------------------------------------//

  public static Date getDate() {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EET"));
    return cal.getTime();
  }

//--------------------------------------------------------------//

  public static Timestamp getTimestamp() {
    return new Timestamp(getDate().getTime());
  }

  //--------------------------------------------------------------//
  public static boolean isPropertySet(String s, Properties pr) {
    try {
      if (!pr.getProperty(s).equals("")) return true;
    } catch (Exception e) {
      return false;
    }

    return true;
  }

//--------------------------------------------------------------//

  public static void setProperty(String s) {
    decodeProperty(s);
  }

//--------------------------------------------------------------//

  private static void decodeProperty(String s) {
    // check for allowed arguments
    if (_decodeArgument(cfg, s, CONFIG_FILE));
    else if (_decodeArgument(cfg, s, LOCAL_PATH));
    else if (_decodeArgument(cfg, s, CVS_TAG));
    else if (_decodeArgument(cfg, s, MODE));
    else if (_decodeArgument(cfg, s, FILE_PATH));
    else if (_decodeArgument(cfg, s, DB_URL));
    else if (_decodeArgument(cfg, s, DB_USER));
    else if (_decodeArgument(cfg, s, DB_PASS));
    else {
      LOG.error("Unknown configuration parameter: " + s);
      programExit(33);
    }
  }

//--------------------------------------------------------------//

  private static boolean checkProperties() {
    if (!isPropertySet(CVS_TAG, cfg)){
      LOG.error(CVS_TAG + " parameter is missing");
      return false;
    }

    return true;
  }

//--------------------------------------------------------------//

  private static boolean _decodeArgument(Properties p, String cmd, String arg) {
    // Exit when we cant handle decoding of command line
    try {
      // if this argument starts with correct String
      if (cmd.startsWith(arg)) {
        // find the length
        int len = arg.length();
        // find the next char to make sure is it correct separator
        String separator = cmd.substring(len, len + 1);
        // if it is correct then continue
        if (separator.equals(":") || separator.equals("="))
          len++;
        else
          // if not the it could be longer argument with the same
          // prefix or use has entered incorrect argument
          return false;
        // Add parameter into Properties table
        if (p.getProperty(arg) == null) {
          p.put(arg, cmd.substring(len));
          if (!arg.equals(DB_PASS)){
            LOG.info("Parameter: ADD: [" + arg + "=" + p.getProperty(arg) + "]");
          }
          else{
            LOG.info("Parameter: ADD: [" + arg + "]");
          }
        } else {
          if (!arg.equals(DB_PASS)){
            LOG.info("Parameter: EXISTS: [" + arg + "=" + p.getProperty(arg) + "]");
          }
          else{
            LOG.info("Parameter: EXISTS: [" + arg + "]");
          }
        }

        // argument is used
        return true;
      } else {
        return false;
      }
      // Catch error, print it
      // and quit
    } catch (Exception e) {
      LOG.error("Logical error while decoding configuration parameters\n"
        + "cmd: " + cmd + "  arg: " + arg);
      LOG.error(e.getMessage());
      programExit(32);
    }

    return true;
  }

//--------------------------------------------------------------//

  public static void programExit(int exitCode) {
    LOG.error("Exit status: " + exitCode);
    System.exit(exitCode);
  }

//--------------------------------------------------------------//


  public static long writeTimeToSTDOUT(String message, long previousTime){
    long currentTime = new Date().getTime();
    System.out.println(prepareProcessTimeMessage(message, previousTime, currentTime));
    return currentTime;
  }

  public static String getProcessingTimeMessage(String message, long previousTime){
    long currentTime = new Date().getTime();
    return prepareProcessTimeMessage(message, previousTime, currentTime);
  }

  private static String prepareProcessTimeMessage(String message, long previousTime, long currentTime){
    double miliseconds = currentTime - previousTime;
    double seconds = miliseconds / 1000;
    return message + miliseconds + " miliseconds, " + seconds + " seconds";
  }
//--------------------------------------------------------------//
}
