package org.jooby.apitool.raml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

public class RamlType {

  public static final RamlType NUMBER = new RamlType("number");
  public static final RamlType INTEGER = new RamlType("integer");
  public static final RamlType BOOLEAN = new RamlType("boolean");
  public static final RamlType STRING = new RamlType("string");
  public static final RamlType FILE = new RamlType("file");
  public static final RamlType DATE_TIME = new RamlType("datetime");

  static {
    NUMBER.example = 0.0;
    INTEGER.example = 0;
    BOOLEAN.example = false;
    STRING.example = "string";
    FILE.example = "file";
    DATE_TIME.example = "1970-01-01T00:00:00.000Z";
  }

  private final RamlTypeRef ref;

  private String type;

  private Map<String, Object> properties;

  private String pattern;

  private Integer minLength;

  private Integer maxLength;

  private Object example;

  public RamlType(String type) {
    this(type, type);
  }

  public RamlType(String type, String name) {
    this.type = type;
    this.ref = new RamlTypeRef(name);
  }

  public RamlType newProperty(String name, String type, boolean required, String... values) {
    if (properties == null) {
      properties = new LinkedHashMap<>();
    }
    if (values.length > 0) {
      properties.put(required ? name : name + "?", ImmutableMap.of("enum", values));
    } else {
      properties.put(required ? name : name + "?", type);
    }
    return this;
  }

  @JsonIgnore
  public RamlTypeRef getRef() {
    return ref;
  }

  public String getType() {
    return type;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(final String pattern) {
    this.pattern = pattern;
  }

  public Integer getMinLength() {
    return minLength;
  }

  public void setMinLength(final Integer minLength) {
    this.minLength = minLength;
  }

  public Integer getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(final Integer maxLength) {
    this.maxLength = maxLength;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public static RamlType valueOf(String name) {
    switch (name.toLowerCase()) {
      case "boolean":
        return BOOLEAN;
      case "byte":
      case "short":
      case "int":
      case "integer":
      case "long":
        return INTEGER;
      case "float":
      case "double":
        return NUMBER;
      case "char":
      case "character":
      case "string":
        return STRING;
      case "file":
      case "upload":
      case "path":
      case "binary":
        return FILE;
      case "date":
      case "datetime":
      case "localdatetime":
        return DATE_TIME;
    }
    return new RamlType("object", name);
  }

  @JsonIgnore
  public boolean isObject() {
    return type.equals("object");
  }

  public Object getExample() {
    return example;
  }

  public void setExample(final Object example) {
    this.example = example;
  }

  public RamlType toArray() {
    return new RamlType(this.type, this.ref.getType() + "[]");
  }
}
