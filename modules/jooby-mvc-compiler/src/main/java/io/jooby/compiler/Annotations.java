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

  String QUERY_PARAM = "io.jooby.annotations.QueryParam";

  String COOKIE_PARAM = "io.jooby.annotations.CookieParam";

  String HEADER_PARAM = "io.jooby.annotations.HeaderParam";

  String FLASH_PARAM = "io.jooby.annotations.FlashParam";

  String FORM_PARAM = "io.jooby.annotations.FormParam";

  Set<String> PATH_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(PATH_PARAM)));

  Set<String> QUERY_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(QUERY_PARAM)));

  Set<String> COOKIE_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(COOKIE_PARAM)));

  Set<String> HEADER_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(HEADER_PARAM)));

  Set<String> FLASH_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(FLASH_PARAM)));

  Set<String> FORM_PARAMS = unmodifiableSet(new LinkedHashSet<>(asList(FORM_PARAM)));
}
