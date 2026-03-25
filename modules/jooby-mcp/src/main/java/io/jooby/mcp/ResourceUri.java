/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

/**
 * @author kliushnichenko
 */
public record ResourceUri(String uri) {
  public static final String CTX_KEY = "__resourceUri";
}
