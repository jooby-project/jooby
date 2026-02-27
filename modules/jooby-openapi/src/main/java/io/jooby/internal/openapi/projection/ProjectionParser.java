/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.projection;

import static io.jooby.internal.openapi.AsmUtils.*;

import io.jooby.Projection;
import io.jooby.annotation.Project;
import io.jooby.internal.openapi.OpenAPIExt;
import io.jooby.internal.openapi.OperationExt;
import io.jooby.internal.openapi.ParserContext;
import io.jooby.value.ValueFactory;
import io.swagger.v3.oas.models.media.Schema;

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

    var projection = operation.getProjection();
    if (projection != null) {
      projection(operation, projection);
    } else {
      findAnnotationByType(annotations, Project.class).stream()
          .map(it -> stringValue(toMap(it), "value"))
          .forEach(projectionView -> projection(operation, projectionView));
    }
  }

  private void projection(OperationExt operation, String viewString) {
    var response = operation.getDefaultResponse();
    var javaType = ctx.javaType(response.getJavaType());
    if (javaType.isArrayType() || javaType.isCollectionLikeType()) {
      javaType = javaType.getContentType();
    }
    var valueFactory = new ValueFactory();
    var isSimple = valueFactory.get(javaType) != null;
    if (isSimple) {
      return;
    }
    var projection = Projection.of(javaType.getRawClass()).include(viewString);
    var content = response.getContent();
    for (var mediaTypes : content.entrySet()) {
      Schema<?> prune =
          SchemaPruner.prune(
              mediaTypes.getValue().getSchema(), projection, openapi.getComponents());
      mediaTypes.getValue().setSchema(prune);
    }
  }
}
