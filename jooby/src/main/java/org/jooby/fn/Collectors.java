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
package org.jooby.fn;

import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public final class Collectors {

  public static <T> Collector<T, ImmutableList.Builder<T>, List<T>> toList() {
    BinaryOperator<ImmutableList.Builder<T>> combiner = (l, r) -> l.addAll(r.build());
    return Collector.of(ImmutableList.Builder::new, (b, e) -> b.add(e), combiner,
        ImmutableList.Builder::build);
  }

  public static <T> Collector<T, ImmutableSet.Builder<T>, Set<T>> toSet() {
    BinaryOperator<ImmutableSet.Builder<T>> combiner = (l, r) -> l.addAll(r.build());
    return Collector.of(ImmutableSet.Builder::new, (b, e) -> b.add(e), combiner,
        ImmutableSet.Builder::build);
  }

}
