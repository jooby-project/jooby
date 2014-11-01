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

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import org.objectweb.asm.Type;

public class ASMParamNameProvider implements ParamNameProvider {

  private Map<String, String[]> params;

  public ASMParamNameProvider(final Map<String, String[]> params) {
    this.params = requireNonNull(params, "The params are required.");
  }

  @Override
  public String name(final int index, final Parameter parameter) {
    String descriptor = descriptor(parameter.getDeclaringExecutable());
    String[] names = this.params.get(descriptor);
    return names[index];
  }

  @SuppressWarnings("rawtypes")
  private static String descriptor(final Executable exec) {
    if (exec instanceof Method) {
      return exec.getName() + Type.getMethodDescriptor((Method) exec);
    } else if (exec instanceof Constructor) {
      return exec.getName() + Type.getConstructorDescriptor((Constructor) exec);
    }
    throw new IllegalArgumentException(exec.getClass().getName());
  }

}
