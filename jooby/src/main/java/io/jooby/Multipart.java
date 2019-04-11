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

import io.jooby.internal.HashValue;

import javax.annotation.Nonnull;

/**
 * Multipart class for direct MVC parameter provisioning.
 *
 * HTTP request must be encoded as {@link MediaType#MULTIPART_FORMDATA}.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Multipart extends Formdata {

  /**
   * Creates a new multipart object.
   *
   * @return Multipart instance.
   */
  static @Nonnull Multipart create() {
    return new HashValue(null).setObjectType("multipart");
  }
}
