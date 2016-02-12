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

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.util.Types;

public class SerObject implements Serializable {

  /** def serial ID. */
  private static final long serialVersionUID = 1L;

  Map<String, Object> attr = new HashMap<>();

  @SuppressWarnings("unchecked")
  public <T> T get(final String name) {
    return (T) attr.get(name);
  }

  public void put(final String name, final Object value) {
    Object v = value;
    if (v instanceof ParameterizedType) {
      ParameterizedType type = (ParameterizedType) value;
      v = Types.newParameterizedType(type.getRawType(), type.getActualTypeArguments());
    }
    attr.put(name, v);
  }

}
