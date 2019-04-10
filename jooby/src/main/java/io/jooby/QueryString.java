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
 * Query string class for direct MVC parameter provisioning.
 *
 * @author edgar
 * @since 2.0.0
 */
public class QueryString extends HashValue {
  /** Empty query string. */
  public static final QueryString EMPTY = new QueryString("");

  private final String queryString;

  /**
   * Creates a query string object.
   *
   * @param queryString Raw string (no decoded it).
   */
  public QueryString(@Nonnull String queryString) {
    this.queryString = queryString;
  }

  /**
   * Query string with the leading <code>?</code> or empty string.
   *
   * @return Query string with the leading <code>?</code> or empty string.
   */
  public String queryString() {
    return queryString;
  }
}
