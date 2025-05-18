/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.FileUpload;
import io.jooby.Jooby;
import io.jooby.Router;
import io.jooby.ServiceRegistry;
import io.jooby.kt.Kooby;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.AbstractModelConverter;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.Schema;

public class ModelConverterExt extends AbstractModelConverter {

  private static final Set<Class<?>> IGNORE =
      Set.of(Jooby.class, Kooby.class, ServiceRegistry.class, Router.class);

  public ModelConverterExt(ObjectMapper mapper) {
    super(mapper);
  }

  @Override
  public Schema resolve(
      AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    JavaType javaType = _mapper.getTypeFactory().constructType(type.getType());
    if (javaType.isCollectionLikeType() || javaType.isArrayType()) {
      if (isFile(javaType.getContentType().getRawClass())) {
        return new ArraySchema().items(new FileSchema());
      }
    }
    if (isFile(javaType.getRawClass())) {
      return new FileSchema();
    }
    // Skip base apps
    if (IGNORE.contains(javaType.getRawClass())) {
      return null;
    }
    return super.resolve(type, context, chain);
  }

  private boolean isFile(Class<?> type) {
    return type == FileUpload.class || type == Path.class || type == File.class;
  }
}
