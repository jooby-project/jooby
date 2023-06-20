/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;

/** See https://bugs.openjdk.org/browse/JDK-8180450 */
public class SlowPathChecks {

  static boolean isHttpContent(Object msg) {
    if (msg.getClass() == DefaultHttpContent.class) {
      return true;
    }
    return msg instanceof HttpContent;
  }

  static boolean isHttpRequest(Object msg) {
    if (msg.getClass() == DefaultHttpRequest.class
        || msg.getClass() == DefaultFullHttpRequest.class) {
      return true;
    }
    return msg instanceof HttpRequest;
  }

  static boolean isLastHttpContent(Object msg) {
    if (msg == LastHttpContent.EMPTY_LAST_CONTENT) {
      return true;
    }
    if (msg.getClass() == DefaultLastHttpContent.class) {
      return true;
    }
    return msg instanceof LastHttpContent;
  }
}
