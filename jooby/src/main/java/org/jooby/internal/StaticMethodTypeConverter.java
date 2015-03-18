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

import org.jooby.internal.reqparam.StaticMethodParamConverter;

import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.TypeConverter;

class StaticMethodTypeConverter<T> extends AbstractMatcher<TypeLiteral<T>>
    implements TypeConverter {

  private StaticMethodParamConverter converter;

  public StaticMethodTypeConverter(final String name) {
    converter = new StaticMethodParamConverter(name);
  }

  @Override
  public Object convert(final String value, final TypeLiteral<?> type) {
    try {
      return converter.convert(type, new Object[]{value }, null);
    } catch (Exception ex) {
      throw new IllegalStateException("Can't convert: " + value + " to " + type, ex);
    }
  }

  @Override
  public boolean matches(final TypeLiteral<T> type) {
    return !Enum.class.isAssignableFrom(type.getRawType())
        && converter.matches(type);
  }

  @Override
  public String toString() {
    return converter.toString();
  }

}
