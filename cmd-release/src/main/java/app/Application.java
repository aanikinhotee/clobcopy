package app;


import ee.icefire.clobcopy.CVSClient;
import ee.icefire.clobcopy.CheckoutMeta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class Application {

  static Set<String> preCode = new TreeSet<>();

  static {
    preCode.add("TAB");
    preCode.add("SEQ");
    preCode.add("TPS");
    preCode.add("MV");
  }

  static Set<String> code = new TreeSet<>();

  static {
    code.add("PKG");
    code.add("VW");
    code.add("PRC");
    code.add("FNC");
  }

  static Set<String> postCode = new TreeSet<>();

  static {
    postCode.add("TRG");
    postCode.add("SCRIPTS");
  }

  public static void main(String[] args) throws Exception {

    Properties pr = ExtractProperties.getProperties(args);

    CVSClient cvsClient = new CVSClient();
    String tag = pr.getProperty(ExtractProperties.CVS_TAG);
    CheckoutMeta checkoutMeta = cvsClient.checkout("tmpOut", tag);

    List<File> files = checkoutMeta.getFiles();

    String tmpDirAbsolutePath = checkoutMeta.getLocalDirectory().getAbsolutePath() + "/" + tag;
    Path tmpDirPath = Paths.get(tmpDirAbsolutePath);

    try (PrintStream out = new PrintStream(new FileOutputStream(tmpDirAbsolutePath + "/ttest.xx"))) {
      out.printf("REM Database MTA release script for %s\n" +
          "REM Built on %s.\n" +
          "\n" +
          "set serveroutput on format wrapped size unlimited\n" +
          "set sqlblanklines on\n" +
          "set timing on\n" +
          "set time on\n" +
          "\n" +
          "SPOOL install_MTA.log\n" +
          "SET DEFINE OFF\n" +
          "\n" +
          "PROMPT [---------------  DB  ----------------] \n", tag, LocalDateTime.now());




      // first level

      File tmpDirFile = tmpDirPath.toFile();
      if(tmpDirFile.isDirectory()) {
        out.println("rem tmp dir: " + tmpDirFile.getAbsolutePath());
        File[] schemas = tmpDirFile.listFiles();
        if(schemas != null) {
          for (File file1 : schemas) {
            processSchema(out, file1);
          }
        }
      }


      for (File file : files) {
        String executionCommand = file.getAbsolutePath().replaceFirst(tmpDirAbsolutePath, "@@DB");
        out.println("prompt Executing " + executionCommand);
        out.println(executionCommand);
      }

      out.println("\nCOMMIT\n" +
          "/\n" +
          "\n" +
          "SPOOL OFF");
    }

  }

  private static void processSchema(PrintStream out, File file1) {
    out.println("rem first level files: " + file1.getName());

    File[] objectTypes = file1.listFiles();
    if(objectTypes != null) {
      for (File file2 : objectTypes) {
        if(preCode.contains(file2.getName())) {
          processObjectType(out, file2);
        }
        if(code.contains(file2.getName())) {
          processObjectType(out, file2);
        }
        if(postCode.contains(file2.getName())) {
          processObjectType(out, file2);
        }
      }
    }
  }

  private static void processObjectType(PrintStream out, File file2) {
    out.println("rem second level files: " + file2.getName());
    File[] scripts = file2.listFiles();
    if (scripts != null) {
      for (File file3 : scripts) {
        out.println("rem third level files: " + file3.getName());
      }
    }
  }
}
