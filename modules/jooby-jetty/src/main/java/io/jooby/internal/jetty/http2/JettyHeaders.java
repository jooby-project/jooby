/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty.http2;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.PreEncodedHttpField;

import io.jooby.MediaType;

public final class JettyHeaders {
  public static final HttpField SERVER = new PreEncodedHttpField(HttpHeader.SERVER, "J");
  public static final HttpField TEXT_PLAIN =
      new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());

  public static final HttpField JSON =
      new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON.asString());

  public static HttpField contentType(MediaType contentType) {
    if (MediaType.text.equals(contentType)) {
      return TEXT_PLAIN;
    } else if (MediaType.json.equals(contentType)) {
      return JSON;
    }
    return new HttpField(HttpHeader.CONTENT_TYPE, contentType.toContentTypeHeader());
  }
}
