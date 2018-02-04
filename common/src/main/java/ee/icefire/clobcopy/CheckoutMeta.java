package ee.icefire.clobcopy;

import java.io.File;
import java.util.List;

public class CheckoutMeta {
  List<File> files;

  File localDirectory;

  public CheckoutMeta(List<File> files, File localDirectory) {
    this.files = files;
    this.localDirectory = localDirectory;
  }

  public List<File> getFiles() {
    return files;
  }

  public void setFiles(List<File> files) {
    this.files = files;
  }

  public File getLocalDirectory() {
    return localDirectory;
  }

  public void setLocalDirectory(File localDirectory) {
    this.localDirectory = localDirectory;
  }
}
