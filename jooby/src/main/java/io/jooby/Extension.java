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

/**
 * Simple extension contract for adding and reusing commons application infrastructure components
 * and/or integrate with external libraries.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Extension {

  default boolean lateinit() {
    return false;
  }

  /**
   * Install, configure additional features to a Jooby application.
   *
   * @param application Jooby application.
   * @throws Exception If something goes wrong.
   */
  void install(@Nonnull Jooby application) throws Exception;
}
