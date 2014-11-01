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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.jooby.Err;
import org.jooby.Status;
import org.jooby.Upload;
import org.jooby.Mutant;

import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;

public class UploadMutant implements Mutant {

  private String name;

  private List<Upload> uploads;

  public UploadMutant(final String name, final List<Upload> uploads) {
    this.name = name;
    this.uploads = uploads;
  }

  @Override
  public boolean isPresent() {
    return true;
  }

  @Override
  public boolean booleanValue() {
    throw typeError(boolean.class);
  }

  @Override
  public byte byteValue() {
    throw typeError(byte.class);
  }

  @Override
  public short shortValue() {
    throw typeError(short.class);
  }

  @Override
  public int intValue() {
    throw typeError(int.class);
  }

  @Override
  public long longValue() {
    throw typeError(long.class);
  }

  @Override
  public String stringValue() {
    throw typeError(String.class);
  }

  @Override
  public float floatValue() {
    throw typeError(float.class);
  }

  @Override
  public double doubleValue() {
    throw typeError(double.class);
  }

  @Override
  public <T extends Enum<T>> T enumValue(final Class<T> type) {
    throw typeError(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> toList(final Class<T> type) {
    if (type == Upload.class) {
      return (List<T>) uploads;
    }
    throw typeError(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Set<T> toSet(final Class<T> type) {
    if (type == Upload.class) {
      return (Set<T>) ImmutableSet.copyOf(uploads);
    }
    throw typeError(type);
  }

  @Override
  public <T extends Comparable<T>> SortedSet<T> toSortedSet(final Class<T> type) {
    throw typeError(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> toOptional(final Class<T> type) {
    if (type == Upload.class) {
      return (Optional<T>) Optional.of(uploads.get(0));
    }
    throw typeError(type);
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public <T> T to(final TypeLiteral<T> type) {
    Class<? super T> rawType = type.getRawType();
    if (rawType == Upload.class) {
      return (T) uploads.get(0);
    }
    if (rawType == Optional.class) {
      return (T) toOptional(classFrom(type));
    }
    if (rawType == List.class) {
      return (T) toList(classFrom(type));
    }
    if (rawType == Set.class) {
      return (T) toSet(classFrom(type));
    }
    if (rawType == SortedSet.class) {
      return (T) toSortedSet((Class) classFrom(type));
    }
    throw typeError(rawType);
  }

  private static Class<?> classFrom(final TypeLiteral<?> type) {
    return classFrom(type.getType());
  }

  private static Class<?> classFrom(final Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type actualType = parameterizedType.getActualTypeArguments()[0];
      return classFrom(actualType);
    }
    throw new Err(Status.BAD_REQUEST, "Unknown type: " + type);
  }

  private Err typeError(final Class<?> type) {
    return new Err(Status.BAD_REQUEST, "Can't convert to " + name + " to " + type);
  }
}
