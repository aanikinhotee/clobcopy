package ee.icefire.clobcopy;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Package: ee.icefire.clobcopy
 * User: anton
 * Date: 2/11/17
 * Time: 9:06 PM
 */
public class FileLoaderTest {


  @Test
  public void testFileLoad() throws URISyntaxException, IOException, FileLoaderException {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("testo").getFile());
    assertTrue(file.exists());

    FileLoader fileLoader = new FileLoader();
    String string = fileLoader.readFile(file);

    assertEquals("üõöä", string);

  }
}
