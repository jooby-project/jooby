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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.View;
import org.jooby.fn.ExSupplier;
import org.jooby.mvc.Viewable;

class MvcRoute implements Route.Handler {

  private Method router;

  private ParamProvider provider;

  private List<MediaType> produces;

  public MvcRoute(final Method router, final ParamProvider provider, final List<MediaType> produces) {
    this.router = requireNonNull(router, "Router method is required.");
    this.provider = requireNonNull(provider, "Param prodiver is required.");
    this.produces = requireNonNull(produces, "Produce types are required.");
  }

  @Override
  public void handle(final Request req, final Response res) throws Exception {

    Object handler = req.getInstance(router.getDeclaringClass());

    List<Param> parameters = provider.parameters(router);
    Object[] args = new Object[parameters.size()];
    for (int i = 0; i < parameters.size(); i++) {
      args[i] = parameters.get(i).get(req, res);
    }

    final Object result = router.invoke(handler, args);

    Class<?> returnType = router.getReturnType();
    if (returnType == void.class || returnType == Void.class) {
      // ignore glob pattern
      if (!req.route().pattern().contains("*")) {
        res.status(Status.NO_CONTENT);
      }
      return;
    }
    res.status(Status.OK);

    // format!
    List<MediaType> accept = req.accept();

    ExSupplier<Object> viewable = () -> {
      if (result instanceof View) {
        return result;
      }
      // default view name
      String defaultViewName = Optional.ofNullable(router.getAnnotation(Viewable.class))
          .map(template -> template.value().isEmpty() ? router.getName() : template.value())
          .orElse(router.getName());
      return View.of(defaultViewName, result);
    };

    ExSupplier<Object> notViewable = () -> result;

    List<MediaType> viewableTypes = res.viewableTypes();
    Function<MediaType, ExSupplier<Object>> provider = (type) -> {
      Optional<MediaType> matches = viewableTypes.stream()
        .filter(it -> it.matches(type))
        .findFirst();
        return matches.isPresent() ? viewable : notViewable;
    };

    Response.Formatter formatter = res.format();

    // add formatters
    accept.forEach(type -> formatter.when(type, provider.apply(type)));
    produces.forEach(type -> formatter.when(type, provider.apply(type)));

    // send!
    formatter.send();
  }
}
