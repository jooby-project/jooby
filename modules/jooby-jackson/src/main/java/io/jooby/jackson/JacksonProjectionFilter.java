/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson;

import java.util.ArrayDeque;
import java.util.Deque;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import io.jooby.Projection;

/**
 * High-performance, fully stateless Jackson filter for Jooby Projections. Determines the correct
 * filtering context by walking Jackson's internal stream context path back to the root.
 *
 * @author edgar
 * @since 4.0.0
 */
public class JacksonProjectionFilter extends SimpleBeanPropertyFilter {

  private final Projection<?> root;

  public JacksonProjectionFilter(Projection<?> root) {
    this.root = root;
  }

  @Override
  public void serializeAsField(
      Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
      throws Exception {

    // Bypass projection filtering for Map entries to match Avaje's behavior.
    // We want to dump the entire Map payload without validating its dynamic keys against the static
    // tree.
    if (pojo instanceof java.util.Map) {
      writer.serializeAsField(pojo, jgen, provider);
      return;
    }

    // 1. Resolve the active projection node for the object currently being serialized.
    Projection<?> current = resolveNode(jgen.getOutputContext());

    if (current != null) {
      String fieldName = writer.getName();

      // 2. If the current node has no children defined, it acts as a wildcard (e.g., user
      // requested 'address' instead of 'address(city)'), so we include all fields.
      // Otherwise, we strictly check if the field is in the children map.
      if (current.getChildren().isEmpty() || current.getChildren().containsKey(fieldName)) {
        writer.serializeAsField(pojo, jgen, provider);
      }
    }
  }

  private Projection<?> resolveNode(JsonStreamContext context) {
    if (context == null) {
      return root;
    }

    // Use a Deque to build the path in the correct (root-to-leaf) order by
    // inserting at the front, eliminating the need for Collections.reverse().
    Deque<String> path = new ArrayDeque<>();

    // 1. Start from the parent context to build the path TO the current object being serialized.
    // The current context's name is the property currently being evaluated, not the path.
    JsonStreamContext curr = context.getParent();

    while (curr != null && !curr.inRoot()) {
      // 2. Only extract names from Object contexts. Array boundaries are ignored
      // so that lists (e.g., List<Role>) map seamlessly to their parent field name.
      if (curr.inObject() && curr.getCurrentName() != null) {
        path.addFirst(curr.getCurrentName());
      }
      curr = curr.getParent();
    }

    Projection<?> node = root;
    for (String segment : path) {
      if (node == null) {
        return null; // The path Jackson took is completely outside our projection
      }

      // If we hit a node in our projection tree that exists but has no explicitly
      // defined children, it means the user wants this entire subgraph.
      // We stop traversing Jackson's path and return this wildcard node.
      if (node != root && node.getChildren().isEmpty()) {
        return node;
      }

      node = node.getChildren().get(segment);
    }

    return node;
  }
}
