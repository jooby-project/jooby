/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.NoSuchElementException;
import java.util.StringJoiner;
import java.util.stream.Stream;

public class Err extends RuntimeException {

  public static class Missing extends NoSuchElementException {
    private final String parameter;

    public Missing(String name) {
      super("Missing value: '" + name + "'");
      this.parameter = name;
    }

    public String getParameter() {
      return parameter;
    }
  }

  public static class Provisioning extends NoSuchElementException {

    public Provisioning(Parameter parameter, Throwable cause) {
      this("Unable to provision parameter: '" + toString(parameter) + "', require by: " + toString(
          parameter.getDeclaringExecutable()), cause);
    }

    public Provisioning(String message, Throwable cause) {
      super(message);
      initCause(cause);
    }

    private static String toString(Parameter parameter) {
      return parameter.getName() + ": " + parameter.getParameterizedType();
    }

    private static String toString(Executable method) {
      StringBuilder buff = new StringBuilder();
      if (method instanceof Constructor) {
        buff.append("constructor ");
        buff.append(method.getDeclaringClass().getCanonicalName());
      } else {
        buff.append("method ");
        buff.append(method.getDeclaringClass().getCanonicalName());
        buff.append(".");
        buff.append(method.getName());
      }
      buff.append("(");
      StringJoiner params = new StringJoiner(", ");
      Stream.of(method.getGenericParameterTypes()).forEach(type -> params.add(type.getTypeName()));
      buff.append(params);
      buff.append(")");
      return buff.toString();
    }
  }

  public static class BadRequest extends Err {
    public BadRequest(String message) {
      super(StatusCode.BAD_REQUEST, message);
    }

    public BadRequest(String message, Throwable cause) {
      super(StatusCode.BAD_REQUEST, message, cause);
    }
  }

  public final StatusCode statusCode;

  public Err(StatusCode status) {
    this(status, status.toString());
  }

  public Err(StatusCode status, String message) {
    this(status, message, null);
  }

  public Err(StatusCode status, String message, Throwable cause) {
    super(message, cause);
    this.statusCode = status;
  }

}
