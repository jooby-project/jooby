/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;

public record HttpParamList(List<HttpParam> parameters, List<String> includes)
    implements Iterable<HttpParam> {
  public static final List<String> NAME_DESC = List.of("name", "description");
  public static final List<String> NAME_TYPE_DESC = List.of("name", "type", "description");
  public static final List<String> PARAM = List.of("name", "type", "in", "description");

  @NonNull @Override
  public Iterator<HttpParam> iterator() {
    return parameters.iterator();
  }

  public boolean isEmpty() {
    return parameters.isEmpty();
  }
}
