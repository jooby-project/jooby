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
package org.jooby.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Provider;

import org.jooby.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.primitives.Primitives;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class LifecycleProcessor implements TypeListener {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public <I> void hear(final TypeLiteral<I> type, final TypeEncounter<I> encounter) {
    Class<?> rawType = type.getRawType();
    if (Managed.class.isAssignableFrom(rawType)) {
      encounter.register((InjectionListener<I>) injectee -> {
        try {
          log.debug("starting: {}", rawType.getName());
          ((Managed) injectee).start();
        } catch (Exception ex) {
          Throwables.propagateIfPossible(ex);
          throw new IllegalStateException(rawType.getName() + ".start() resulted in error: ", ex);
        }
      });
    } else {
      // hard way
      Method method = findMethod(rawType, PostConstruct.class);
      if (method != null) {
        encounter.register((InjectionListener<I>) injectee -> {
          try {
            log.debug("starting: {}", method);
            method.invoke(injectee);
          } catch (Exception ex) {
            Throwable cause = ex;
            if (cause instanceof InvocationTargetException) {
              // override cause
            cause = ((InvocationTargetException) cause).getTargetException();
          }
          Throwables.propagateIfPossible(cause);
          throw new IllegalStateException(rawType.getName() + "." + method.getName()
              + "() resulted in error: ", cause);
        }
      });
      }
    }
  }

  public static void onPreDestroy(final Injector injector, final Logger log) {
    Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();
    for (Binding<?> binding : bindings.values()) {
      if (Scopes.isSingleton(binding)) {
        Class<?> rawType = binding.getKey().getTypeLiteral().getRawType();
        if (includes(rawType)) {
          Object target = findManaged(binding);
          if (target instanceof Managed) {
            try {
              log.debug("stopping: {}", rawType.getName());
              ((Managed) target).stop();
            } catch (Exception ex) {
              log.error(rawType.getName() + ".stop() resulted in error", ex);
            }
          } else {
            // hard way
            Method method = findMethod(rawType, PreDestroy.class);
            if (method != null) {
              try {
                log.debug("stopping: {}", rawType.getName());
                method.invoke(target);
              } catch (Exception ex) {
                Throwable cause = ex;
                if (cause instanceof InvocationTargetException) {
                  // override cause
                  cause = ((InvocationTargetException) cause).getTargetException();
                }
                log.error(rawType.getName() + "." + method.getName() + "() resulted in error",
                    cause);
              }
            }
          }
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private static Object findManaged(final Binding<?> binding) {
    if (binding instanceof ProviderInstanceBinding) {
      ProviderInstanceBinding pib = (ProviderInstanceBinding) binding;
      Provider provider = pib.getUserSuppliedProvider();
      if (provider instanceof Managed) {
        return provider;
      }
    }
    com.google.inject.Provider<?> provider = binding.getProvider();
    return provider.get();
  }

  private static boolean includes(final Class<?> rawType) {
    if (rawType == String.class || rawType.isPrimitive() || Primitives.isWrapperType(rawType)) {
      return false;
    }
    return true;
  }

  private static Method findMethod(final Class<?> rawType,
      final Class<? extends Annotation> annotation) {
    for (Method method : rawType.getDeclaredMethods()) {
      if (method.getAnnotation(annotation) != null) {
        int mods = method.getModifiers();
        if (Modifier.isStatic(mods)) {
          throw new IllegalArgumentException(annotation.getSimpleName()
              + " method should not be static: " + method);
        }
        if (!Modifier.isPublic(mods)) {
          throw new IllegalArgumentException(annotation.getSimpleName()
              + " method must be public: " + method);
        }
        if (method.getParameterCount() > 0) {
          throw new IllegalArgumentException(annotation.getSimpleName()
              + " method should not accept arguments: " + method);
        }
        if (method.getReturnType() != void.class) {
          throw new IllegalArgumentException(annotation.getSimpleName()
              + " method should not return anything: " + method);
        }
        return method;
      }
    }
    return null;
  }
}
