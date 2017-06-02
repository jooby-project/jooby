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
package org.jooby.internal.couchbase;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import org.jooby.couchbase.GeneratedValue;

import com.couchbase.client.java.repository.annotation.Id;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import javaslang.control.Try;

@SuppressWarnings("rawtypes")
public final class IdGenerator {

  private static LoadingCache<Class, Field> CACHE = CacheBuilder.newBuilder()
      .build(new CacheLoader<Class, Field>() {
        @Override
        public Field load(final Class key) throws Exception {
          if (key == Object.class) {
            throw new IllegalArgumentException("Entity class: " + key.getName()
                + " must have an 'id' field or a field annotated with @" + Id.class.getName());
          }
          Field[] fields = key.getDeclaredFields();
          for (Field field : fields) {
            if (field.getName().equals("id")) {
              field.setAccessible(true);
              return field;
            } else if (field.getAnnotation(Id.class) != null) {
              field.setAccessible(true);
              return field;
            }
          }
          return load(key.getSuperclass());
        }
      });

  public static String getIdName(final Object bean) {
    return field(bean).getName();
  }

  public static Object getOrGenId(final Object bean, final Supplier<Long> next) {
    Field fid = field(bean);
    Object id = getId(bean, fid);
    if (id == null && fid.isAnnotationPresent(GeneratedValue.class)) {
      if (fid.getType() != Long.class) {
        throw new IllegalArgumentException("Generated value must be of type Long: " + fid);
      }
      id = Try.of(() -> {
        Long seq = next.get();
        fid.set(bean, seq);
        return seq;
      }).getOrElseThrow(x -> new IllegalStateException("Can't generate id for: " + fid, x));

    }
    return id;
  }

  public static Object getId(final Object bean) {
    Field id = field(bean);
    return getId(bean, id);
  }

  private static Field field(final Object bean) {
    Field id = Try.of(() -> CACHE.getUnchecked(bean.getClass()))
        .getOrElseThrow(x -> Throwables.propagate(((UncheckedExecutionException) x).getCause()));
    return id;
  }

  private static Object getId(final Object bean, final Field id) {
    return Try.of(() -> id.get(bean)).get();
  }

}
