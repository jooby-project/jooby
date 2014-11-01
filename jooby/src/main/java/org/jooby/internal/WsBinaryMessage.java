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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.jooby.Err;
import org.jooby.Status;
import org.jooby.Mutant;

import com.google.common.base.Charsets;
import com.google.inject.TypeLiteral;

public class WsBinaryMessage implements Mutant {

  private ByteBuffer buffer;

  public WsBinaryMessage(final ByteBuffer buffer) {
    this.buffer = buffer;
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

  @Override
  public <T> List<T> toList(final Class<T> type) {
    throw typeError(type);
  }

  @Override
  public <T> Set<T> toSet(final Class<T> type) {
    throw typeError(type);
  }

  @Override
  public <T extends Comparable<T>> SortedSet<T> toSortedSet(final Class<T> type) {
    throw typeError(type);
  }

  @Override
  public <T> Optional<T> toOptional(final Class<T> type) {
    throw typeError(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T to(final TypeLiteral<T> type) {
    Class<? super T> rawType = type.getRawType();
    if (rawType == byte[].class) {
      return (T) buffer.array();
    }
    if (rawType == ByteBuffer.class) {
      return (T) buffer;
    }
    if (rawType == InputStream.class) {
      return (T) new ByteArrayInputStream(buffer.array());
    }
    if (rawType == Reader.class) {
      return (T) new InputStreamReader(new ByteArrayInputStream(buffer.array()), Charsets.UTF_8);
    }
    throw typeError(rawType);
  }

  private Err typeError(final Class<?> type) {
    return new Err(Status.BAD_REQUEST, "Can't convert to "
        + ByteBuffer.class.getName() + " to " + type);
  }
}
