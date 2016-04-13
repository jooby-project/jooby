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
package org.jooby.exec;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.jooby.Env;
import org.jooby.Jooby.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.typesafe.config.ConfigValueType;

import javaslang.Function4;
import javaslang.control.Try;

/**
 * <h1>executor</h1>
 * <p>
 * Manage the life cycle of {@link ExecutorService} and build async apps, schedule tasks, etc...
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * ...
 * import org.jooby.exec.Exec;
 * ...
 *
 * {
 *   use(new Exec());
 *
 *   get("/", req -> {
 *     ExecutorService executor = req.require(ExecutorService.class);
 *     // work with executor
 *   });
 * }
 * }</pre>
 *
 * <p>
 * The default executor is a {@link Executors#newFixedThreadPool(int)} with threads defined by
 * {@link Runtime#availableProcessors()}
 * </p>
 *
 * <h2>explicit creation</h2>
 * <p>
 * The default {@link ExecutorService} is nice and give you something that just works out of the
 * box. But, what if you need to control the number of threads?
 * </p>
 * <p>
 * Explicit control is provided via <code>executors</code> which allow the following syntax:
 * </p>
 *
 * <pre>
 * type (= int)? (, daemon (= boolean)? )? (, priority (= int)? )?
 * </pre>
 *
 * <p>
 * Let's see some examples:
 * </p>
 *
 * <pre>
 * # fixed thread pool with a max number of threads equals to the available runtime processors
 * executors = "fixed"
 * </pre>
 *
 * <pre>
 * # fixed thread pool with a max number of 10 threads
 * executors = "fixed = 10"
 * </pre>
 *
 * <pre>
 * # fixed thread pool with a max number of 10 threads
 * executors = "fixed = 10"
 * </pre>
 *
 * <pre>
 * # scheduled thread pool with a max number of 10 threads
 * executors = "scheduled = 10"
 * </pre>
 *
 * <pre>
 * # cached thread pool with daemon threads and max priority
 * executors = "cached, daemon = true, priority = 10"
 * </pre>
 *
 * <pre>
 * # forkjoin thread pool with asyncMode
 * executors = "forkjoin, asyncMode = true"
 * </pre>
 *
 * <h2>multiple executors</h2>
 * <p>
 * Multiple executors are provided by expanding the <code>executors</code> properties, like:
 * </p>
 *
 * <pre>
 *  executors {
 *    pool1: fixed
 *    jobs: forkjoin
 *  }
 * </pre>
 *
 * <p>
 * Later, you can request your executor like:
 * </p>
 * <pre>{@code
 * {
 *   get("/", req -> {
 *     ExecutorService pool1 = req.require("pool1", ExecutorService.class);
 *     ExecutorService jobs = req.require("jobs", ExecutorService.class);
 *   });
 * }
 * }</pre>
 *
 * <h2>shutdown</h2>
 * <p>
 * Any {@link ExecutorService} created by this module will automatically shutdown on application
 * shutdown time.
 * </p>
 *
 * @author edgar
 * @since 0.16.0
 */
public class Exec implements Module {

  private static final BiConsumer<String, Executor> NOOP = (n, e) -> {
  };

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private boolean daemon = true;

  private int priority = Thread.NORM_PRIORITY;

  private Map<String, Function4<String, Integer, Supplier<ThreadFactory>, Map<String, Object>, ExecutorService>> f =
      /** executor factory. */
      ImmutableMap
          .of(
              "cached", (name, n, tf, opts) -> Executors.newCachedThreadPool(tf.get()),
              "fixed", (name, n, tf, opts) -> Executors.newFixedThreadPool(n, tf.get()),
              "scheduled", (name, n, tf, opts) -> Executors.newScheduledThreadPool(n, tf.get()),
              "forkjoin", (name, n, tf, opts) -> {
                boolean asyncMode = Boolean.parseBoolean(opts.getOrDefault("asyncMode", "false")
                    .toString());
                return new ForkJoinPool(n, fjwtf(name), null, asyncMode);
              });

  private String namespace;

  protected Exec(final String namespace) {
    this.namespace = namespace;
  }

  public Exec() {
    this("executors");
  }

  /**
   * Defined the default value for daemon. This value is used when a executor spec doesn't define a
   * value for daemon. Default is: <code>true</code>
   *
   * @param daemon True for default daemon.
   * @return This module.
   */
  public Exec daemon(final boolean daemon) {
    this.daemon = daemon;
    return this;
  }

  /**
   * Defined the default value for priority. This value is used when a executor spec doesn't define
   * a value for priority. Default is: {@link Thread#NORM_PRIORITY}.
   *
   * @param priority One of {@link Thread#MIN_PRIORITY}, {@link Thread#NORM_PRIORITY} or
   *        {@link Thread#MAX_PRIORITY}.
   * @return This module.
   */
  public Exec priority(final int priority) {
    this.priority = priority;
    return this;
  }

  @Override
  public Config config() {
    return ConfigFactory.empty("exec.conf").withValue(namespace,
        ConfigValueFactory.fromAnyRef("fixed"));
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    configure(env, conf, binder, NOOP);
  }

