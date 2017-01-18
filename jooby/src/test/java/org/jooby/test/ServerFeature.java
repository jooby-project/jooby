package org.jooby.test;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;

import org.apache.http.client.utils.URIBuilder;
import org.jooby.Jooby;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;

/**
 * Internal use only.
 *
 * @author edgar
 */
@RunWith(JoobySuite.class)
public abstract class ServerFeature extends Jooby {

  public static boolean DEBUG = false;

  protected int port;

  protected int securePort;

  public static String protocol = "http";

  private Client server = null;

  public ServerFeature(final String prefix) {
    super(prefix);
  }

  public ServerFeature() {
  }

  @Before
  public void debug() {
    if (DEBUG) {
      java.util.logging.Logger.getLogger("httpclient.wire.header").setLevel(
          java.util.logging.Level.FINEST);
      java.util.logging.Logger.getLogger("httpclient.wire.content").setLevel(
          java.util.logging.Level.FINEST);

      System.setProperty("org.apache.commons.logging.Log",
          "org.apache.commons.logging.impl.SimpleLog");
      System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
      System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
      System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
      System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers",
          "debug");
    }
  }

  @Rule
  public Client createServer() {
    checkState(server == null, "Server was created already");
    server = new Client(
        protocol + "://localhost:" + (protocol.equals("https") ? securePort : port));
    return server;
  }

  public Client request() {
    checkState(server != null, "Server wasn't started");
    return server;
  }

  public Client https() throws IOException {
    server.stop();
    server = new Client("https://localhost:" + securePort);
    server.start();
    return server;
  }

  protected URIBuilder ws(final String... parts) throws Exception {
    URIBuilder builder = new URIBuilder("ws://localhost:" + port + "/"
        + Joiner.on("/").join(parts));
    return builder;
  }

  @Override
  public Jooby securePort(final int port) {
    this.securePort = port;
    return super.securePort(port);
  }

  @Override
  public Jooby port(final int port) {
    this.port = port;
    return super.port(port);
  }

}
