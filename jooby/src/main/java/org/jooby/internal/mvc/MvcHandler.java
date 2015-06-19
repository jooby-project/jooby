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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.internal.reqparam.RequestParam;
import org.jooby.internal.reqparam.RequestParamProvider;

import com.google.common.base.Throwables;

class MvcHandler implements Route.MethodHandler {

  private Method handler;

  private RequestParamProvider provider;

  public MvcHandler(final Method handler, final RequestParamProvider provider) {
    this.handler = requireNonNull(handler, "Handler method is required.");
    this.provider = requireNonNull(provider, "Param prodiver is required.");
  }

  @Override
  public Method method() {
    return handler;
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Exception {

    try {
      Object target = req.require(handler.getDeclaringClass());

      List<RequestParam> parameters = provider.parameters(handler);
      Object[] args = new Object[parameters.size()];
      for (int i = 0; i < parameters.size(); i++) {
        args[i] = parameters.get(i).value(req, rsp);
      }

      final Object result = handler.invoke(target, args);

      Class<?> returnType = handler.getReturnType();
      if (returnType == void.class) {
        return;
      }

      rsp.send(result);
    } catch (InvocationTargetException ex) {
      Throwable cause = ex.getCause();
      Throwables.propagateIfInstanceOf(cause, Exception.class);
      Throwables.propagate(cause);
    }
  }
}
