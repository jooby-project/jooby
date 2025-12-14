/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.List;

import io.swagger.v3.oas.models.tags.Tag;

public class TagExt extends Tag {

  private final List<HttpRequest> operations;

  public TagExt(Tag tag, List<HttpRequest> operations) {
    setDescription(tag.getDescription());
    setName(tag.getName());
    setExternalDocs(tag.getExternalDocs());
    setExtensions(tag.getExtensions());
    this.operations = operations;
  }

  public List<HttpRequest> getOperations() {
    return operations;
  }

  public List<HttpRequest> getRoutes() {
    return operations;
  }
}
