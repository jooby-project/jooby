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
package org.jooby.scanner;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.is;
import static javaslang.Predicates.noneOf;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Router;
import org.jooby.mvc.Path;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.typesafe.config.Config;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import javaslang.control.Try;

/**
 * <h1>scanner</h1>
 * <p>
 * Classpath scanning services via
 * <a href="https://github.com/lukehutch/fast-classpath-scanner">FastClasspathScanner</a>.
 * FastClasspathScanner is an uber-fast, ultra-lightweight classpath scanner for Java, Scala and
 * other JVM languages.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   use(new Scanner());
 * }
 * }</pre>
 *
 * <p>
 * This modules scan the application class-path and automatically discover and register
 * <code>MVC routes/controllers</code>, {@link org.jooby.Jooby.Module} and {@link Jooby}
 * applications.
 * </p>
 *
 * <p>
 * It scans the application package. That's the package where your bootstrap application belong to.
 * Multi-package scanning is available too:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Scanner("foo", "bar"));
 * }
 * }</pre>
 *
 * <h2>services</h2>
 *
 * <p>
 * The next example scans and initialize any class in the application package annotated with
 * <code>Named</code>:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Scanner()
 *     .scan(Named.class)
 *   );
 * }
 * }</pre>
 *
 * <p>
 * The next example scans and initialize any class in the application package that implements
 * <code>MyService</code>:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Scanner()
 *     .scan(MyService.class)
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Guava {@link Service} are also supported:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Scanner()
 *     .scan(com.google.common.util.concurrent.Service.class)
 *   );
 *
 *   get("/guava", req -> {
 *     ServiceManager sm = req.require(ServiceManager.class);
 *     ...
 *   });
 * }
 * }</pre>
 *
 * <p>
 * They are added to {@link ServiceManager} and started and stopped automatically.
 * </p>
 *
 * <p>
 * Raw/plain Guice {@link Module} are supported too
 * </p>
 * :
 *
 * <pre>{@code
 * {
 *   use(new Scanner()
 *     .scan(Module.class)
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Of course, you can combine two or more strategies:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Scanner()
 *     .scan(MyService.class)
 *     .scan(Named.class)
 *     .scan(Singleton.class)
 *     .scan(MyAnnotation.class)
 *   );
 * }
 * }</pre>
 *
 * <p>
 * In all cases, services are created as <code>singleton</code> and started/stopped automatically
 * when {@link PostConstruct} and {@link PreDestroy} annotations are present.
 * </p>
 *
 * @author edgar
 * @since 1.0.0
 */
public class Scanner implements Jooby.Module {

  @SuppressWarnings("rawtypes")
  private static final Predicate<Class> A = Class::isAnnotation;

  @SuppressWarnings("rawtypes")
  private static final Predicate<Class> C = k -> !Modifier.isAbstract(k.getModifiers());

  @SuppressWarnings("rawtypes")
  private static final Predicate<Class> I = A.negate().and(Class::isInterface);

  @SuppressWarnings("rawtypes")
  private static final Predicate<Class> S = noneOf(I, A);

  private List<String> packages;

  @SuppressWarnings("rawtypes")
  private Set<Class> serviceTypes = new LinkedHashSet<>();

  /**
   * Creates a new {@link Scanner} and uses the provided scan spec or packages.
   *
   * @param scanSpec Scan spec or packages. See <a href=
   *        "https://github.com/lukehutch/fast-classpath-scanner/wiki/2.-Constructor#specifying-more-complex-scanning-criteria">Scan
   *        spec</a>.
   */
  public Scanner(final String... scanSpec) {
    this.packages = Lists.newArrayList(scanSpec);
  }

