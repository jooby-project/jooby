/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.List;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;

public interface HttpMessage {

  ParameterList getHeaders();

  ParameterList getCookies();

  Schema<?> getBody();

  AsciiDocContext context();

  default Schema<?> selectBody(Schema<?> body, String modifier) {
    if (body != AsciiDocContext.EMPTY_SCHEMA) {
      return switch (modifier) {
        case "full" -> body;
        case "simple" -> context().reduceSchema(body);
        default -> context().emptySchema(body);
      };
    }
    return body;
  }

  default Schema<?> toSchema(Content content, List<String> contentType) {
    if (content == null || content.isEmpty()) {
      return null;
    }
    if (contentType.isEmpty()) {
      // first response
      return content.values().iterator().next().getSchema();
    }
    for (var key : contentType) {
      var mediaType = content.get(key);
      if (mediaType != null) {
        return mediaType.getSchema();
      }
    }
    return null;
  }
}
