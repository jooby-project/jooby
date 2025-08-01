/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import io.jooby.StatusCode;

public class ThrowsDoc {
  private final String text;
  private final StatusCode statusCode;

  public ThrowsDoc(StatusCode statusCode, String text) {
    this.statusCode = statusCode;
    if (text == null) {
      this.text = statusCode.reason();
    } else {
      this.text = statusCode.reason() + ": " + text;
    }
  }

  public String getText() {
    return text;
  }

  public StatusCode getStatusCode() {
    return statusCode;
  }
}
