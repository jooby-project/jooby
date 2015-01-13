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
package org.jooby.internal.reqparam;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.jooby.Err;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Status;
import org.jooby.internal.RouteMetadata;

import com.google.common.primitives.Primitives;
import com.google.common.reflect.Reflection;
import com.google.inject.TypeLiteral;

public class BeanParamInjector {

  public static Object createAndInject(final Request req, final Class<?> beanType)
      throws Exception {
    if (beanType.isPrimitive() || Primitives.isWrapperType(beanType)
        || CharSequence.class.isAssignableFrom(beanType)) {
      throw new Err(Status.BAD_REQUEST);
    }
    if (beanType.isInterface()) {
      return newBeanInterface(req, beanType);
    }
    return newBean(req, beanType);
  }

  private static Object newBean(final Request req, final Class<?> beanType) throws Exception {
    Response rsp = req.require(Response.class);
    RouteMetadata classInfo = req.require(RouteMetadata.class);
    Constructor<?>[] constructors = beanType.getDeclaredConstructors();
    if (constructors.length > 1) {
      throw new IllegalArgumentException("Bean param has multiple constructors: " + beanType);
    }
    final Object bean;
    Constructor<?> constructor = constructors[0];
    RequestParamProvider provider =
        new RequestParamProviderImpl(new RequestParamNameProvider(classInfo));
    List<RequestParam> parameters = provider.parameters(constructor);
    Object[] args = new Object[parameters.size()];
    for (int i = 0; i < args.length; i++) {
      args[i] = parameters.get(i).value(req, rsp);
    }
    // inject args
    bean = constructor.newInstance(args);

    // inject fields
    Field[] fields = beanType.getDeclaredFields();
    for (Field field : fields) {
      int mods = field.getModifiers();
      if (!Modifier.isFinal(mods) && !Modifier.isStatic(mods)) {
        // get
        RequestParam param = new RequestParam(field);
        @SuppressWarnings("unchecked")
        Object value = req.param(param.name).to(param.type);

        // set
        field.setAccessible(true);
        field.set(bean, value);
      }
    }
    return bean;
  }

  private static Object newBeanInterface(final Request req, final Class<?> beanType) {
    return Reflection.newProxy(beanType, (proxy, method, args) -> {
      StringBuilder name = new StringBuilder(method.getName()
          .replace("get", "")
          .replace("is", "")
          );
      name.setCharAt(0, Character.toLowerCase(name.charAt(0)));
      return req.param(name.toString()).to(TypeLiteral.get(method.getGenericReturnType()));
    });
  }

}
