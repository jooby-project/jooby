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
package io.jooby.internal;

import io.jooby.QueryString;

import javax.annotation.Nonnull;

public class QueryStringValue extends HashValue implements QueryString {
  private String queryString;

  public QueryStringValue(String queryString) {
    setObjectType("queryString");
    this.queryString = queryString;
  }

  @Nonnull @Override public String queryString() {
    return queryString;
  }
}
