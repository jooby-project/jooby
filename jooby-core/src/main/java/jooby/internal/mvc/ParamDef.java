package jooby.internal.mvc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;

import jooby.mvc.Header;

import com.google.common.collect.ImmutableSet;

public class ParamDef {

  private String name;

  private Class<?> type;

  private Type parameterizedType;

  private Set<Annotation> annotations = Collections.emptySet();

  public ParamDef(final Parameter parameter) {
    this(parameter.getName(), parameter.getType(), parameter.getParameterizedType(), parameter
        .getAnnotations());
  }

  public ParamDef(final String name, final Class<?> type, final Type parameterizedType,
      final Annotation[] annotations) {
    this(name, type, parameterizedType, ImmutableSet.copyOf(annotations));
  }

  public ParamDef(final String name, final Class<?> type, final Type parameterizedType,
      final Set<Annotation> annotations) {
    this.name = name;
    this.type = type;
    this.parameterizedType = parameterizedType;
    this.annotations = annotations;
    // rewrite name if @Named is present
    getAnnotation(Named.class).ifPresent(named -> this.name = named.value());
    getAnnotation(Header.class).ifPresent(header -> {
      String h = header.value();
      if (h.length() > 0) {
        this.name = header.value();
      }
    });
  }

  public String name() {
    return name;
  }

  public Class<?> type() {
    return type;
  }

  public Type parameterizedType() {
    return parameterizedType;
  }

  public <A extends Annotation> Optional<A> getAnnotation(final Class<A> type) {
    for (Annotation annotation : annotations) {
      if (type.isInstance(annotation)) {
        return Optional.of(type.cast(annotation));
      }
    }
    return Optional.empty();
  }

  public boolean typeIs(final Type type) {
    if (parameterizedType.equals(type) || this.type.equals(type)) {
      return true;
    }
    if (parameterizedType instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) parameterizedType;
      return pt.getActualTypeArguments()[0].equals(type);
    }

    return false;
  }

  public boolean hasAnnotation(final Class<? extends Annotation> type) {
    return getAnnotation(type).isPresent();
  }

  public Set<Annotation> annotations() {
    return annotations;
  }

  @Override
  public String toString() {
    return name + "(" + type + ")";
  }
}
