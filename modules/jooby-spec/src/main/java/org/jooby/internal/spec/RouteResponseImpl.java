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
package org.jooby.internal.spec;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.jooby.spec.RouteResponse;

import com.google.common.base.MoreObjects;

public class RouteResponseImpl extends SerObject implements RouteResponse {

  /** default serial. */
  private static final long serialVersionUID = 1L;

  public RouteResponseImpl(final Type type, final String doc,
      final Map<Integer, String> statusCodes) {
    put("type", type);
    put("doc", doc);
    put("statusCodes", statusCodes);
    if (type == Object.class && !statusCodes.isEmpty()) {
      Entry<Integer, String> next = statusCodes.entrySet().iterator().next();
      if (next.getKey() == 204) {
        put("type", void.class);
      }
    }
  }

  protected RouteResponseImpl() {
  }

  @Override
  public Type type() {
    return get("type");
  }

  @Override
  public Optional<String> doc() {
    return Optional.ofNullable(get("doc"));
  }

  @Override
  public Map<Integer, String> statusCodes() {
    return get("statusCodes");
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper("")
        .add("type", type())
        .add("statusCodes", statusCodes())
        .toString() + "\n";
  }
}
