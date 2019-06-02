/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import io.jooby.Context;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MvcMethod {

  int line = -1;

  private List<String> parameters;

  private MvcAnnotation model;

  Method method;

  String source;

  private String pattern;

  public String getSource() {
    return source == null ? method.getDeclaringClass().getSimpleName() : source;
  }

  public int getLine() {
    return line;
  }

  public Method getMethod() {
    return method;
  }

  public String getParameterName(int index) {
    Parameter parameter = method.getParameters()[index];
    return parameter.isNamePresent() ? parameter.getName() : parameters.get(index);
  }

  void parameter(String name) {
    if (parameters == null) {
      parameters = new ArrayList<>();
    }
    parameters.add(name);
  }

  public void destroy() {
    if (parameters != null) {
      parameters.clear();
    }
    this.method = null;
  }

  public String getHandlerName() {
    return method.getDeclaringClass().getName() + "$" + model.getMethod() + "$" + method.getName();
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  public MvcAnnotation getModel() {
    return model;
  }

  public String getHttpMethod() {
    return model.getMethod();
  }

  public void setModel(MvcAnnotation model) {
    this.model = model;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public MvcMethod copy() {
    MvcMethod result = new MvcMethod();
    result.model = model;
    result.method = method;
    result.parameters = parameters;
    result.source = source;
    result.line = line;
    return result;
  }

  public boolean isSuspendFunction() {
    int len = method.getParameterCount();
    if (len > 0) {
      return method.getParameterTypes()[len - 1].getName().equals("kotlin.coroutines.Continuation");
    }
    return false;
  }

  public Type getReturnType(ClassLoader loader) throws ClassNotFoundException {
    if (isSuspendFunction()) {
      return loader.loadClass("kotlin.coroutines.Continuation");
    }
    boolean isVoid = method.getReturnType() == void.class;
    return isVoid ? Context.class : method.getGenericReturnType();
  }
}
