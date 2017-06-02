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
package org.jooby.internal.aws;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javaslang.control.Try.CheckedRunnable;

public class AwsShutdownSupport implements CheckedRunnable {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Object dep;

  public AwsShutdownSupport(final Object dep) {
    this.dep = dep;
  }

  @Override
  public void run() throws Throwable {
    if (dep == null) {
      return;
    }
    try {
      Optional<Method> shutdown = Arrays.stream(dep.getClass().getMethods())
          .filter(m -> m.getName().startsWith("shutdown")
              && m.getParameterCount() == 0
              && Modifier.isPublic(m.getModifiers()))
          .findFirst();
      if (shutdown.isPresent()) {
        log.debug("stopping {}", dep);
        shutdown.get().invoke(dep);
      } else {
        log.debug("no shutdown method found for: {}", dep);
      }
    } catch (InvocationTargetException ex) {
      throw Match(ex.getTargetException()).of(
          Case(instanceOf(Exception.class), x -> x),
          Case($(), x -> new IllegalStateException("shutdown result in error", x)));
    } finally {
      dep = null;
    }
  }

}
