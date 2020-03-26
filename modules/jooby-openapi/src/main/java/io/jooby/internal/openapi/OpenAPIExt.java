/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jooby.SneakyThrows;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class OpenAPIExt extends OpenAPI {
  @JsonIgnore
  private List<OperationExt> operations = Collections.emptyList();

  @JsonIgnore
  private String source;

  public static Optional<OpenAPI> fromTemplate(Path basedir, ClassLoader classLoader, String templateName) {
    try {
      Path path = basedir.resolve("conf").resolve(templateName);
      if (Files.exists(path)) {
        return Optional.of(Yaml.mapper().readValue(path.toFile(), OpenAPIExt.class));
      }
      URL resource = classLoader.getResource(templateName);
      if (resource != null) {
        return Optional.of(Yaml.mapper().readValue(resource, OpenAPIExt.class));
      }
      return Optional.empty();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public List<OperationExt> getOperations() {
    return operations;
  }

  public void setOperations(List<OperationExt> operations) {
    this.operations = operations;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String classname) {
    this.source = classname;
  }
}
