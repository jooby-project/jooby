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
package org.jooby.internal.mvc;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedParamProvider implements ParamProvider {

  private Map<Executable, List<Param>> cache = new ConcurrentHashMap<>();

  private ParamProvider provider;

  public CachedParamProvider(final ParamProvider provider) {
    this.provider = provider;
  }

  @Override
  public List<Param> parameters(final Executable exec) {
    Parameter[] parameters = exec.getParameters();
    if (parameters.length == 0) {
      return Collections.emptyList();
    }
    List<Param> params = cache.get(exec);
    if (params == null) {
      params = provider.parameters(exec);
      cache.put(exec, params);
    }
    return params;
  }

}
