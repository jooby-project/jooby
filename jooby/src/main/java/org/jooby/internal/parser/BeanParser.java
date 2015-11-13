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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jooby.Mutant;
import org.jooby.Parser;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.internal.mvc.RequestParam;
import org.jooby.internal.mvc.RequestParamNameProviderImpl;
import org.jooby.internal.mvc.RequestParamProvider;
import org.jooby.internal.mvc.RequestParamProviderImpl;
import org.jooby.reflect.ParameterNameProvider;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.Reflection;
import com.google.inject.TypeLiteral;

public class BeanParser implements Parser {

  @Override
  public Object parse(final TypeLiteral<?> type, final Context ctx) throws Exception {
    Class<?> beanType = type.getRawType();
    if (Primitives.isWrapperType(Primitives.wrap(beanType))
        || CharSequence.class.isAssignableFrom(beanType)) {
      return ctx.next();
    }
    return ctx.ifparams(map -> {
      final Object bean;
      if (beanType.isInterface()) {
        bean = newBeanInterface(ctx.require(Request.class), beanType);
      } else {
        bean = newBean(ctx.require(Request.class), ctx.require(Response.class), map, beanType);
      }

      return bean == null ? ctx.next() : bean;
    });
  }

  @Override
  public String toString() {
    return "bean";
  }

  private Object newBean(final Request req, final Response rsp,
      final Map<String, Mutant> params, final Class<?> beanType)
      throws Exception {
    ParameterNameProvider classInfo = req.require(ParameterNameProvider.class);
    Constructor<?>[] constructors = beanType.getDeclaredConstructors();
    if (constructors.length > 1) {
      return null;
    }
    final Object bean;
    Constructor<?> constructor = constructors[0];
    RequestParamProvider provider =
        new RequestParamProviderImpl(new RequestParamNameProviderImpl(classInfo));
    List<RequestParam> parameters = provider.parameters(constructor);
    Object[] args = new Object[parameters.size()];
    for (int i = 0; i < args.length; i++) {
      args[i] = parameters.get(i).value(req, rsp);
    }
    // inject args
    bean = constructor.newInstance(args);

    // inject fields
    for (Entry<String, Mutant> param : params.entrySet()) {
      String pname = param.getKey();
      try {
        List<String> path = name(pname);
        Object root = seek(bean, path);
        String fname = path.get(path.size() - 1);

        Field field = root.getClass().getDeclaredField(fname);
        int mods = field.getModifiers();
        if (!Modifier.isFinal(mods) && !Modifier.isStatic(mods) && !Modifier.isTransient(mods)) {
          // get
          RequestParam fparam = new RequestParam(field);
          @SuppressWarnings("unchecked")
          Object value = req.param(pname).to(fparam.type);

          // set
          field.setAccessible(true);
          field.set(root, value);
        }
      } catch (NoSuchFieldException ex) {
        LoggerFactory.getLogger(Request.class).debug("No matching field for: {}", pname);
      }
    }
    return bean;
  }

  /**
   * Given a path like: <code>profile[address][country][name]</code> this method will traverse the
   * path and seek the object in [country].
   *
   * @param bean Root bean.
   * @param path Path to traverse.
   * @return The last object in the path.
   * @throws Exception If something goes wrong.
   */
  private Object seek(final Object bean, final List<String> path) throws Exception {
    Object it = bean;
    for (int i = 0; i < path.size() - 1; i++) {
      Field field = it.getClass().getDeclaredField(path.get(i));
      field.setAccessible(true);

      Object next = field.get(it);
      if (next == null) {
        next = field.getType().newInstance();
        field.set(it, next);
      }
      it = next;
    }
    return it;
  }

  private Object newBeanInterface(final Request req, final Class<?> beanType) {

    return Reflection.newProxy(beanType, (proxy, method, args) -> {
      StringBuilder name = new StringBuilder(method.getName()
          .replace("get", "")
          .replace("is", "")
          );
      name.setCharAt(0, Character.toLowerCase(name.charAt(0)));
      return req.param(name.toString()).to(TypeLiteral.get(method.getGenericReturnType()));
    });
  }

  private static List<String> name(final String name) {
    return Splitter.on(new CharMatcher() {
      @Override
      public boolean matches(final char c) {
        return c == '[' || c == ']';
      }
    }).trimResults().omitEmptyStrings().splitToList(name);
  }
}
