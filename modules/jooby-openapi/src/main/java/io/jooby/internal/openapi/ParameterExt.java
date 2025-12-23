/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.util.List;
import java.util.Objects;

import org.objectweb.asm.tree.AnnotationNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

public class ParameterExt extends Parameter {
  @JsonIgnore private String javaType;

  @JsonIgnore private Object defaultValue;

  @JsonIgnore private boolean single = true;

  @JsonIgnore private List<AnnotationNode> annotations = List.of();

  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }

  public String getJavaType() {
    return javaType;
  }

  public Object getDefaultValue() {
    if (defaultValue != null) {
      if (javaType.equals(boolean.class.getName())) {
        return Objects.equals(defaultValue, 1);
      }
    }
    return defaultValue;
  }

  @Override
  public void setSchema(Schema schema) {
    super.setSchema(schema);
  }

  public boolean isSingle() {
    return single;
  }

  public void setSingle(boolean single) {
    this.single = single;
  }

  public void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public void setRequired(Boolean required) {
    super.setRequired(required);
  }

  @Override
  public String toString() {
    return javaType + " " + getName();
  }

  public static Parameter header(@NonNull String name, @Nullable String value) {
    return basic(name, "header", value);
  }

  @JsonIgnore
  public boolean isPassword() {
    return getSchema() instanceof StringSchema
        && ("password".equalsIgnoreCase(getName())
            || "pass".equalsIgnoreCase(getName())
            || "secret".equalsIgnoreCase(getName()));
  }

  public List<AnnotationNode> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(List<AnnotationNode> annotations) {
    this.annotations = annotations;
  }

  public static Parameter basic(@NonNull String name, @NonNull String in, @Nullable String value) {
    ParameterExt param = new ParameterExt();
    param.setName(name);
    param.setIn(in);
    param.setDefaultValue(value);
    param.setSchema(new StringSchema());
    param.setJavaType(String.class.getName());
    return param;
  }

  public void processConstraints() {
    JakartaConstraints.apply(getSchema(), annotations);
  }
}
