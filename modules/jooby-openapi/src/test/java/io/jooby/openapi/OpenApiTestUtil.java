package io.jooby.openapi;

import io.jooby.internal.openapi.Operation;
import io.jooby.internal.openapi.Response;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;

import java.util.function.Consumer;

public class OpenApiTestUtil {

  public static void withResponse(Operation operation, Consumer<Response> consumer) {
    consumer.accept(operation.getResponse());
  }

  public static void withContent(Response response, Consumer<Content> consumer) {
    consumer.accept(response.getContent());
  }

  public static void withSchema(Response response, String mediaType, Consumer<Schema> consumer) {
    withSchema(response.getContent(), mediaType, consumer);
  }

  public static void withSchema(Content content, String mediaType, Consumer<Schema> consumer) {
    consumer.accept(content.get(mediaType).getSchema());
  }
}
