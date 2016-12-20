/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.thymeleaf;

import org.jooby.Env;

class Thlxss {
  private final Env env;

  public Thlxss(final Env env) {
    this.env = env;
  }

  public String escape(final String value, final String xss) {
    return xss(value, xss);
  }

  public String escape(final String value, final String xss1, final String xss2) {
    return xss(value, xss1, xss2);
  }

  public String escape(final String value, final String xss1, final String xss2,
      final String xss3) {
    return xss(value, xss1, xss2, xss3);
  }

  public String escape(final String value, final String xss1, final String xss2, final String xss3,
      final String xss4) {
    return xss(value, xss1, xss2, xss3, xss4);
  }

  public String escape(final String value, final String xss1, final String xss2, final String xss3,
      final String xss4, final String xss5) {
    return xss(value, xss1, xss2, xss3, xss4, xss5);
  }

  private String xss(final String value, final String... xss) {
    return env.xss(xss).apply(value);
  }
}
