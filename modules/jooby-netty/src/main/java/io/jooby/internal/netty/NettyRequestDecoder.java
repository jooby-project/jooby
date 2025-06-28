/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.handler.codec.http.*;

public class NettyRequestDecoder extends HttpRequestDecoder {

  private static final String GET = HttpMethod.GET.name();
  private static final String POST = HttpMethod.POST.name();
  private static final String PUT = HttpMethod.PUT.name();
  private static final String DELETE = HttpMethod.DELETE.name();

  public NettyRequestDecoder(HttpDecoderConfig config) {
    super(config);
  }

  @Override
  protected HttpMessage createMessage(String[] initialLine) throws Exception {
    return new DefaultHttpRequest(
        HttpVersion.valueOf(initialLine[2]),
        valueOf(initialLine[0]),
        initialLine[1],
        headersFactory);
  }

  private static HttpMethod valueOf(String name) {
    // fast-path
    if (name == GET) {
      return HttpMethod.GET;
    }
    if (name == POST) {
      return HttpMethod.POST;
    }
    if (name == DELETE) {
      return HttpMethod.DELETE;
    }
    if (name == PUT) {
      return HttpMethod.PUT;
    }
    // "slow"-path: ensure method is on upper case
    return HttpMethod.valueOf(toUpperCase(name));
  }

  private static String toUpperCase(String name) {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isLowerCase(name.charAt(i))) {
        return name.toUpperCase();
      }
    }
    return name;
  }
}
