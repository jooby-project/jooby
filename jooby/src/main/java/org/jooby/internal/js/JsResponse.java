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
package org.jooby.internal.js;

import java.util.Map;
import java.util.stream.Collectors;

import org.jooby.Response;

import com.google.common.collect.ImmutableMap;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JsResponse extends Response.Forwarding {

  public JsResponse(final Response rsp) {
    super(rsp);
  }

  public void sendjs(final ScriptObjectMirror result) throws Exception {
    Object value;
    if (result.isArray()) {
      value = result.entrySet().stream()
          .map(Map.Entry::getValue)
          .collect(Collectors.toList());
    } else {
      value = ImmutableMap.copyOf(result);
    }
    super.send(value);
  }

}
