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
package org.jooby.rx;

import java.util.Map;
import java.util.concurrent.Executor;

import com.google.common.collect.ImmutableMap;

import javaslang.Lazy;
import rx.Scheduler;
import rx.plugins.RxJavaSchedulersHook;
import rx.schedulers.Schedulers;

class ExecSchedulerHook extends RxJavaSchedulersHook {

  private Lazy<Map<String, Scheduler>> schedulers;

  public ExecSchedulerHook(final Map<String, Executor> executors) {
    // we don't want eager initialization of Schedulers
    this.schedulers = Lazy.of(() -> {
      ImmutableMap.Builder<String, Scheduler> schedulers = ImmutableMap.builder();
      executors.forEach((k, e) -> schedulers.put(k, Schedulers.from(e)));
      return schedulers.build();
    });
  }

  @Override
  public Scheduler getComputationScheduler() {
    return schedulers.get().get("computation");
  }

  @Override
  public Scheduler getIOScheduler() {
    return schedulers.get().get("io");
  }

  @Override
  public Scheduler getNewThreadScheduler() {
    return schedulers.get().get("newThread");
  }

}
