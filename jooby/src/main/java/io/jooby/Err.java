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

public class Err extends RuntimeException {

  public static class Missing extends Err {
    public Missing(String name) {
      super(StatusCode.BAD_REQUEST, "Missing value: '" + name + "'");
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
