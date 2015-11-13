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
import java.util.stream.IntStream;

import org.jooby.reflect.ParameterNameProvider;

public class RequestParamNameProviderImpl {

  private ParameterNameProvider nameProvider;

  public RequestParamNameProviderImpl(final ParameterNameProvider nameProvider) {
    this.nameProvider = nameProvider;
  }

  public String name(final Parameter parameter) {
    String name = RequestParam.nameFor(parameter);
    if (name != null) {
      return name;
    }
    // asm
    Executable exec = parameter.getDeclaringExecutable();
    Parameter[] params = exec.getParameters();
    int idx = IntStream.range(0, params.length)
        .filter(i -> params[i].equals(parameter))
        .findFirst()
        .getAsInt();
    String[] names = nameProvider.names(exec);
    return names[idx];
  }

}
