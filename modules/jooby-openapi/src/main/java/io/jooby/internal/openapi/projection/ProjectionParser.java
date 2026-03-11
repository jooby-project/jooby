/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.projection;

import static io.jooby.internal.openapi.AsmUtils.*;

import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import io.jooby.Projection;
import io.jooby.annotation.Project;
import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.internal.openapi.OperationExt;
import io.jooby.internal.openapi.ParserContext;
import io.jooby.value.ValueFactory;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;

public class ProjectionParser {
  private OpenAPIExt openapi;
  private ParserContext ctx;

  private ProjectionParser(ParserContext ctx, OpenAPIExt openapi) {
    this.ctx = ctx;
    this.openapi = openapi;
  }

  public static void parse(ParserContext ctx, OpenAPIExt openapi) {
    var parser = new ProjectionParser(ctx, openapi);
    for (OperationExt operation : openapi.getOperations()) {
      parser.parseOperation(operation);
    }
  }

  private void parseOperation(OperationExt operation) {
    var annotations = operation.getAllAnnotations();

    if (operation.isScript()) {
      AsmProjectionParser.parse(ctx, operation.getNode())
          .ifPresent(
              projectionDef -> {
                projection(operation, projectionDef.targetClass, projectionDef.viewString);
              });
    } else {
      var projection = operation.getProjection();
      if (projection != null) {
        projection(operation, projection);
      } else {
        findAnnotationByType(annotations, Project.class).stream()
            .map(it -> stringValue(toMap(it), "value"))
            .forEach(projectionView -> projection(operation, projectionView));
      }
    }
  }

  private void projection(OperationExt operation, String viewString) {
    projection(operation, operation.getDefaultResponse().getJavaType(), viewString);
  }

  private void projection(OperationExt operation, String responseType, String viewString) {
    projection(operation, ctx.javaType(responseType), viewString);
  }

  private void projection(OperationExt operation, JavaType responseType, String viewString) {
    var response = operation.getDefaultResponse();
    var contentType = responseType;
    if (responseType.isArrayType() || responseType.isCollectionLikeType()) {
      contentType = responseType.getContentType();
    }
    var valueFactory = new ValueFactory();
    var isSimple = valueFactory.get(contentType) != null;
    if (isSimple) {
      return;
    }
    if (operation.isScript()) {
      prepareScript(operation, responseType);
    }
    var projection = Projection.of(contentType.getRawClass()).include(viewString);
    var content = response.getContent();
    for (var mediaTypes : content.entrySet()) {
      var prune =
          SchemaPruner.prune(
              mediaTypes.getValue().getSchema(), projection, openapi.getRequiredComponents());
      mediaTypes.getValue().setSchema(prune);
    }
  }

  private void prepareScript(OperationExt operation, JavaType responseType) {
    var response = operation.getDefaultResponse();
    var contentType =
        (responseType.isArrayType() || responseType.isCollectionLikeType())
            ? responseType.getContentType()
            : responseType;
    var schemas = openapi.getRequiredSchemas();
    var schema =
        schemas.computeIfAbsent(
            contentType.getRawClass().getSimpleName(),
            schemaName -> {
              // 1.Initialize
              ctx.schema(contentType.getRawClass());
              //noinspection OptionalGetWithoutIsPresent
              var schemaRef = ctx.schemaRef(contentType.getRawClass().getName());
              return schemaRef.get().schema;
            });
    // Save schemas after projection in case a new one was created
    ctx.schemas().forEach(it -> openapi.schema(it.getName(), it));
    if (responseType.isArrayType() || responseType.isCollectionLikeType()) {
      schema = new ArraySchema().items(schema);
    }
    for (Map.Entry<String, MediaType> e : response.getContent().entrySet()) {
      e.getValue().setSchema(schema);
    }
  }
}
