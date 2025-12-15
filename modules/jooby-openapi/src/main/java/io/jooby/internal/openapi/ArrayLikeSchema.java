/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.models.media.Schema;

@JsonIgnoreProperties({"items"})
public class ArrayLikeSchema<T> extends Schema<T> {

  public static ArrayLikeSchema<?> create(Schema<?> schema, Schema<?> items) {
    var arrayLikeSchema = new ArrayLikeSchema<>();
    arrayLikeSchema.setItems(items);
    arrayLikeSchema.setProperties(schema.getProperties());
    arrayLikeSchema.setType(schema.getType());
    arrayLikeSchema.setTypes(schema.getTypes());
    arrayLikeSchema.setName(schema.getName());
    return arrayLikeSchema;
  }
}
