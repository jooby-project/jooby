/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;

/**
 * Force SSL handler. Check for non-HTTPs request and force client to use HTTPs by redirecting the
 * call to the HTTPs version.
 *
 * @author edgar
 */
public class SSLHandler implements Route.Before {
  private static final int SECURE_PORT = 443;
  private final String host;
  private final int port;
  private boolean useProxy;

  /**
   * Creates a SSLHandler and redirect non-HTTPS request to the given host and port.
   *
   * @param host Host to redirect.
   * @param port HTTP port.
   */
  public SSLHandler(@Nonnull String host, int port) {
    this.host = host;
    this.port = port;
  }

  /**
   * Creates a SSLHandler and redirect non-HTTPS request to the given host.
   *
   * @param host Host to redirect.
   */
  public SSLHandler(@Nonnull String host) {
    this(host, SECURE_PORT);
  }

  /**
   * Creates a SSLHandler and redirect non-HTTPs requests to the HTTPS version of this call. Host
   * is recreated from <code>Host</code> header or <code>X-Forwarded-Host</code>.
   *
   * @param useProxy True for trust/use the <code>X-Forwarded-Host</code>. Otherwise, only the
   *     <code>Host</code> header is used it.
   * @param port HTTPS port.
   */
  public SSLHandler(boolean useProxy, int port) {
    this.host = null;
    this.port = port;
    this.useProxy = useProxy;
  }

  /**
   * Creates a SSLHandler and redirect non-HTTPs requests to the HTTPS version of this call. Host
   * is recreated from <code>Host</code> header or <code>X-Forwarded-Host</code>.
   *
   * @param useProxy True for trust/use the <code>X-Forwarded-Host</code>. Otherwise, only the
   *     <code>Host</code> header is used it.
   */
  public SSLHandler(boolean useProxy) {
    this(useProxy, SECURE_PORT);
  }

  @Override public void apply(@Nonnull Context ctx) {
    if (!ctx.isSecure()) {
      String host;
        if (this.host == null) {
        String hostAndPort = ctx.getHostAndPort(useProxy);
        int i = hostAndPort.lastIndexOf(':');
        host = i > 0 ? hostAndPort.substring(0, i) : hostAndPort;
      } else {
        host = this.host;
      }
      StringBuilder buff = new StringBuilder("https://");
      buff.append(host);

      if (host.equals("localhost")) {
        int securePort = ctx.getRouter().getServerOptions().getSecurePort();
        buff.append(":").append(securePort);
      } else {
        if (port > 0 && port != SECURE_PORT) {
          buff.append(":").append(port);
        }
      }
      buff.append(ctx.pathString());
      buff.append(ctx.queryString());
      ctx.sendRedirect(buff.toString());
    }
  }
}
