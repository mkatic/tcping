package org.mkatic;

import com.beust.jcommander.ParameterException;

public class Tcping {

  public static void main(String[] args) {
    try {
      Cli.args.init(args);
      //Cli singleton makes sure either one is set, if not, we fail before reaching this code
      if (Cli.args.startCatcher()) {
        Catcher.instance.start();
      } else {
        Pitcher pitcher = new Pitcher();
        pitcher.start();
      }
    } catch (ParameterException pe) {
      pe.getJCommander().usage();
    } catch (InterruptedException e) {
      System.out.println("InterruptedException: " + e.getMessage());
      System.exit(-1);
    }
  }
}