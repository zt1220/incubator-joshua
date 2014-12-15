package joshua.decoder;

import java.io.IOException;

/**
 * @author orluke
 * 
 */
public class ArgsParser {

  private String configFile = null;

  /**
   * Parse the arguments passed from the command line when the JoshuaDecoder application was
   * executed from the command line.
   * 
   * @param args
   */
  public ArgsParser(String[] args, JoshuaConfiguration joshuaConfiguration) {

    /*
     * Look for a verbose flag, -v.
     * 
     * Look for an argument to the "-config" flag to find the config file, if any. 
     */
    if (args.length >= 1) {
      // Search for a verbose flag
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-v")) {
          Decoder.VERBOSE = Integer.parseInt(args[i + 1].trim());
          break;
        }
      } 

      // Search for the configuration file
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-c") || args[i].equals("-config")) {

          setConfigFile(args[i + 1].trim());
          try {
            Decoder.LOG(1, "Parameters read from configuration file:");
            joshuaConfiguration.readConfigFile(getConfigFile());
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }

          break;
        }
      }

      // Now process all the command-line args
      Decoder.LOG(1, "Parameters overridden from the command line:");
      joshuaConfiguration.processCommandLineOptions(args);
    }
  }

  /**
   * @return the configFile
   */
  public String getConfigFile() {
    return configFile;
  }

  /**
   * @param configFile the configFile to set
   */
  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }
}
