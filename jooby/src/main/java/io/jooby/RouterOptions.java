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

public class RouterOptions {
  private boolean caseSensitive = true;

  private boolean ignoreTrailingSlash = true;

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public RouterOptions setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  public boolean isIgnoreTrailingSlash() {
    return ignoreTrailingSlash;
  }

  public RouterOptions setIgnoreTrailingSlash(boolean ignoreTrailingSlash) {
    this.ignoreTrailingSlash = ignoreTrailingSlash;
    return this;
  }
}
