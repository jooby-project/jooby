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
package org.jooby.internal.parser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jooby.Err;
import org.jooby.Mutant;
import org.jooby.Parser;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.internal.ParameterNameProvider;
import org.jooby.internal.mvc.RequestParam;
import org.jooby.internal.parser.bean.BeanPlan;

import com.google.common.primitives.Primitives;
import com.google.common.reflect.Reflection;
import com.google.inject.TypeLiteral;

import javaslang.control.Try;

public class BeanParser implements Parser {

  private Function<? super Throwable, Try<? extends Object>> MISSING = x -> {
    return x instanceof Err.Missing ? Try.success(null) : Try.failure(x);
  };

  private Function<? super Throwable, Try<? extends Object>> RETHROW = Try::failure;

  private Function<? super Throwable, Try<? extends Object>> recoverMissing;

  @SuppressWarnings("rawtypes")
  private final Map<TypeLiteral, BeanPlan> forms;

  public BeanParser(final boolean allowNulls) {
    this.recoverMissing = allowNulls ? MISSING : RETHROW;
    this.forms = new ConcurrentHashMap<>();
  }

  @Override
  public Object parse(final TypeLiteral<?> type, final Context ctx) throws Throwable {
    Class<?> beanType = type.getRawType();
    if (Primitives.isWrapperType(Primitives.wrap(beanType))
        || CharSequence.class.isAssignableFrom(beanType)) {
      return ctx.next();
    }
    return ctx.ifparams(map -> {
      final Object bean;
      if (List.class.isAssignableFrom(beanType)) {
        bean = newBean(ctx.require(Request.class), ctx.require(Response.class), map, type);
      } else if (beanType.isInterface()) {
        bean = newBeanInterface(ctx.require(Request.class), ctx.require(Response.class), beanType);
      } else {
        bean = newBean(ctx.require(Request.class), ctx.require(Response.class), map, type);
      }

      return bean;
    });
  }

  @Override
  public String toString() {
    return "bean";
  }

  @SuppressWarnings("rawtypes")
  private Object newBean(final Request req, final Response rsp,
      final Map<String, Mutant> params, final TypeLiteral type) throws Throwable {
    BeanPlan form = forms.get(type);
    if (form == null) {
      form = new BeanPlan(req.require(ParameterNameProvider.class), type);
      forms.put(type, form);
    }
    return form.newBean(p -> value(p, req, rsp), params.keySet());
  }

  private Object newBeanInterface(final Request req, final Response rsp, final Class<?> beanType) {
    return Reflection.newProxy(beanType, (proxy, method, args) -> {
      StringBuilder name = new StringBuilder(method.getName()
          .replace("get", "")
          .replace("is", ""));
      name.setCharAt(0, Character.toLowerCase(name.charAt(0)));
      return value(new RequestParam(method, name.toString(), method.getGenericReturnType()), req,
          rsp);
    });
  }

  private Object value(final RequestParam param, final Request req, final Response rsp)
      throws Throwable {
    return Try.of(() -> param.value(req, rsp))
        .recoverWith(recoverMissing)
        .getOrElseThrow(Function.identity());
  }

}
