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
package org.jooby.crash;

import static java.util.Arrays.asList;
import static javaslang.Tuple.of;
import static org.jooby.crash.CrashFSDriver.endsWith;
import static org.jooby.crash.CrashFSDriver.noneOf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.crsh.plugin.CRaSHPlugin;
import org.crsh.plugin.PluginContext;
import org.crsh.plugin.PluginLifeCycle;
import org.crsh.vfs.FS;

import javaslang.control.Try;

class CrashBootstrap extends PluginLifeCycle {

  private static final Predicate<Path> ACCEPT = endsWith(".class").negate();

  private List<CrashFSDriver> drivers = new ArrayList<>();

  public PluginContext start(final ClassLoader loader, final Properties props,
      final Map<String, Object> attributes, final Set<CRaSHPlugin<?>> plugins) throws IOException {
    FS conffs = newFS(CrashFSDriver.parse(loader, asList(
        of("crash", ACCEPT))));

    FS cmdfs = newFS(CrashFSDriver.parse(loader, asList(
        of("cmd", ACCEPT),
        of("org/jooby/crash", ACCEPT),
        of("crash/commands", noneOf("jndi.groovy", "jdbc.groovy", "jpa.groovy", "jul.groovy")))));

    setConfig(props);

    PluginContext ctx = new PluginContext(executor("crash"), scanner("crash-scanner"),
        () -> plugins, attributes, cmdfs, conffs, loader);

    ctx.refresh();

    start(ctx);

    return ctx;
  }

  public void shutdown() {
    drivers.forEach(it -> Try.run(it::close));
    super.stop();
  }

  private FS newFS(final List<CrashFSDriver> drivers) throws IOException {
    FS fs = new FS();
    for (CrashFSDriver driver : drivers) {
      fs.mount(driver);
    }
    this.drivers.addAll(drivers);
    return fs;
  }

  private static ScheduledExecutorService scanner(final String name) {
    return Executors.newScheduledThreadPool(1, r -> {
      Thread thread = Executors.defaultThreadFactory().newThread(r);
      thread.setName(name);
      return thread;
    });
  }

  private static ExecutorService executor(final String name) {
    AtomicInteger next = new AtomicInteger(0);
    return new ThreadPoolExecutor(0, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
        r -> {
          Thread thread = Executors.defaultThreadFactory().newThread(r);
          thread.setName(name + "-" + next.incrementAndGet());
          return thread;
        });
  }

}
