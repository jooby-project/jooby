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

/**
 * Runtime exception with status code.
 *
 * @author edgar
 * @since 2.0.0
 */
public class StatusCodeException extends RuntimeException {

  private final StatusCode statusCode;

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   */
  public StatusCodeException(@Nonnull StatusCode statusCode) {
    this(statusCode, statusCode.toString());
  }

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   * @param message Error message.
   */
  public StatusCodeException(@Nonnull StatusCode statusCode, @Nonnull String message) {
    this(statusCode, message, null);
  }

  /**
   * Creates an error with the given status code.
   *
   * @param statusCode Status code.
   * @param message Error message.
   * @param cause Cause.
   */
  public StatusCodeException(@Nonnull StatusCode statusCode, @Nonnull String message, @Nullable Throwable cause) {
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
