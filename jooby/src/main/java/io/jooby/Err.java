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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * Runtime exception with status code.
 *
 * @author edgar
 * @since 2.0.0
 */
public class Err extends RuntimeException {

  /**
   * Specific exception for bad request.
   *
   * @since 2.0.0
   * @author edgar
   */
  public static class BadRequest extends Err {

    /**
     * Creates a bad request exception.
     *
     * @param message Message.
     */
    public BadRequest(@Nonnull String message) {
      super(StatusCode.BAD_REQUEST, message);
    }

    /**
     * Creates a bad request exception.
     *
     * @param message Message.
     * @param cause Throwable.
     */
    public BadRequest(@Nonnull String message, @Nonnull Throwable cause) {
      super(StatusCode.BAD_REQUEST, message, cause);
    }
  }

  /**
   * Type mismatch exception. Used when a value can't be converted to the required type.
   *
   * @since 2.0.0
   * @author edgar
   */
  public static class TypeMismatch extends BadRequest {
    private final String name;

    /**
     * Creates a type mismatch error.
     *
     * @param name Parameter/attribute name.
     * @param type Parameter/attribute type.
     * @param cause Cause.
     */
    public TypeMismatch(@Nonnull String name, @Nonnull Type type, @Nonnull Throwable cause) {
      super("Cannot convert value: '" + name + "', to: '" + type.getTypeName() + "'", cause);
      this.name = name;
    }

    /**
     * Creates a type mismatch error.
     *
     * @param name Parameter/attribute name.
     * @param type Parameter/attribute type.
     */
    public TypeMismatch(@Nonnull String name, @Nonnull Type type) {
      this(name, type, null);
    }

    /**
     * Parameter/attribute name.
     *
     * @return Parameter/attribute name.
     */
    public @Nonnull String getName() {
      return name;
    }
  }

  /**
   * Missing exception. Used when a required attribute is missing.
   *
   * @since 2.0.0
   * @author edgar
   */
  public static class Missing extends BadRequest {
    private final String name;

    /**
     * Creates a missing exception.
     *
     * @param name Parameter/attribute name.
     */
    public Missing(@Nonnull String name) {
      super("Missing value: '" + name + "'");
      this.name = name;
    }

    /**
     * Parameter/attribute name.
     *
     * @return Parameter/attribute name.
     */
    public String getName() {
      return name;
    }
  }

  /**
   * Provisioning exception, throws by MVC routes when parameter binding fails.
   *
   * @since 2.0.0
   * @author edgar
   */
  public static class Provisioning extends BadRequest {

    /**
     * Creates a provisioning exception.
     *
     * @param parameter Failing parameter.
     * @param cause Cause.
     */
    public Provisioning(@Nonnull Parameter parameter, @Nonnull Throwable cause) {
      this("Unable to provision parameter: '" + toString(parameter) + "', require by: " + toString(
          parameter.getDeclaringExecutable()), cause);
    }

    /**
     * Creates a provisioning exception.
     *
     * @param message Error message.
     * @param cause Cause.
     */
    public Provisioning(@Nonnull String message, @Nonnull Throwable cause) {
      super(message, cause);
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

  private final StatusCode statusCode;

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   */
  public Err(@Nonnull StatusCode statusCode) {
    this(statusCode, statusCode.toString());
  }

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   * @param message Error message.
   */
  public Err(@Nonnull StatusCode statusCode, @Nonnull String message) {
    this(statusCode, message, null);
  }

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   * @param message Error message.
   * @param cause Cause.
   */
  public Err(@Nonnull StatusCode statusCode, @Nonnull String message, @Nullable Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  /**
   * Status code.
   *
   * @return Status code.
   */
  public @Nonnull StatusCode getStatusCode() {
    return statusCode;
  }
}
