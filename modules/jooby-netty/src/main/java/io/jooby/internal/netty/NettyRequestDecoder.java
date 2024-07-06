/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.handler.codec.http.*;

public class NettyRequestDecoder extends HttpRequestDecoder {

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
    if (name == HttpMethod.GET.name()) {
      return HttpMethod.GET;
    }
    if (name == HttpMethod.POST.name()) {
      return HttpMethod.POST;
    }
    if (name == HttpMethod.DELETE.name()) {
      return HttpMethod.DELETE;
    }
    if (name == HttpMethod.PUT.name()) {
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
