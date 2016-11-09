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
package org.jooby.internal.parser.bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.jooby.Request;
import org.jooby.internal.ParameterNameProvider;
import org.jooby.internal.mvc.RequestParam;
import org.jooby.internal.mvc.RequestParamNameProviderImpl;
import org.jooby.internal.mvc.RequestParamProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.inject.TypeLiteral;

import javaslang.CheckedFunction1;
import javaslang.Tuple;
import javaslang.Tuple2;

@SuppressWarnings("rawtypes")
public class BeanPlan {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Request.class);

  private Constructor<?> constructor;

  private List<RequestParam> parameters;

  private TypeLiteral beanType;

  private Map<Object, BeanPath> cache = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public BeanPlan(final ParameterNameProvider classInfo, final Class beanType) {
    this(classInfo, TypeLiteral.get(beanType));
  }

  public BeanPlan(final ParameterNameProvider classInfo, final TypeLiteral beanType) {
    Constructor<?> inject = null, def = null;
    Class rawType = beanType.getRawType();
    if (rawType == List.class) {
      rawType = ArrayList.class;
    }
    Constructor<?>[] cons = rawType.getDeclaredConstructors();
    if (cons.length == 1) {
      def = cons[0];
    } else {
      for (Constructor<?> c : cons) {
        if (c.isAnnotationPresent(Inject.class)) {
          if (inject != null) {
            throw new IllegalStateException(
                "Ambigous constructor found: " + rawType.getName()
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
      throw new IllegalStateException("Ambigous constructor found: " + rawType.getName()
          + ". Bean/Form type must have a no-args constructor or must be annotated with @"
          + Inject.class.getName());
    }
    this.beanType = beanType;
    this.constructor = constructor;
    this.parameters = new RequestParamProviderImpl(new RequestParamNameProviderImpl(classInfo))
        .parameters(constructor);
  }

  public Object newBean(final CheckedFunction1<RequestParam, Object> lookup,
      final Set<String> params) throws Throwable {
    log.debug("instantiating object {}", constructor);

    Object[] args = new Object[parameters.size()];
    List<String> names = new ArrayList<>(params);
    // remove constructor injected params
    for (int i = 0; i < args.length; i++) {
      RequestParam param = parameters.get(i);
      args[i] = lookup.apply(param);
      // skip constructor injected param (don't override)
      names.remove(param.name);
    }
    Object bean = constructor.newInstance(args);

    List<BeanPath> paths = compile(names.stream().sorted().iterator(), beanType);
    for (BeanPath path : paths) {
      String rawpath = path.toString();
      log.debug("  setting {}", rawpath);
      path.set(bean, lookup.apply(new RequestParam(path.setelem(), rawpath, path.settype())));
    }
    return bean;
  }

  private List<BeanPath> compile(final Iterator<String> it, final TypeLiteral beanType) {
    List<BeanPath> result = new ArrayList<>();
    while (it.hasNext()) {
      String path = it.next();
      Tuple2<TypeLiteral, String> ckey = Tuple.of(beanType, path);
      BeanPath cached = cache.get(ckey);
      if (cached == null) {
        List<Tuple2<String, Integer>> segments = segments(path);
        List<BeanPath> chain = new ArrayList<>();
        // traverse path
        TypeLiteral ittype = beanType;
        for (int i = 0; i < segments.size() - 1; i++) {
          Tuple2<String, Integer> segment = segments.get(i);
          final BeanPath cpath;
          if (segment._2 != null) {
            if (segment._1 == null) {
              cpath = new BeanIndexedPath(null, segment._2, ittype);
            } else {
              BeanPath getter = member("get", segment._1, ittype, 0);
              if (getter instanceof BeanMethodPath) {
                ((BeanMethodPath) getter).setter = member("set", segment._1, ittype, 1);
              }
              cpath = new BeanIndexedPath(getter, segment._2, ittype);
            }
          } else {
            BeanPath getter = member("get", segment._1, ittype, 0);
            if (getter instanceof BeanMethodPath) {
              ((BeanMethodPath) getter).setter = member("set", segment._1, ittype, 1);
            }
            cpath = getter;
          }
          if (cpath != null) {
            chain.add(cpath);
            ittype = TypeLiteral.get(cpath.type());
          }
        }

        // set path
        Tuple2<String, Integer> last = segments.get(segments.size() - 1);
        BeanPath cpath = member("set", last._1, ittype, 1);
        if (cpath != null) {
          if (last._2 != null) {
            BeanPath getter = member("get", last._1, ittype, 0);
            if (getter instanceof BeanMethodPath) {
              ((BeanMethodPath) getter).setter = cpath;
            }
            cpath = new BeanIndexedPath(getter, last._2, ittype);
          }
          if (chain.size() == 0) {
            cached = cpath;
          } else {
            cached = new BeanComplexPath(chain, cpath, path);
          }
          cache.put(ckey, cached);
        }
      }
      if (cached != null) {
        result.add(cached);
      }
    }
    return result;
  }

  private List<Tuple2<String, Integer>> segments(final String path) {
    List<String> segments = Splitter.on(CharMatcher.anyOf("[].")).trimResults()
        .omitEmptyStrings()
        .splitToList(path);
    List<Tuple2<String, Integer>> result = new ArrayList<>(segments.size());
    for (int i = 0; i < segments.size(); i++) {
      String segment = segments.get(i);
      try {
        int idx = Integer.parseInt(segment);
        if (result.size() > 0) {
          result.set(result.size() - 1, Tuple.of(result.get(result.size() - 1)._1, idx));
        } else {
          result.add(Tuple.of(null, idx));
        }
      } catch (NumberFormatException x) {
        result.add(Tuple.of(segment, null));
      }
    }

    return result;
  }

  private BeanPath member(final String prefix, final String name, final TypeLiteral root,
      final int pcount) {
    Class rawType = root.getRawType();
    BeanPath fn = method(prefix, name, rawType.getDeclaredMethods(), pcount);
    if (fn == null) {
      fn = field(name, rawType.getDeclaredFields());
      // superclass lookup?
      if (fn == null) {
        Class<?> superclass = rawType.getSuperclass();
        if (superclass != Object.class) {
          return member(prefix, name, TypeLiteral.get(rawType.getGenericSuperclass()), pcount);
        }
      }
    }
    return fn;
  }

  private BeanFieldPath field(final String name, final Field[] fields) {
    for (Field f : fields) {
      if (f.getName().equals(name)) {
        return new BeanFieldPath(name, f);
      }
    }
    return null;
  }

  private BeanMethodPath method(final String prefix, final String name, final Method[] methods,
      final int pcount) {
    String bname = javaBeanMethod(new StringBuilder(prefix), name);
    for (Method m : methods) {
      String mname = m.getName();
      if ((bname.equals(mname) || name.equals(mname)) && m.getParameterCount() == pcount) {
        return new BeanMethodPath(name, m);
      }
    }
    return null;
  }

  private String javaBeanMethod(final StringBuilder prefix, final String name) {
    return prefix.append(Character.toUpperCase(name.charAt(0))).append(name, 1, name.length())
        .toString();
  }
}