  /**
   * Creates a new {@link Scanner} and use the application package (a.k.a as namespace).
   */
  public Scanner() {
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    List<String> packages = Optional.ofNullable(this.packages)
        .orElseGet(() -> ImmutableList.of(conf.getString("application.ns")));
    Set<String> spec = Sets.newLinkedHashSet(packages);
    serviceTypes.forEach(it -> spec.add(it.getPackage().getName()));

    FastClasspathScanner scanner = new FastClasspathScanner(spec.toArray(new String[spec.size()]));

    Router routes = env.router();

    ClassLoader loader = getClass().getClassLoader();
    Function<String, Class> loadClass = name -> Try.of(() -> loader.loadClass(name)).get();

    // bind once as singleton + post/pre callbacks
    Set<Object> bindings = new HashSet<>();
    Predicate<Object> once = bindings::add;

    Consumer<Class> bind = klass -> {
      binder.bind(klass).asEagerSingleton();
      env.lifeCycle(klass);
    };

    ScanResult result = scanner.scan(conf.getInt("runtime.processors") + 1);

    Predicate<String> inPackage = name -> packages.stream()
        .filter(name::startsWith)
        .findFirst()
        .isPresent();

    /** Controllers: */
    result.getNamesOfClassesWithAnnotation(Path.class)
        .stream()
        .filter(once)
        .map(loadClass)
        .filter(C)
        .forEach(routes::use);

    /** Modules: */
    result.getNamesOfClassesImplementing(Jooby.Module.class)
        .stream()
        .filter(once)
        .map(loadClass)
        .filter(C)
        .forEach(klass -> ((Jooby.Module) newObject(klass)).configure(env, conf, binder));

    /** Apps: */
    result.getNamesOfSubclassesOf(Jooby.class)
        .stream()
        .filter(once)
        .filter(is(conf.getString("application.class")).negate())
        .map(loadClass)
        .filter(C)
        .forEach(klass -> routes.use(((Jooby) newObject(klass))));

    /** Annotated with: */
    serviceTypes.stream()
        .filter(A)
        .forEach(a -> {
          result.getNamesOfClassesWithAnnotation(a)
              .stream()
              .filter(once)
              .map(loadClass)
              .filter(C)
              .forEach(bind);
        });

    /** Implements: */
    serviceTypes.stream()
        .filter(I)
        .filter(noneOf(type(Jooby.Module.class), type(Module.class), type(Service.class)))
        .forEach(i -> {
          result.getNamesOfClassesImplementing(i)
              .stream()
              .filter(inPackage)
              .filter(once)
              .map(loadClass)
              .filter(C)
              .forEach(bind);
        });

    /** SubclassOf: */
    serviceTypes.stream()
        .filter(S)
        .forEach(k -> {
          result.getNamesOfSubclassesOf(k)
              .stream()
              .filter(inPackage)
              .filter(once)
              .map(loadClass)
              .filter(C)
              .forEach(bind);
        });

    /** Guice modules: */
    if (serviceTypes.contains(Module.class)) {
      result.getNamesOfClassesImplementing(Module.class)
          .stream()
          .filter(inPackage)
          .filter(once)
          .map(loadClass)
          .filter(C)
          .forEach(klass -> ((Module) newObject(klass)).configure(binder));
    }

    /** Guava services: */
    if (serviceTypes.contains(Service.class)) {
      Set<Class<Service>> guavaServices = new HashSet<>();
      result.getNamesOfClassesImplementing(Service.class)
          .stream()
          .filter(inPackage)
          .filter(once)
          .map(loadClass)
          .filter(C)
          .forEach(guavaServices::add);

      if (guavaServices.size() > 0) {
        guavaServices(env, binder, guavaServices);
      }
    }
  }

  /**
   * Add a scan criteria like an annotation, interface or class.
   *
   * @param type A scan criteria/type.
   * @return This module.
   */
  public Scanner scan(final Class<?> type) {
    // standard vs guice annotations
    Match(type).of(
        Case(is(Named.class),
            Arrays.asList(Named.class, com.google.inject.name.Named.class)),
        Case(is(com.google.inject.name.Named.class),
            Arrays.asList(Named.class, com.google.inject.name.Named.class)),
        Case(is(Singleton.class),
            Arrays.asList(Singleton.class, com.google.inject.Singleton.class)),
        Case(is(com.google.inject.Singleton.class),
            Arrays.asList(Singleton.class, com.google.inject.Singleton.class)),
        Case($(), Arrays.asList(type)))
        .forEach(serviceTypes::add);
    return this;
  }

  private static <T> T newObject(final Class<T> klass) {
    return Try.of(() -> klass.newInstance()).get();
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private static void guavaServices(final Env env, final Binder binder,
      final Set<Class<Service>> serviceTypes) {
    Consumer<Class> guavaService = klass -> {
      binder.bind(klass).asEagerSingleton();
      serviceTypes.add(klass);
    };

    serviceTypes.forEach(guavaService);
    // lazy service manager
    AtomicReference<ServiceManager> sm = new AtomicReference<>();
    Provider<ServiceManager> smProvider = () -> sm.get();
    binder.bind(ServiceManager.class).toProvider(smProvider);
    // ask Guice for services, create ServiceManager and start services
    env.onStart(r -> {
      List<Service> services = serviceTypes.stream()
          .map(r::require)
          .collect(Collectors.toList());
      sm.set(new ServiceManager(services));
      sm.get().startAsync().awaitHealthy();
    });
    // stop services
    env.onStop(() -> {
      sm.get().stopAsync().awaitStopped();
    });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private static Predicate<Class> type(final Class type) {
    return klass -> klass.isAssignableFrom(type);
  }

}
