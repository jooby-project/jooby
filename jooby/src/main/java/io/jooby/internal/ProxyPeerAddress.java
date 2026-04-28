/*
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

  private ProxyPeerAddress() {}

  public String getRemoteAddress() {
    return remoteAddress;
  }

  public String getScheme() {
    return scheme;
  }

  public String getHost() {
    return host;
  }

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

    String forwardedFor = ctx.header(X_FORWARDED_FOR).toOptional().orElseGet(ctx::getRemoteAddress);
    result.remoteAddress = mostRecent(forwardedFor);

    String forwardedProto = ctx.header(X_FORWARDED_PROTO).toOptional().orElseGet(ctx::getScheme);
    result.scheme = mostRecent(forwardedProto);

    String forwardedHost = ctx.header(X_FORWARDED_HOST).toOptional().orElseGet(ctx::getHost);
    String forwardedPort = ctx.header(X_FORWARDED_PORT).valueOrNull();

    String value = mostRecent(forwardedHost);
    String hostPort = null;

    if (value.startsWith("[")) {
      int end = value.lastIndexOf("]");
      if (end != -1) {
        int index = value.indexOf(":", end);
        if (index != -1) {
          hostPort = value.substring(index + 1);
          value = value.substring(0, index);
        }
      }
    } else {
      int index = value.lastIndexOf(":");
      if (index != -1) {
        hostPort = value.substring(index + 1);
        value = value.substring(0, index);
      }
    }

    if (forwardedPort == null && hostPort != null) {
      forwardedPort = hostPort;
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
      return header.trim();
    } else {
      return header.substring(0, index).trim();
    }
  }
}
