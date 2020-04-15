/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;

public class ProxyPeerAddress {

  private static final String X_FORWARDED_FOR = "X-Forwarded-For";
  private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
  private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
  private static final String X_FORWARDED_PORT = "X-Forwarded-Port";

  private String remoteAddress;

  private String scheme;

  private String host;

  private int port;

  private ProxyPeerAddress() {
  }

  /**
   * The X-Forwarded-For (XFF) header is a de-facto standard header for identifying the originating
   * IP address of a client connecting to a web server through an HTTP proxy or a load balancer.
   * When traffic is intercepted between clients and servers, server access logs contain the
   * IP address of the proxy or load balancer only. To see the original IP address of the client,
   * the X-Forwarded-For request header is used.
   *
   * @return Remote address.
   */
  public String getRemoteAddress() {
    return remoteAddress;
  }

  /**
   * The X-Forwarded-Proto (XFP) header is a de-facto standard header for identifying the protocol
   * (HTTP or HTTPS) that a client used to connect to your proxy or load balancer. Your server
   * access logs contain the protocol used between the server and the load balancer, but not the
   * protocol used between the client and the load balancer. To determine the protocol used between
   * the client and the load balancer, the X-Forwarded-Proto request header can be used.
   *
   * @return Scheme.
   */
  public String getScheme() {
    return scheme;
  }

  /**
   * The X-Forwarded-Host (XFH) header is a de-facto standard header for identifying the original
   * host requested by the client in the Host HTTP request header.
   *
   * @return Host.
   */
  public String getHost() {
    return host;
  }

  /**
   * Port from {@link #getHost()}.
   *
   * @return Port from {@link #getHost()}.
   */
  public int getPort() {
    return port;
  }

  public void set(Context ctx) {
    ctx.setRemoteAddress(getRemoteAddress());
    ctx.setHost(getHost());
    ctx.setScheme(getScheme());
    ctx.setPort(getPort());
  }

  public static ProxyPeerAddress parse(Context ctx) {
    ProxyPeerAddress result = new ProxyPeerAddress();

    String forwardedFor = ctx.header(X_FORWARDED_FOR)
        .toOptional().orElseGet(ctx::getRemoteAddress);
    result.remoteAddress = mostRecent(forwardedFor);

    String forwardedProto = ctx.header(X_FORWARDED_PROTO)
        .toOptional().orElseGet(ctx::getScheme);
    result.scheme = mostRecent(forwardedProto);

    String forwardedHost = ctx.header(X_FORWARDED_HOST)
        .toOptional().orElseGet(ctx::getHost);

    String forwardedPort = ctx.header(X_FORWARDED_PORT)
        .valueOrNull();

    String value = mostRecent(forwardedHost);
    if (value.startsWith("[")) {
      int end = value.lastIndexOf("]");
      if (end == -1) {
        end = 0;
      }
      int index = value.indexOf(":", end);
      if (index != -1) {
        forwardedPort = value.substring(index + 1);
        value = value.substring(0, index);
      }
    } else {
      int index = value.lastIndexOf(":");
      if (index != -1) {
        forwardedPort = value.substring(index + 1);
        value = value.substring(0, index);
      }
    }
    String hostHeader = value;
    if (forwardedPort != null) {
      try {
        result.port = Integer.parseInt(mostRecent(forwardedPort));
      } catch (NumberFormatException ignore) {
        result.port = defaultPort(ctx, hostHeader);
      }
    } else {
      result.port = defaultPort(ctx, hostHeader);
    }

    result.host = hostHeader;

    return result;
  }

  private static int defaultPort(Context ctx, String host) {
    if ("localhost".equals(host)) {
      return ctx.getServerPort();
    }
    return ctx.isSecure() ? 443 : 80;
  }

  private static String mostRecent(String header) {
    int index = header.indexOf(',');
    if (index == -1) {
      return header;
    } else {
      return header.substring(0, index);
    }
  }
}
