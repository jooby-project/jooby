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
package org.jooby.hbv;

import static java.util.Objects.requireNonNull;
import static javax.validation.Validation.byProvider;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Parser;
import org.jooby.Request;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

/**
 * Bean validation via Hibernate Validator.
 *
 * <h1>exposes</h1>
 * <ul>
 * <li>a {@link Validator},</li>
 * <li>a {@link HibernateValidatorConfiguration} and</li>
 * <li>a {@link Parser}</li>
 * </ul>
 *
 * <h1>usage</h1>
 *
 * <pre>
 * {
 *   use(new Hbv());
 *
 *   get("/", req {@literal ->} {
 *     Validator validator = req.require(Validator.class);
 *     Car car = req.params().to(Car.class);
 *     Set&lt;ConstraintViolation&gt; violations = validator.validate(car);
 *     if (violations.size() {@literal >} 0) {
 *       // handle errors
 *       ...
 *     }
 *   });
 * }
 * </pre>
 *
 * <h2>automatic validations of HTTP params and body</h2>
 *
 * Previous example demonstrate how to manually validate a bean created via:
 * {@link Request#params()} or {@link Request#body()}. The boilerplate code can be avoided if you
 * explicitly tell the validation module which classes require validation.
 *
 * The previous example can be rewritten as:
 *
 * <pre>
 * {
 *    use(new Hbv(Car.class));
 *
 *    get("/", () {@literal ->} {
 *      Car car = req.params().to(Car.class);
 *      // a valid car is here
 *      ...
 *    });
 * }
 * </pre>
 *
 * Here a {@link Parser} will do the boilerplate part and throws
 * a {@link ConstraintViolationException}.
 *
 * <h3>rendering a ConstraintViolationException</h3>
 *
 * The default err handler will render the {@link ConstraintViolationException} without problem, but
 * suppose we have a JavaScript client and want to display the errors in a friendly way.
 *
 * <pre>
 * {
 *   use(new Jackson()); // JSON renderer
 *
 *   use(new Hbv(Car.class)); // Validate Car objects
 *
 *   err((req, rsp, err) {@literal ->} {
 *     Throwable cause = err.getCause();
 *     if (cause instanceof ConstraintViolationException) {
 *       Set&lt;ConstraintViolation&lt;?&gt;&gt; constraints =
 *           ((ConstraintViolationException) cause) .getConstraintViolations();
 *
 *       Map&lt;Path, String&gt; errors = constraints.stream()
 *          .collect(Collectors.toMap(
 *              ConstraintViolation::getPropertyPath,
 *              ConstraintViolation::getMessage
 *           ));
 *       rsp.send(errors);
 *     }
 *   });
 *
 *   get("/", () {@literal ->} {
 *     Car car = req.params().to(Car.class);
 *     // a valid car is here
 *     ...
 *   });
 * }
 *</pre>
 *
 * The call to <code>rsp.send(errors);</code> will be rendered by ```Jackson``` (or any other that
 * applies) and will produces a more friendly response, here it will be a JavaScript object with the
 * errors.
 *
 * <h2>constraint validator factory</h2>
 *
 * {@link ConstraintValidatorFactory} is the extension point for customizing how constraint
 * validators are instantiated and released.
 *
 * In Jooby, a {@link ConstraintValidatorFactory} is powered by Guice.
 *
 * <h2>configuration</h2>
 *
 * Any property defined at <code>hibernate.validator</code> will be add it automatically:
 *
 * application.conf:
 *
 * <pre>
 * hibernate.validator.fail_fast = true
 * </pre>
 *
 * Or programmatically:
 *
 * <pre>
 * {
 *   use(new Hbv().doWith(config {@literal ->} {
 *     config.failFast(true);
 *   }));
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.6.0
 */
public class Hbv implements Jooby.Module {

  private Predicate<TypeLiteral<?>> predicate;

  private BiConsumer<HibernateValidatorConfiguration, Config> configurer;

  /**
   * Creates a new {@link Hbv} module.
   *
   * @param predicate A predicate to test if a class require validation or not.
   */
  public Hbv(final Predicate<TypeLiteral<?>> predicate) {
    this.predicate = requireNonNull(predicate, "Predicate is required.");
  }

  /**
   * Creates a new {@link Hbv} module.
   *
   * @param classes List of classes that require validation.
   */
  public Hbv(final Class<?>... classes) {
    this(typeIs(classes));
  }

  /**
   * Creates a new {@link Hbv} module. No automatic validation is applied.
   */
  public Hbv() {
    this(none());
  }

  /**
   * Setup a configurer callback.
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public Hbv doWith(final Consumer<HibernateValidatorConfiguration> configurer) {
    requireNonNull(configurer, "Configurer callback is required.");
    this.configurer = (hvc, conf) -> {
      configurer.accept(hvc);
    };
    return this;
  }

  /**
   * Setup a configurer callback.
   *
   * @param configurer Configurer callback.
   * @return This module.
   */
  public Hbv doWith(final BiConsumer<HibernateValidatorConfiguration, Config> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer callback is required.");
    return this;
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    HibernateValidatorConfiguration configuration = byProvider(HibernateValidator.class)
        .configure();

    if (config.hasPath("hibernate.validator")) {
      config.getConfig("hibernate.validator").root().forEach((k, v) -> {
        configuration.addProperty("hibernate.validator." + k, v.unwrapped().toString());
      });
    }

    if (configurer != null) {
      configurer.accept(configuration, config);
    }

    binder.bind(HibernateValidatorConfiguration.class).toInstance(configuration);

    binder.bind(Validator.class).toProvider(HbvFactory.class).asEagerSingleton();

    Multibinder.newSetBinder(binder, Parser.class).addBinding()
        .toInstance(new HbvParser(predicate));
  }

  static Predicate<TypeLiteral<?>> typeIs(final Class<?>[] classes) {
    return type -> {
      Class<?> it = type.getRawType();
      for (Class<?> klass : classes) {
        if (it.isAssignableFrom(klass)) {
          return true;
        }
      }
      return false;
    };
  }

  static Predicate<TypeLiteral<?>> none() {
    return type -> false;
  }

}
