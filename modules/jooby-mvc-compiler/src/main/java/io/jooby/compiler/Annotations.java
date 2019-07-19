/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.compiler;

import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

public interface Annotations {
  String PATH = "io.jooby.annotations.Path";

  String GET = "io.jooby.annotations.GET";

  String POST = "io.jooby.annotations.POST";

  Set<String> HTTP_METHODS = unmodifiableSet(new LinkedHashSet<>(asList(GET, POST)));

  String PATH_PARAM = "io.jooby.annotations.PathParam";

  Set<String> PATH_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(PATH_PARAM)));

  static boolean isPathParam(String type) {
    return PATH_PARAMS.contains(type);
  }
}
