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
package org.jooby.reflect;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.internal.reqparam.RequestParamNameProviderImpl;

/**
 * Like {@link ParameterNameProvider} but it check for {@link Named} presence first. Useful to
 * extract parameter names from a MVC Route.
 *
 * @author edgar
 * @since 0.6.2
 */
public class ReqParameterNameProvider implements ParameterNameProvider {

  private RequestParamNameProviderImpl paramerterNameProvider;

  @Inject
  public ReqParameterNameProvider(final ParameterNameProvider paramerterNameProvider) {
    this.paramerterNameProvider = new RequestParamNameProviderImpl(paramerterNameProvider);
  }

  @Override
  public String[] names(final Executable exec) {
    Parameter[] parameters = exec.getParameters();
    String[] names = new String[parameters.length];
    for (int i = 0; i < names.length; i++) {
      names[i] = paramerterNameProvider.name(parameters[i]);
    }
    return names;
  }

}
