/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.apt.JoobyProcessor.Options.SKIP_ATTRIBUTE_ANNOTATIONS;
import static io.jooby.internal.apt.CodeBlock.indent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.function.Predicate;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor14;
import javax.lang.model.util.Types;

import io.jooby.apt.JoobyProcessor.Options;

public class RouteAttributesGenerator {
  private record EnumValue(String type, String value) {}

  private static final Predicate<String> HTTP_ANNOTATION =
      it ->
          (it.startsWith("io.jooby.annotation")
                  && !it.contains("io.jooby.annotation.Transactional"))
              || it.startsWith("jakarta.ws.rs")
              || it.startsWith("javax.ws.rs");

  private static final Predicate<String> OPEN_API = it -> it.startsWith("io.swagger");

  private static final Predicate<String> NULL_ANNOTATION =
      it ->
          it.endsWith("NonNull")
              || it.endsWith("NotNull")
              || it.endsWith("Nonnull")
              || it.endsWith("Nullable");

  private static final Predicate<String> KOTLIN_ANNOTATION = it -> it.equals("kotlin.Metadata");

  private static final Predicate<String> ATTR_FILTER =
      HTTP_ANNOTATION.or(NULL_ANNOTATION).or(KOTLIN_ANNOTATION).or(OPEN_API);

  private final List<String> skip;
  private final Types types;

  public RouteAttributesGenerator(MvcContext context) {
    var environment = context.getProcessingEnvironment();
    this.types = environment.getTypeUtils();
    this.skip = Options.stringListOpt(environment, SKIP_ATTRIBUTE_ANNOTATIONS);
  }

  public Optional<String> toSourceCode(boolean kt, MvcRoute route, int indent) {
    var attributes = annotationMap(route.getMethod());
    if (attributes.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(
          toSourceCode(kt, annotationMap(route.getMethod()), indent + 6, new HashMap<>()));
    }
  }

  private String toSourceCode(
      boolean kt, Map<String, Object> attributes, int indent, Map<String, Object> defaults) {
    var buffer = new StringBuilder();
    var separator = ",\n";
    var pairPrefix = "";
    var pairSuffix = "";
    var typeInfo = kt ? "<String, Any>" : "";
    var factoryMethod = "of" + typeInfo;
    if (attributes.size() > 10) {
      // Map.of Max size is 10
      pairPrefix = "java.util.Map.entry(";
      pairSuffix = ")";
      factoryMethod = "ofEntries" + typeInfo;
    }
    buffer.append("java.util.Map.").append(factoryMethod).append("(\n");
    for (var e : attributes.entrySet()) {
      buffer.append(indent(indent + 4));
      buffer.append(pairPrefix);
      buffer.append(CodeBlock.string(e.getKey())).append(", ");
      buffer.append(valueToSourceCode(kt, e.getValue(), indent + 4, defaults));
      buffer.append(pairSuffix).append(separator);
    }
    buffer.setLength(buffer.length() - separator.length());
    buffer.append(")");
    return buffer.toString();
  }

  private Object valueToSourceCode(
      boolean kt, Object value, int indent, Map<String, Object> defaults) {
    if (value instanceof String) {
      return CodeBlock.string((String) value);
    } else if (value instanceof Character) {
      return "'" + value + "'";
    } else if (value instanceof Map attributeMap) {
      return "\n  " + indent(indent) + toSourceCode(kt, attributeMap, indent + 1, defaults);
    } else if (value instanceof List list) {
      return valueToSourceCode(kt, list, indent, defaults);
    } else if (value instanceof EnumValue enumValue) {
      return enumValue.type + "." + enumValue.value;
    } else if (value instanceof TypeMirror) {
      return value + ".class";
    } else if (value instanceof Float) {
      return value + "f";
    } else if (value instanceof Double) {
      return value + "d";
    } else if (value instanceof Long) {
      return value + "L";
    } else if (value instanceof Short) {
      return "(short)" + value;
    } else if (value instanceof Byte) {
      return "(byte)" + value;
    } else {
      return value;
    }
  }

  private String valueToSourceCode(
      boolean kt, List values, int indent, Map<String, Object> defaults) {
    var buffer = new StringBuilder();
    buffer.append("java.util.List.of(");
    var separator = ", ";
    for (Object value : values) {
      buffer.append(valueToSourceCode(kt, value, indent, defaults)).append(separator);
    }
    buffer.setLength(buffer.length() - separator.length());
    buffer.append(")");
    return buffer.toString();
  }

  private Map<String, Object> annotationMap(ExecutableElement method) {
    // class
    var attributes = annotationMap(method.getEnclosingElement().getAnnotationMirrors());
    // method
    attributes.putAll(annotationMap(method.getAnnotationMirrors()));
    return attributes;
  }

  private Map<String, Object> annotationMap(List<? extends AnnotationMirror> annotations) {
    var result = new TreeMap<String, Object>();
    for (var annotation : annotations) {
      var elem = annotation.getAnnotationType().asElement();
      var retention = elem.getAnnotation(Retention.class);
      var retentionPolicy = retention == null ? RetentionPolicy.CLASS : retention.value();
      var type = annotation.getAnnotationType().toString();
      if (
      // ignore annotations not available at runtime
      retentionPolicy != RetentionPolicy.RUNTIME
          // ignore core, jars annotations
          || ATTR_FILTER.test(type)
          // ignore user specified annotations
          || skip.stream().anyMatch(type::startsWith)) {

        continue;
      }
      String prefix = elem.getSimpleName().toString();
      // Set all values and then override with present values (fix for JDK 11+)
      result.putAll(toMap(annotation.getElementValues(), prefix));
      // toMap(elements.getElementValuesWithDefaults(annotation),
      // prefix).forEach(result::putIfAbsent);
    }
    return result;
  }

  private Map<String, Object> toMap(
      Map<? extends ExecutableElement, ? extends AnnotationValue> values, String prefix) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (var attribute : values.entrySet()) {
      var value = annotationValue(attribute.getValue());
      if (value != null && !value.toString().isEmpty()) {
        var method = attribute.getKey().getSimpleName().toString();
        var name = method.equals("value") ? prefix : prefix + "." + method;
        // Found value is override on JDK 11 with default annotation value, we trust that spe
        result.putIfAbsent(name, value);
      }
    }
    return result;
  }

  private Object annotationValue(AnnotationValue annotationValue) {
    try {
      return annotationValue.accept(
          new SimpleAnnotationValueVisitor14<Object, Void>() {
            @Override
            protected Object defaultAction(Object value, Void unused) {
              return value;
            }

            @Override
            public Object visitAnnotation(AnnotationMirror mirror, Void unused) {
              var annotation = annotationMap(List.of(mirror));
              return annotation.isEmpty() ? null : annotation;
            }

            @Override
            public Object visitEnumConstant(VariableElement enumeration, Void unused) {
              var typeMirror = enumeration.asType();
              var element = types.asElement(typeMirror);
              return new EnumValue(element.toString(), enumeration.toString());
            }

            @Override
            public Object visitArray(List<? extends AnnotationValue> values, Void unused) {
              if (!values.isEmpty()) {
                var result = new ArrayList<>();
                for (var it : values) {
                  result.add(annotationValue(it));
                }
                return result;
              }
              return null;
            }
          },
          null);
    } catch (UnsupportedOperationException x) {
      // See https://github.com/jooby-project/jooby/issues/2417
      return null;
    }
  }
}