  protected void configure(final Env env, final Config conf, final Binder binder,
      final BiConsumer<String, Executor> callback) {
    List<Map<String, Object>> executors = conf.hasPath(namespace)
        ? executors(conf.getValue(namespace), daemon, priority,
            Runtime.getRuntime().availableProcessors())
        : Collections.emptyList();
    List<Entry<String, ExecutorService>> services = new ArrayList<>(executors.size());
    for (Map<String, Object> options : executors) {
      // thread factory options
      String name = (String) options.remove("name");
      log.debug("found executor: {}{}", name, options);
      Boolean daemon = (Boolean) options.remove("daemon");
      Integer priority = (Integer) options.remove("priority");
      String type = String.valueOf(options.remove("type"));
      // number of processors
      Integer n = (Integer) options.remove(type);

      // create executor
      Function4<String, Integer, Supplier<ThreadFactory>, Map<String, Object>, ExecutorService> factory = f
          .get(type);
      if (factory == null) {
        throw new IllegalArgumentException(
            "Unknown executor: " + type + " must be one of " + f.keySet());
      }
      ExecutorService executor = factory.apply(type, n, () -> factory(name, daemon, priority),
          options);

      bind(binder, name, executor);
      callback.accept(name, executor);

      services.add(Maps.immutableEntry(name, executor));
    }

    services.stream()
        .filter(it -> it.getKey().equals("default"))
        .findFirst()
        .ifPresent(e -> {
          bind(binder, null, e.getValue());
        });

    env.onStop(() -> {
      services.forEach(exec -> Try.run(() -> exec.getValue().shutdown()).onFailure(cause -> {
        log.error("shutdown of {} resulted in error", exec.getKey(), cause);
      }));
      services.clear();
    });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private static void bind(final Binder binder, final String name, final ExecutorService executor) {
    Class klass = executor.getClass();

    Set<Class> types = collector(klass);
    for (Class type : types) {
      Key key = name == null ? Key.get(type) : Key.get(type, Names.named(name));
      binder.bind(key).toInstance(executor);
    }
  }

  @SuppressWarnings("rawtypes")
  private static Set<Class> collector(final Class type) {
    if (type != null && Executor.class.isAssignableFrom(type)) {
      Set<Class> types = new HashSet<>();
      if (type.isInterface() || !Modifier.isAbstract(type.getModifiers())) {
        types.add(type);
      }
      types.addAll(collector(type.getSuperclass()));
      Arrays.asList(type.getInterfaces()).forEach(it -> types.addAll(collector(it)));
      return types;
    }
    return Collections.emptySet();
  }

  private static ThreadFactory factory(final String name, final boolean daemon,
      final int priority) {
    AtomicLong id = new AtomicLong(0);
    return r -> {
      Thread thread = new Thread(r, name + "-" + id.incrementAndGet());
      thread.setDaemon(daemon);
      thread.setPriority(priority);
      return thread;
    };
  }

  private static ForkJoinWorkerThreadFactory fjwtf(final String name) {
    AtomicLong id = new AtomicLong();
    return pool -> {
      ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
      thread.setName(name + "-" + id.incrementAndGet());
      return thread;
    };
  }

  private static List<Map<String, Object>> executors(final ConfigValue candidate,
      final boolean daemon, final int priority, final int n) {
    if (candidate.valueType() == ConfigValueType.STRING) {
      Map<String, Object> options = executor("default", daemon, priority, n,
          candidate.unwrapped());
      return ImmutableList.of(options);
    }
    ConfigObject conf = (ConfigObject) candidate;
    List<Map<String, Object>> result = new ArrayList<>();
    for (Entry<String, ConfigValue> executor : conf.entrySet()) {
      String name = executor.getKey();
      Object value = executor.getValue().unwrapped();
      Map<String, Object> options = new HashMap<>();
      options.putAll(executor(name, daemon, priority, n, value));
      result.add(options);
    }
    return result;
  }

  private static Map<String, Object> executor(final String name, final boolean daemon,
      final int priority,
      final int n,
      final Object value) {
    Map<String, Object> options = new HashMap<>();
    options.put("name", name);
    options.put("daemon", daemon);
    options.put("priority", priority);
    Iterable<String> spec = Splitter.on(",").trimResults().omitEmptyStrings()
        .split(value.toString());
    for (String option : spec) {
      String[] opt = option.split("=");
      String optname = opt[0].trim();
      Object optvalue;
      if (optname.equals("daemon")) {
        optvalue = opt.length > 1 ? Boolean.parseBoolean(opt[1].trim()) : daemon;
      } else if (optname.equals("asyncMode")) {
        optvalue = opt.length > 1 ? Boolean.parseBoolean(opt[1].trim()) : false;
      } else if (optname.equals("priority")) {
        optvalue = opt.length > 1 ? Integer.parseInt(opt[1].trim()) : priority;
      } else {
        optvalue = opt.length > 1 ? Integer.parseInt(opt[1].trim()) : n;
        options.put("type", optname);
      }
      options.put(optname, optvalue);
    }
    return options;
  }

}
