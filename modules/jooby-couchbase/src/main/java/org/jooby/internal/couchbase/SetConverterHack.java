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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.repository.AsyncRepository;

import javaslang.CheckedFunction1;
import javaslang.control.Try;

public final class SetConverterHack {

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(SetConverterHack.class);

  private static final CheckedFunction1<Class<?>, Field> FIELD = CheckedFunction1
      .<Class<?>, Field> of(c -> {
        Field fld = c.getDeclaredField("converter");
        fld.setAccessible(true);
        return fld;
      }).memoized();

  public static void forceConverter(final AsyncRepository repo, final JacksonMapper converter) {
    Try.run(() -> FIELD.apply(repo.getClass()).set(repo, converter))
        .onFailure(x -> log.warn("Set converter resulted in error", x));
  }
}
