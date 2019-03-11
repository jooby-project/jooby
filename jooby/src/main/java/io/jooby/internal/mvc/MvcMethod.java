package io.jooby.internal.mvc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class MvcMethod {

  int line = -1;

  private List<String> parameters;

  Method method;

  String source;

  private String httpMethod;

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
    return method.getDeclaringClass().getName() + "$" + httpMethod + "$" + method.getName();
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }
}
