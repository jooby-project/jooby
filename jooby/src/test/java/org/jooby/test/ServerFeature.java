package org.jooby.test;

import static com.google.common.base.Preconditions.checkState;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.client.utils.URIBuilder;
import org.jooby.Jooby;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;

@RunWith(JoobySuite.class)
public abstract class ServerFeature extends Jooby {

  public static boolean DEBUG = false;

  @Named("port")
  @Inject
  protected int port;

  private String protocol = "http";

  private Client server = null;

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
    server = new Client(protocol + "://localhost:" + port);
    return server;
  }

  public Client request() {
    return request("http");
  }

  public Client request(final String protocol) {
    this.protocol = protocol;
    checkState(server != null, "Server wasn't started");
    return server;
  }

  protected URIBuilder ws(final String... parts) throws Exception {
    URIBuilder builder = new URIBuilder("ws://localhost:" + port + "/"
        + Joiner.on("/").join(parts));
    return builder;
  }

}
