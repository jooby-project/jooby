/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.lang.reflect.Type;
import java.util.Map;

import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.media.Schema;

public class ResolvedSchemaExt extends ResolvedSchema {
  public Map<Type, Schema> referencedSchemasByType;
}
