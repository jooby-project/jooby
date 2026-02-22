/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.jooby.Projection;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.AnyGetterWriter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

/** A Jackson 3 property filter that enforces a Jooby Projection. */
public class JacksonProjectionFilter extends SimpleBeanPropertyFilter {

  private final Projection<?> projection;

  public JacksonProjectionFilter(Projection<?> projection) {
    this.projection = projection;
  }

  @Override
  public void serializeAsProperty(
      Object pojo, JsonGenerator gen, SerializationContext provider, PropertyWriter writer)
      throws Exception {

    // Bypass projection filtering for Map entries to match Avaje's behavior.
    // We want to dump the entire Map payload without validating its dynamic keys against the static
    // tree.
    if (pojo instanceof java.util.Map) {
      writer.serializeAsProperty(pojo, gen, provider);
      return;
    }

    if (include(writer, gen)) {
      writer.serializeAsProperty(pojo, gen, provider);
    } else if (!gen.canOmitProperties()) {
      writer.serializeAsOmittedProperty(pojo, gen, provider);
    } else if (writer instanceof AnyGetterWriter) {
      // Support for @JsonAnyGetter maps
      ((AnyGetterWriter) writer).getAndFilter(pojo, gen, provider, this);
    }
  }

  // Custom include method that takes JsonGenerator so we can access the context
  private boolean include(PropertyWriter writer, JsonGenerator gen) {
    if (projection == null || projection.getChildren().isEmpty()) {
      return true; // No projection applied, serialize everything
    }

    String propName = writer.getName();
    TokenStreamContext context = gen.streamWriteContext();

    // 1. Build the current path from Jackson's TokenStreamContext
    List<String> path = new ArrayList<>();
    path.add(propName);

    // Walk up the context tree to build the full property path.
    // We skip ARRAY contexts because projections don't care about array indexes.
    TokenStreamContext parent = context.getParent();
    while (parent != null && !parent.inRoot()) {
      if (parent.currentName() != null) {
        path.add(parent.currentName());
      }
      parent = parent.getParent();
    }

    // Context gives us leaf-to-root, so we reverse it for root-to-leaf traversal
    Collections.reverse(path);

    // 2. Traverse our Projection tree
    Projection<?> currentNode = projection;

    for (String pathSegment : path) {
      // If the node has no children defined, it acts as a "deep wildcard"
      if (currentNode.getChildren().isEmpty()) {
        return true;
      }

      currentNode = currentNode.getChildren().get(pathSegment);

      // If the path segment isn't found in the projection tree, block it
      if (currentNode == null) {
        return false;
      }
    }

    return true;
  }
}
