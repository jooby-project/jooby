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
package org.jooby;

/**
 * HTTP Verbs (a.k.a methods)
 */
public enum Verb {
  /** HTTP OPTIONS. */
  OPTIONS,

  /** HTTP GET. */
  GET,

  /** HTTP HEAD. */
  HEAD,

  /** HTTP POST. */
  POST,

  /** HTTP PUT. */
  PUT,

  /** HTTP PATCH. */
  PATCH,

  /** HTTP DELETE. */
  DELETE,

  /** HTTP TRACE. */
  TRACE,

  /** HTTP CONNECT. */
  CONNECT;

  /**
   * True if this verb matches any of the current verbs.
   *
   * @param verbs The verbs to test.
   * @return True if this verb matches any of the current verbs.
   */
  public boolean is(final Verb... verbs) {
    for(Verb verb: verbs) {
      if (verb == this) {
        return true;
      }
    }
    return false;
  }
}