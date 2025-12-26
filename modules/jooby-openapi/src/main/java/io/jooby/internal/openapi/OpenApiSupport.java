/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import static io.swagger.v3.oas.models.Components.COMPONENTS_SCHEMAS_REF;

import java.util.NoSuchElementException;
import java.util.Optional;

import io.swagger.v3.oas.models.media.Schema;

public class OpenApiSupport {

  protected final OpenAPIExt openapi;

  public OpenApiSupport(OpenAPIExt openapi) {
    this.openapi = openapi;
  }

  public Schema<?> resolveSchema(Schema<?> schema) {
    if (schema.get$ref() != null) {
      return resolveSchemaInternal(schema.get$ref())
          .orElseThrow(() -> new NoSuchElementException("Schema not found: " + schema.get$ref()));
    }
    return schema;
  }

  protected Optional<Schema<?>> resolveSchemaInternal(String name) {
    var components = openapi.getComponents();
    if (components == null || components.getSchemas() == null) {
      throw new NoSuchElementException("No schema found");
    }
    if (name.startsWith(COMPONENTS_SCHEMAS_REF)) {
      name = name.substring(COMPONENTS_SCHEMAS_REF.length());
    }
    return Optional.ofNullable((Schema<?>) components.getSchemas().get(name));
  }
}
