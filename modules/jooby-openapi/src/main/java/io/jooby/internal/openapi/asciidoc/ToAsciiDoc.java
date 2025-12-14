/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.Map;

public interface ToAsciiDoc {
  String list(Map<String, Object> options);

  String table(Map<String, Object> options);
}
