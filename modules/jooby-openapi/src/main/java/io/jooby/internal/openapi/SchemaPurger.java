/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.util.*;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

public class SchemaPurger {

  public static void purgeUnused(OpenAPI openAPI) {
    if (openAPI == null
        || openAPI.getComponents() == null
        || openAPI.getComponents().getSchemas() == null) {
      return;
    }

    Set<String> visitedSchemas = new HashSet<>();
    Queue<String> queue = new LinkedList<>();

    // 1. Gather Roots (Using your visitor/parser)
    // Scan Paths, Parameters, Responses, and RequestBodies for $refs pointing to schemas.
    Set<String> rootSchemaNames = extractRootSchemaNames(openAPI);

    for (String schemaName : rootSchemaNames) {
      if (visitedSchemas.add(schemaName)) {
        queue.add(schemaName);
      }
    }

    Map<String, Schema> allSchemas = openAPI.getComponents().getSchemas();

    // 2. Traverse Graph (BFS)
    while (!queue.isEmpty()) {
      String currentName = queue.poll();
      Schema<?> currentSchema = allSchemas.get(currentName);

      if (currentSchema == null) continue;

      // Scan this schema for nested $refs (properties, items, allOf, anyOf, oneOf)
      Set<String> nestedSchemaNames = extractSchemaNamesFromSchema(currentSchema);

      for (String nestedName : nestedSchemaNames) {
        // CIRCULAR REFERENCE CHECK:
        // visitedSchemas.add() returns false if the element is already present.
        // If it's already visited, we ignore it, breaking the cycle.
        if (visitedSchemas.add(nestedName)) {
          queue.add(nestedName);
        }
      }
    }

    // 3. Purge Unused
    // retainAll efficiently drops any key from the components map that isn't in our visited set.
    allSchemas.keySet().retainAll(visitedSchemas);
  }

  // --- Helper Methods (To be integrated with your OpenAPI visitor) ---

  private static Set<String> extractRootSchemaNames(OpenAPI openAPI) {
    Set<String> roots = new HashSet<>();

    // 1. Scan Paths for schemas used in operations
    if (openAPI.getPaths() != null) {
      openAPI
          .getPaths()
          .values()
          .forEach(
              pathItem -> {

                // Check path-level parameters
                if (pathItem.getParameters() != null) {
                  pathItem
                      .getParameters()
                      .forEach(
                          param -> roots.addAll(extractSchemaNamesFromSchema(param.getSchema())));
                }

                if (pathItem.readOperations() != null) {
                  pathItem
                      .readOperations()
                      .forEach(
                          operation -> {

                            // Check operation parameters (Query, Path, Header, etc.)
                            if (operation.getParameters() != null) {
                              operation
                                  .getParameters()
                                  .forEach(
                                      param ->
                                          roots.addAll(
                                              extractSchemaNamesFromSchema(param.getSchema())));
                            }

                            // Check Request Bodies
                            if (operation.getRequestBody() != null
                                && operation.getRequestBody().getContent() != null) {
                              operation
                                  .getRequestBody()
                                  .getContent()
                                  .values()
                                  .forEach(
                                      mediaType ->
                                          roots.addAll(
                                              extractSchemaNamesFromSchema(mediaType.getSchema())));
                            }

                            // Check Responses
                            if (operation.getResponses() != null) {
                              operation
                                  .getResponses()
                                  .values()
                                  .forEach(
                                      response -> {
                                        if (response.getContent() != null) {
                                          response
                                              .getContent()
                                              .values()
                                              .forEach(
                                                  mediaType ->
                                                      roots.addAll(
                                                          extractSchemaNamesFromSchema(
                                                              mediaType.getSchema())));
                                        }
                                      });
                            }
                          });
                }
              });
    }

    // 2. Scan Components for shared non-schema objects that reference schemas
    if (openAPI.getComponents() != null) {

      // Shared Parameters
      if (openAPI.getComponents().getParameters() != null) {
        openAPI
            .getComponents()
            .getParameters()
            .values()
            .forEach(param -> roots.addAll(extractSchemaNamesFromSchema(param.getSchema())));
      }

      // Shared Responses
      if (openAPI.getComponents().getResponses() != null) {
        openAPI
            .getComponents()
            .getResponses()
            .values()
            .forEach(
                response -> {
                  if (response.getContent() != null) {
                    response
                        .getContent()
                        .values()
                        .forEach(
                            mediaType ->
                                roots.addAll(extractSchemaNamesFromSchema(mediaType.getSchema())));
                  }
                });
      }

      // Shared RequestBodies
      if (openAPI.getComponents().getRequestBodies() != null) {
        openAPI
            .getComponents()
            .getRequestBodies()
            .values()
            .forEach(
                requestBody -> {
                  if (requestBody.getContent() != null) {
                    requestBody
                        .getContent()
                        .values()
                        .forEach(
                            mediaType ->
                                roots.addAll(extractSchemaNamesFromSchema(mediaType.getSchema())));
                  }
                });
      }
    }

    return roots;
  }

  private static Set<String> extractSchemaNamesFromSchema(Schema<?> schema) {
    Set<String> refs = new HashSet<>();

    // 1. Check direct ref
    if (schema.get$ref() != null) {
      refs.add(extractName(schema.get$ref()));
    }

    // 2. Check properties
    if (schema.getProperties() != null) {
      schema
          .getProperties()
          .values()
          .forEach(prop -> refs.addAll(extractSchemaNamesFromSchema(prop)));
    }

    // 3. Check arrays (items)
    if (schema.getItems() != null) {
      refs.addAll(extractSchemaNamesFromSchema(schema.getItems()));
    }

    // 4. Check compositions
    if (schema.getAllOf() != null)
      schema.getAllOf().forEach(s -> refs.addAll(extractSchemaNamesFromSchema(s)));
    if (schema.getAnyOf() != null)
      schema.getAnyOf().forEach(s -> refs.addAll(extractSchemaNamesFromSchema(s)));
    if (schema.getOneOf() != null)
      schema.getOneOf().forEach(s -> refs.addAll(extractSchemaNamesFromSchema(s)));

    // 5. Check additionalProperties (Maps)
    if (schema.getAdditionalProperties() instanceof Schema) {
      refs.addAll(extractSchemaNamesFromSchema((Schema<?>) schema.getAdditionalProperties()));
    }

    return refs;
  }

  private static String extractName(String ref) {
    return ref != null ? ref.substring(ref.lastIndexOf('/') + 1) : null;
  }
}
