package app;


import ee.icefire.clobcopy.CVSClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class Application {


    public static void main(String[] args) throws Exception {



        CVSClient cvsClient = new CVSClient();
        List<File> files = cvsClient.checkout("tmpOut", "PROD_20171207_DIGIALLKIRI");

        for(File file : files) {
            System.out.println(file.getName());
        }


    }
}
