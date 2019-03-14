/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
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

  public String getPattern() {
    return pattern;
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

  public MvcMethod copy() {
    MvcMethod result = new MvcMethod();
    result.httpMethod = httpMethod;
    result.method = method;
    result.parameters = parameters;
    result.source = source;
    result.line = line;
    result.pattern = pattern;
    return result;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }
}
