package org.mkatic;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.net.Inet4Address;

@Parameters
enum Cli {

  args;

  @Parameter(names = {"-mps"}, description = "messages per second")
  private int mps = 30;
  @Parameter(names = {"-port"}, description = "well known and ephemeral port numbers are not allowed, default 44444")
  private int port = 44444;
  @Parameter(names = {"-size"}, description = "message size, 50 to 3000 bytes")
  private int packetSize = 300;

  @Parameter(names = {"-c"}, description = "catcher mode")
  private boolean catcher;
  @Parameter(names = {"-p"}, description = "pitcher mode")
  private boolean pitcher;

  @Parameter(names = {"-bind"}, description = "catcher bind address, defaults to 127.0.0.1")
  private String catcherAdress = "127.0.0.1";

  @Parameter(description = "catcher hostname or address, defaults to 127.0.0.1")
  private String pitcherTarget = "127.0.0.1";
  private Inet4Address bindAddress;

  @Parameter(names = "--help", help = true, description = "Show this menu")
  private boolean help;

  private JCommander jc;

  public void init(final String[] args) throws ParameterException {
    jc = JCommander.newBuilder().addObject(this).build();
    jc.parse(args);
    if ((catcher && pitcher) || (!catcher && !pitcher)) {
      throwParameterException("Select catcher or pitcher mode, one and not both");
    }
    if (!inRange(port, 1024, 49151)) {
      throwParameterException("invalid port number");
    }
    if (catcher) {
      checkIpv4(catcherAdress);
    } else {
      if (!inRange(packetSize, 50, 3000)) {
        throwParameterException("invalid packet size");
      }
      if (!inRange(mps, 1, Integer.MAX_VALUE)) {
        throwParameterException("invalid message per second count");
      }
      checkIpv4(pitcherTarget);
    }
  }

  //We need a single ip or hostname in both modes, assign whichever is used to bindAddress
  private void checkIpv4(final String host) throws ParameterException {
    try {
      bindAddress = (Inet4Address) Inet4Address.getByName(host);
    } catch (Exception e) {
      throwParameterException("invalid hostname or IPv4 address");
    }
  }

  private boolean inRange(final int i, final int min, final int max) {
    return (i >= min) && (i <= max);
  }

  private void throwParameterException(final String message) throws ParameterException {
    ParameterException pe = new ParameterException(message);
    pe.setJCommander(jc);
    throw pe;
  }

  public Inet4Address getBindAddress() {
    return bindAddress;
  }

  public boolean startCatcher() {
    return catcher;
  }

  public boolean startPitcher() {
    return pitcher;
  }

  public int getPacketSize() {
    return packetSize;
  }

  public int getPort() {
    return port;
  }

  public int getMps() {
    return mps;
  }
}
