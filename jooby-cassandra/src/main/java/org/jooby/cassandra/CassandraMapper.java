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
package org.jooby.cassandra;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import org.jooby.Deferred;
import org.jooby.Route.Mapper;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

class CassandraMapper implements Mapper<Object> {

  @SuppressWarnings({"unchecked", "rawtypes" })
  private static class DeferredHandler implements Deferred.Initializer0 {

    private ListenableFuture future;

    public DeferredHandler(final ListenableFuture future) {
      this.future = future;
    }

    @Override
    public void run(final Deferred deferred) throws Exception {
      Futures.addCallback(future, new FutureCallback() {
        @Override
        public void onSuccess(final Object result) {
          deferred.resolve(resultSet(result));
        }

        @Override
        public void onFailure(final Throwable x) {
          deferred.reject(x);
        }
      });
    }
  }

  @Override
  public String name() {
    return "cassandra";
  }

  @Override
  public Object map(final Object value) throws Throwable {
    return Match(value).of(
        Case(instanceOf(ListenableFuture.class),
            future -> new Deferred(new DeferredHandler(future))),
        Case($(), resultSet(value)));
  }

  private static Object resultSet(final Object result) {
    return Match(result).of(
        Case(instanceOf(ResultSet.class), ResultSet::all),
        Case(instanceOf(Result.class), Result::all),
        Case($(), result));
  }

}
