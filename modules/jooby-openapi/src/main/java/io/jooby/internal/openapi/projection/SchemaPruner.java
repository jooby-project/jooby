/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.projection;

import java.util.List;
import java.util.Map;

import io.jooby.Projection;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Utility to create a pruned OpenAPI Schema based on a Jooby Projection.
 *
 * @since 4.0.0
 */
public class SchemaPruner {

  public static Schema<?> prune(
      Schema<?> original, Projection<?> projection, Components components) {
    if (original == null || projection == null) {
      return original;
    }

    // 1. Deep wildcard (e.g., address(*)). Return the original schema/ref untouched.
    if (projection.getChildren().isEmpty()) {
      return original;
    }

    // 2. Handle Arrays: Recursively prune the items.
    if (original instanceof ArraySchema) {
      ArraySchema arraySchema = (ArraySchema) original;
      Schema<?> prunedItems = prune(arraySchema.getItems(), projection, components);

      ArraySchema newArraySchema = new ArraySchema();
      copyMetadata(original, newArraySchema);
      newArraySchema.setItems(prunedItems);
      return newArraySchema;
    }

    // --- AGGRESSIVE ROOT RESOLUTION ---
    // We MUST resolve the actual object schema if 'original' is just a $ref pointer,
    // otherwise we can't see the properties we need to prune.
    Schema<?> actualSchema = resolveSchema(original, components);
    if (actualSchema == null || actualSchema.getProperties() == null) {
      return original;
    }

    // --- THE CACHE CHECK (Early Exit) ---
    // We use the base name of the $ref (if it had one) to name our new component.
    String baseName = getBaseName(original);
    String newComponentName = null;

    if (baseName != null) {
      newComponentName = generateProjectedName(baseName, projection);

      if (components.getSchemas().containsKey(newComponentName)) {
        return new Schema<>().$ref("#/components/schemas/" + newComponentName);
      }
    }

    // --- THE PRUNING ---
    Schema<?> prunedSchema = new ObjectSchema();
    copyMetadata(actualSchema, prunedSchema);
    Map<String, Schema> originalProps = actualSchema.getProperties();

    for (Map.Entry<String, Projection<?>> entry : projection.getChildren().entrySet()) {
      String propName = entry.getKey();
      Projection<?> childNode = entry.getValue();
      Schema<?> originalPropSchema = originalProps.get(propName);

      if (originalPropSchema != null) {
        Schema<?> prunedProp = prune(originalPropSchema, childNode, components);
        prunedSchema.addProperty(propName, prunedProp);
      }
    }

    // --- REGISTER IN COMPONENTS CACHE ---
    if (newComponentName != null && components != null) {
      components.addSchemas(newComponentName, prunedSchema);
      return new Schema<>().$ref("#/components/schemas/" + newComponentName);
    }

    return prunedSchema;
  }

  /**
   * Resolves a $ref to its actual Schema object in the Components map. If the schema is not a $ref,
   * it returns the schema itself.
   */
  private static Schema<?> resolveSchema(Schema<?> schema, Components components) {
    if (schema.get$ref() != null) {
      // Extract "User" from "#/components/schemas/User"
      String refName = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
      Schema<?> resolved = components.getSchemas().get(refName);
      return resolved != null ? resolved : schema;
    }
    return schema;
  }

  private static String getBaseName(Schema<?> schema) {
    if (schema.get$ref() != null) {
      return schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
    }
    return schema.getName();
  }

  private static String generateProjectedName(String baseName, Projection<?> projection) {
    String shortHash = Integer.toString(Math.abs(projection.toView().hashCode()), 36);
    return baseName + "_" + shortHash;
  }

  private static void copyMetadata(Schema<?> source, Schema<?> target) {
    target.setName(source.getName());
    target.setTitle(source.getTitle());
    target.setDescription(source.getDescription());
    target.setFormat(source.getFormat());
    target.setDefault(source.getDefault());
    if (source.getExample() != null) {
      target.setExample(source.getExample());
    }
    target.setEnum((List) source.getEnum());
    target.setRequired(source.getRequired());

    target.setMaximum(source.getMaximum());
    target.setMinimum(source.getMinimum());
    target.setMaxLength(source.getMaxLength());
    target.setMinLength(source.getMinLength());
    target.setPattern(source.getPattern());
    target.setMaxItems(source.getMaxItems());
    target.setMinItems(source.getMinItems());
    target.setUniqueItems(source.getUniqueItems());

    if (source.getExtensions() != null) {
      source.getExtensions().forEach(target::addExtension);
    }
  }
}
