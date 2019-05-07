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
package io.jooby;

import javax.annotation.Nonnull;

/**
 * Specific exception for bad request.
 *
 * @since 2.0.0
 * @author edgar
 */
public class BadRequestException extends StatusCodeException {

  /**
   * Creates a bad request exception.
   *
   * @param message Message.
   */
  public BadRequestException(@Nonnull String message) {
    super(StatusCode.BAD_REQUEST, message);
  }

  /**
   * Creates a bad request exception.
   *
   * @param message Message.
   * @param cause Throwable.
   */
  public BadRequestException(@Nonnull String message, @Nonnull Throwable cause) {
    super(StatusCode.BAD_REQUEST, message, cause);
  }
}
