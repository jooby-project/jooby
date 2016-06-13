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

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.jooby.Err;
import org.jooby.Status;

import com.typesafe.config.Config;

public class StatusCodeProvider {

  private Config conf;

  @Inject
  public StatusCodeProvider(final Config conf) {
    this.conf = conf;
  }

  public Status apply(final Throwable cause) {
    if (cause instanceof Err) {
      return Status.valueOf(((Err) cause).statusCode());
    }
    /**
     * usually a class name, except for inner classes where '$' is replaced it by '.'
     */
    Function<Class<?>, String> name = type -> Optional.ofNullable(type.getDeclaringClass())
        .map(dc -> new StringBuilder(dc.getName())
            .append('.')
            .append(type.getSimpleName())
            .toString())
        .orElse(type.getName());

    Config err = conf.getConfig("err");
    int status = -1;
    Class<?> type = cause.getClass();
    while (type != Throwable.class && status == -1) {
      String classname = name.apply(type);
      if (err.hasPath(classname)) {
        status = err.getInt(classname);
      } else {
        type = type.getSuperclass();
      }
    }
    return status == -1 ? Status.SERVER_ERROR : Status.valueOf(status);
  }
}
