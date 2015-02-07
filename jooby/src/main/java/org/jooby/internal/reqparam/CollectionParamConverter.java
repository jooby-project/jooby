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

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;

import org.jooby.ParamConverter;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableCollection.Builder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.TypeLiteral;

public class CollectionParamConverter implements ParamConverter {

  private final Map<Class<?>, Supplier<ImmutableCollection.Builder<?>>> parsers =
      ImmutableMap.<Class<?>, Supplier<ImmutableCollection.Builder<?>>> builder()
          .put(List.class, ImmutableList.Builder::new)
          .put(Set.class, ImmutableSet.Builder::new)
          .put(SortedSet.class, ImmutableSortedSet::naturalOrder)
          .build();

  private boolean matches(final TypeLiteral<?> toType) {
    return parsers.containsKey(toType.getRawType())
        && toType.getType() instanceof ParameterizedType;
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Context ctx)
      throws Exception {
    if (matches(toType)) {
      Builder builder = parsers.get(toType.getRawType()).get();
      if (values != null) {
        TypeLiteral<?> paramType = TypeLiteral.get(((ParameterizedType) toType.getType())
            .getActualTypeArguments()[0]);
        for (Object value : values) {
          builder.add(ctx.convert(paramType, new Object[]{value }));
        }
      }
      return builder.build();
    } else {
      return ctx.convert(toType, values);
    }
  }

}
