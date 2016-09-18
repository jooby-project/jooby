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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.inject.Inject;

import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.internal.ParameterNameProvider;
import org.jooby.internal.mvc.RequestParam;
import org.jooby.internal.mvc.RequestParamNameProviderImpl;
import org.jooby.internal.mvc.RequestParamProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import javaslang.CheckedFunction1;
import javaslang.Tuple;
import javaslang.Tuple4;

public class BeanPlan {

  static abstract class BeanMemberRef<M extends Member> {

    protected final M member;

    public BeanMemberRef(final M e) {
      this.member = e;
    }

    @SuppressWarnings("rawtypes")
    public abstract Class type();

    public abstract Type gtype();

    public abstract Object set(Object src, Object value) throws Exception;

    public abstract Object get(Object src) throws Exception;
  }

  static class BeanMethod extends BeanMemberRef<Method> {

    public BeanMethod(final Method m) {
      super(m);
      m.setAccessible(true);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class type() {
      return member.getReturnType();
    }

    @Override
    public Type gtype() {
      return member.getParameters()[0].getParameterizedType();
    }

    @Override
    public Object set(final Object src, final Object value) throws Exception {
      return member.invoke(src, value);
    }

    @Override
    public Object get(final Object src) throws Exception {
      return member.invoke(src);
    }

  }

  static class BeanField extends BeanMemberRef<Field> {

    public BeanField(final Field f) {
      super(f);
      f.setAccessible(true);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class type() {
      return member.getType();
    }

    @Override
    public Type gtype() {
      return member.getGenericType();
    }

    @Override
    public Object set(final Object src, final Object value) throws Exception {
      member.set(src, value);
      return value;
    }

    @Override
    public Object get(final Object src) throws Exception {
      return member.get(src);
    }

  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Request.class);

  private Constructor<?> constructor;

  private List<RequestParam> parameters;

  @SuppressWarnings("rawtypes")
  private LoadingCache<Tuple4<Class, String, String, Integer>, BeanMemberRef> cache = CacheBuilder
      .newBuilder()
      .build(new CacheLoader<Tuple4<Class, String, String, Integer>, BeanMemberRef>() {

        @Override
        public BeanMemberRef load(final Tuple4<Class, String, String, Integer> key)
            throws Exception {
          BeanMemberRef member = member(key._2, key._3, key._1, key._4);
          if (member == null) {
            throw new NoSuchElementException(key._1 + "." + key._3);
          }
          return member;
        }

      });

  public BeanPlan(final ParameterNameProvider classInfo, final Class<?> beanType) {
    Constructor<?> inject = null, def = null;
    Constructor<?>[] cons = beanType.getDeclaredConstructors();
    if (cons.length == 1) {
      def = cons[0];
    } else {
      for (Constructor<?> c : cons) {
        if (c.isAnnotationPresent(Inject.class)) {
          if (inject != null) {
            throw new IllegalStateException(
                "Ambigous constructor found: " + beanType.getName()
                    + ". Only one @" + Inject.class.getName() + " allowed");
          }
          inject = c;
        } else if (c.getParameterCount() == 0) {
          def = c;
        }
      }
    }
    Constructor<?> constructor = inject == null ? def : inject;
    if (constructor == null) {
      throw new IllegalStateException("Ambigous constructor found: " + beanType.getName()
          + ". Bean/Form type must have a no-args constructor or must be annotated with @"
          + Inject.class.getName());
    }
    this.constructor = constructor;
    this.parameters = new RequestParamProviderImpl(new RequestParamNameProviderImpl(classInfo))
        .parameters(constructor);
  }

  public Object newBean(final CheckedFunction1<RequestParam, Object> lookup,
      final Map<String, Mutant> params) throws Throwable {
    return newBean(lookup, params.keySet());
  }

  @SuppressWarnings("rawtypes")
  public Object newBean(final CheckedFunction1<RequestParam, Object> lookup,
      final Set<String> params) throws Throwable {
    Object[] args = new Object[parameters.size()];
    // remove constructor injected params
    Set<String> names = new HashSet<>(params);
    for (int i = 0; i < args.length; i++) {
      RequestParam param = parameters.get(i);
      args[i] = lookup.apply(param);
      names.remove(param.name);
    }
    log.debug("instantiating object {}", constructor);
    Object bean = constructor.newInstance(args);

    for (String name : names) {
      List<String> path = name(name);
      Object root = seek(bean, path);
      String tail = path.get(path.size() - 1);
      try {
        BeanMemberRef member = ref(root.getClass(), "set", tail, 1);
        member.set(root,
            lookup.apply(new RequestParam((AnnotatedElement) member.member, name, member.gtype())));
      } catch (NoSuchElementException x) {
        log.debug("Method/Field not found: {}", x.getMessage());
      }
    }
    return bean;
  }

  @SuppressWarnings("rawtypes")
  private Object seek(final Object bean, final List<String> expression) throws Throwable {
    Object it = bean;
    for (int i = 0; i < expression.size() - 1; i++) {
      BeanMemberRef ref = ref(it.getClass(), "get", expression.get(i), 0);
      Object next = ref.get(it);
      if (next == null) {
        // TODO: should we create a new bean plan? What about recursive object (self-reference)?
        next = ref.type().newInstance();
        ref(it.getClass(), "set", expression.get(i), 1).set(it, next);
      }
      it = next;
    }
    return it;
  }

  @SuppressWarnings("rawtypes")
  private BeanMemberRef ref(final Class type, final String prefix, final String name,
      final int paramCount) throws Throwable {
    try {
      return cache.getUnchecked(Tuple.of(type, prefix, name, paramCount));
    } catch (UncheckedExecutionException x) {
      throw x.getCause();
    }
  }

  @SuppressWarnings("rawtypes")
  private BeanMemberRef member(final String prefix, final String name, final Class<?> type,
      final int pcount) {
    BeanMemberRef fn = method(prefix, name, type.getDeclaredMethods(), pcount);
    if (fn == null) {
      fn = field(name, type.getDeclaredFields());
      // superclass lookup?
      if (fn == null) {
        Class<?> superclass = type.getSuperclass();
        if (superclass != Object.class) {
          return member(prefix, name, superclass, pcount);
        }
      }
    }
    return fn;
  }

  @SuppressWarnings("rawtypes")
  private BeanMemberRef field(final String name, final Field[] fields) {
    for (Field f : fields) {
      if (f.getName().equals(name)) {
        return new BeanField(f);
      }
    }
    return null;
  }

  @SuppressWarnings("rawtypes")
  private BeanMemberRef method(final String prefix, final String name, final Method[] methods,
      final int pcount) {
    String setter = javaBeanMethod(new StringBuilder(prefix), name);
    for (Method m : methods) {
      String mname = m.getName();
      if ((setter.equals(mname) || name.equals(mname)) && m.getParameterCount() == pcount) {
        return new BeanMethod(m);
      }
    }
    return null;
  }

  private static List<String> name(final String name) {
    return Splitter.on(new CharMatcher() {
      @Override
      public boolean matches(final char c) {
        return c == '[' || c == ']';
      }
    }).trimResults()
        .omitEmptyStrings()
        .splitToList(name);
  }

  private String javaBeanMethod(final StringBuilder prefix, final String name) {
    return prefix.append(Character.toUpperCase(name.charAt(0))).append(name, 1, name.length())
        .toString();
  }

}
