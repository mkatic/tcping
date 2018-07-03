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

        } catch (Exception e) {
            System.out.println(e.getMessage());
            if (e instanceof ParameterException) {
                ((ParameterException) e).getJCommander().usage();
            }
        }
    }
}