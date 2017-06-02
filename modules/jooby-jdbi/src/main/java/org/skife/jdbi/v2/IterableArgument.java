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
package org.skife.jdbi.v2;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.jooby.jdbi.Jdbi;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class IterableArgument implements Argument {

  private Iterable<?> values;

  private Foreman foreman;

  @SuppressWarnings({"unchecked", "rawtypes" })
  public IterableArgument(final Object value, final StatementContext ctx) {
    if (value.getClass().isArray()) {
      ImmutableList.Builder<Object> builder = ImmutableList.builder();
      for (int i = 0; i < Array.getLength(value); i++) {
        builder.add(Array.get(value, i));
      }
      this.values = builder.build();
    } else {
      this.values = (Iterable<?>) value;
    }
    this.foreman = new Foreman();
    List<ArgumentFactory> factories = (List<ArgumentFactory>) ctx.getAttribute(Jdbi.ARG_FACTORIES);
    factories.forEach(foreman::register);
  }

  @Override
  public void apply(final int position, final PreparedStatement stmt,
      final StatementContext ctx) throws SQLException {
    int i = position;
    for (Object value : values) {
      foreman.createArgument(value.getClass(), value, ctx).apply(i, stmt, ctx);
      i += 1;
    }

    ctx.setAttribute("position", i - 1);

  }

  public int size() {
    return Iterables.size(values);
  }

}
