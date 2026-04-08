/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.MediaType;

/**
 * JSON module using Jackson: https://jooby.io/modules/jackson2.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new JacksonModule());
 *
 *   get("/", ctx -> {
 *     MyObject myObject = ...;
 *     // send json
 *     return myObject;
 *   });
 *
 *   post("/", ctx -> {
 *     // read json
 *     MyObject myObject = ctx.body(MyObject.class);
 *     // send json
 *     return myObject;
 *   });
 * }
 * }</pre>
 *
 * For body decoding the client must specify the <code>Content-Type</code> header set to <code>
 * application/json</code>.
 *
 * <p>You can retrieve the {@link ObjectMapper} via require call:
 *
 * <pre>{@code
 * {
 *
 *   ObjectMapper mapper = require(ObjectMapper.class);
 *
 * }
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/jackson2.
 *
 * @author edgar
 * @since 2.0.0
 * @deprecated Use {@link io.jooby.jackson.JacksonModule} instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class JacksonModule extends Jackson2Module {

  public JacksonModule(@NonNull ObjectMapper mapper, @NonNull MediaType contentType) {
    super(mapper, contentType);
  }

  /**
   * Creates a Jackson module.
   *
   * @param mapper Object mapper to use.
   */
  public JacksonModule(@NonNull ObjectMapper mapper) {
    super(mapper);
  }

  /** Creates a Jackson module using the default object mapper from {@link #create(Module...)}. */
  public JacksonModule() {
    super(create());
  }

  /**
   * Default object mapper. Install {@link Jdk8Module}, {@link JavaTimeModule}, {@link
   * ParameterNamesModule}.
   *
   * @param modules Extra/additional modules to install.
   * @return Object mapper instance.
   */
  public static ObjectMapper create(Module... modules) {
    return Jackson2Module.create(modules);
  }
}
