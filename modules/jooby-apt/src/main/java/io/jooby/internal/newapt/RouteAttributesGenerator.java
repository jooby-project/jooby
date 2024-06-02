/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.newapt;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.function.Predicate;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor14;
import javax.lang.model.util.Types;

import io.jooby.apt.MvcContext;
import io.jooby.internal.apt.Opts;

public class RouteAttributesGenerator {
  private record EnumValue(String type, String value) {}

  private static final Predicate<String> HTTP_ANNOTATION =
      it ->
          (it.startsWith("io.jooby.annotation")
                  && !it.contains("io.jooby.annotation.Transactional"))
              || it.startsWith("jakarta.ws.rs")
              || it.startsWith("javax.ws.rs");

  private static final Predicate<String> NULL_ANNOTATION =
      it ->
          it.endsWith("NonNull")
              || it.endsWith("NotNull")
              || it.endsWith("Nonnull")
              || it.endsWith("Nullable");

  private static final Predicate<String> KOTLIN_ANNOTATION = it -> it.equals("kotlin.Metadata");

  private static final Predicate<String> ATTR_FILTER =
      HTTP_ANNOTATION.or(NULL_ANNOTATION).or(KOTLIN_ANNOTATION);

  private final List<String> skip;
  private final Elements elements;
  private final Types types;

  public RouteAttributesGenerator(MvcContext context) {
    var environment = context.getProcessingEnvironment();
    this.elements = environment.getElementUtils();
    this.types = environment.getTypeUtils();
    this.skip = List.of(Opts.stringListOpt(environment, Opts.OPT_SKIP_ATTRIBUTE_ANNOTATIONS, ""));
  }

  public String toSourceCode(MvcRoute route, String indent) {
    var attributes = annotationMap(route.getMethod());
    if (attributes.isEmpty()) {
      return null;
    } else {
      return toSourceCode(annotationMap(route.getMethod()), indent);
    }
  }

  private String toSourceCode(Map<String, Object> attributes, String indent) {
    var buffer = new StringBuilder();
    var separator = ",\n";
    var pairPrefix = "";
    var pairSuffix = "";
    var factoryMethod = "of";
    if (attributes.size() > 10) {
      // Map.of Max size is 10
      pairPrefix = "java.util.Map.entry(";
      pairSuffix = ")";
      factoryMethod = "ofEntries";
    }
    buffer.append("java.util.Map.").append(factoryMethod).append("(\n");
    var lineIndent = indent + "   ";
    for (var e : attributes.entrySet()) {
      buffer.append(lineIndent);
      buffer.append(pairPrefix);
      buffer.append("\"").append(e.getKey()).append("\"").append(", ");
      buffer.append(valueToSourceCode(e.getValue(), lineIndent));
      buffer.append(pairSuffix).append(separator);
    }
    buffer.setLength(buffer.length() - separator.length());
    buffer.append(")");
    return buffer.toString();
  }

  private Object valueToSourceCode(Object value, String indent) {
    if (value instanceof String) {
      return "\"" + value + "\"";
    } else if (value instanceof Character) {
      return "'" + value + "'";
    } else if (value instanceof Map attributeMap) {
      return "\n  " + indent + toSourceCode(attributeMap, indent + " ");
    } else if (value instanceof List list) {
      return valueToSourceCode(list, indent);
    } else if (value instanceof EnumValue enumValue) {
      return enumValue.type + "." + enumValue.value;
    } else if (value instanceof TypeMirror) {
      return value + ".class";
    } else {
      return value;
    }
  }

  private String valueToSourceCode(List values, String indent) {
    var buffer = new StringBuilder();
    buffer.append("java.util.List.of(");
    var separator = ", ";
    for (Object value : values) {
      buffer.append(valueToSourceCode(value, indent)).append(separator);
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
      toMap(elements.getElementValuesWithDefaults(annotation), prefix).forEach(result::putIfAbsent);
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
