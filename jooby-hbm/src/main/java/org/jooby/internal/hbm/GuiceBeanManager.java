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
package org.jooby.internal.hbm;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jooby.Registry;

import com.google.common.reflect.Reflection;

import javaslang.concurrent.Promise;

/**
 * Hack hibernate and allow guice to create entity listeners.
 */
public class GuiceBeanManager {

  @SuppressWarnings({"unchecked", "rawtypes" })
  public static BeanManager beanManager(final Promise<Registry> injector) {
    return Reflection.newProxy(BeanManager.class, (proxy, method, args) -> {
      final String name = method.getName();
      switch (name) {
        case "createAnnotatedType":
          return createAnnotatedType((Class) args[0]);
        case "createInjectionTarget":
          return createInjectionTarget(injector, ((AnnotatedType) args[0]).getJavaClass());
        case "createCreationalContext":
          return createCreationalContext();
        case "toString":
          return injector.toString();
        default:
          throw new UnsupportedOperationException(method.toString());
      }
    });
  }

  @SuppressWarnings("unchecked")
  private static <T> InjectionTarget<T> createInjectionTarget(
      final Promise<Registry> injector, final Class<T> type) {
    return Reflection.newProxy(InjectionTarget.class, (proxy, method, args) -> {
      final String name = method.getName();
      switch (name) {
        case "produce":
          return injector.future().get().require(type);
        case "inject":
          return null;
        case "postConstruct":
          return null;
        case "preDestroy":
          return null;
        case "dispose":
          return null;
        default:
          throw new UnsupportedOperationException(method.toString());
      }
    });
  }

  @SuppressWarnings("unchecked")
  private static <T> CreationalContext<T> createCreationalContext() {
    return Reflection.newProxy(CreationalContext.class, (proxy, method, args) -> {
      if (method.getName().equals("release")) {
        return null;
      }
      throw new UnsupportedOperationException(method.toString());
    });
  }

  @SuppressWarnings("unchecked")
  private static <T> AnnotatedType<T> createAnnotatedType(final Class<T> type) {
    return Reflection.newProxy(AnnotatedType.class, (proxy, method, args) -> {
      if (method.getName().equals("getJavaClass")) {
        return type;
      }
      throw new UnsupportedOperationException(method.toString());
    });
  }
}
