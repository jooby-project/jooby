/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.AbstractList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.swagger.v3.oas.models.parameters.Parameter;

@JsonIgnoreProperties({"includes"})
public class ParameterList extends AbstractList<Parameter> {
  public static final List<String> NAME_DESC = List.of("name", "description");
  public static final List<String> NAME_TYPE_DESC = List.of("name", "type", "description");
  public static final List<String> PARAM = List.of("name", "type", "in", "description");
  private final List<Parameter> parameters;
  private final List<String> includes;

  public ParameterList(List<Parameter> parameters, List<String> includes) {
    this.parameters = parameters;
    this.includes = includes;
  }

  public List<Parameter> parameters() {
    return parameters;
  }

  public List<String> includes() {
    return includes;
  }

  @Override
  public int size() {
    return parameters.size();
  }

  @NonNull @Override
  public String toString() {
    return parameters.stream().map(Parameter::getName).collect(Collectors.joining(", "));
  }

  @Override
  public Parameter get(int index) {
    return parameters.get(index);
  }
}
