/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.hibernate.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.internal.hibernate.ScanEnvImpl;
import io.jooby.internal.hibernate.SessionServiceProvider;
import io.jooby.internal.hibernate.StatelessSessionServiceProvider;
import io.jooby.internal.hibernate.UnitOfWorkProvider;
import jakarta.inject.Provider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Hibernate ORM module: https://jooby.io/modules/hibernate.
 *
 * <p>Usage:
 *
 * <p>- Add hikari and hibernate dependency
 *
 * <p>- Install them
 *
 * <pre>{@code
 * {
 *   install(new HikariModule());
 *
 *   install(new HibernateModule());
 * }
 * }</pre>
 *
 * - Use it
 *
 * <pre>{@code
 * {
 *
 *   get("/", ctx -> {
 *     EntityManagerFactory emf = require(EntityManagerFactory.class);
 *     // do with emf
 *   });
 *
 * }
 * }</pre>
 *
 * Optionally, you can require/inject a {@link SessionFactory} too:
 *
 * <pre>{@code
 * {
 *
 *   get("/", ctx -> {
 *     SessionFactory sf = require(SessionFactory.class);
 *     // do with sf
 *   });
 *
 * }
 * }</pre>
 *
 * By default the hibernate module scan the {@link Jooby#getBasePackage()} to register all the
 * persistent classes. To scan a different package use the {@link HibernateModule#scan(String...)}
 * method.
 *
 * <p>To turn it off you need to specify all the persistent classes at creation time, using the
 * {@link HibernateModule#HibernateModule(Class[])} constructor.
 *
 * <p>It is important to close either an {@link EntityManager} or {@link Session} created manually
 * from {@link EntityManagerFactory} and {@link SessionFactory}.
 *
 * <p>So code around session/entityManager looks like:
 *
 * <pre>{@code
 * get("/", ctx -> {
 *   EntityManager em = require(EntityManager.class);
 *   Transaction trx = em.getTransaction();
 *   try {
 *     trx.begin();
 *
 *     // work with EntityManager compute a result
 *
 *     trx.commit();
 *
 *     return result;
 *   } catch(Exception x) {
 *     trx.rollback();
 *     throw x;
 *   } finally {
 *     em.close();
 *   }
 * });
 * }</pre>
 *
 * To avoid all these lines of code we do provide a {@link TransactionalRequest} decorator so code
 * looks more simple:
 *
 * <pre>{@code
 * use(new TransactionalRequest());
 *
 * get("/", ctx -> {
 *   EntityManager em = require(EntityManager.class);
 *   // work with EntityManager compute a result
 *   return result;
 * });
 * }</pre>
 *
 * Transaction and lifecycle of session/entityManager is managed by {@link TransactionalRequest}.
 *
 * <p>Complete documentation is available at: <a
 * href="https://jooby.io/modules/hibernate">hibernate</a>.
 *
 * @author edgar
 * @since 2.0.0
 */
public class HibernateModule implements Extension {

  private final String name;
  private List<String> packages = Collections.emptyList();
  private final List<Class<?>> classes;
  private HibernateConfigurer configurer = new HibernateConfigurer();
  private SessionProvider sessionBuilder = SessionBuilder::openSession;
  private StatelessSessionProvider statelessSessionProvider =
      StatelessSessionBuilder::openStatelessSession;

  /**
   * Creates a Hibernate module.
   *
   * @param name The name/key of the data source to attach.
   * @param classes Persistent classes.
   */
  public HibernateModule(@NonNull String name, Class<?>... classes) {
    this.name = name;
    this.classes = List.of(classes);
  }

  /**
   * Creates a new Hibernate module. Use the default/first datasource and register objects using the
   * <code>db</code> key.
   *
   * @param classes Persistent classes.
   */
  public HibernateModule(Class<?>... classes) {
    this("db", classes);
  }

  /**
   * Creates a Hibernate module.
   *
   * @param name The name/key of the data source to attach.
   * @param classes Persistent classes.
   */
  public HibernateModule(@NonNull String name, List<Class<?>> classes) {
    this.name = name;
    this.classes = classes;
  }

  /**
   * Scan packages and look for persistent classes.
   *
   * @param packages Package names.
   * @return This module.
   */
  public @NonNull HibernateModule scan(@NonNull String... packages) {
    this.packages = List.of(packages);
    return this;
  }

  /**
   * Scan packages and look for persistent classes.
   *
   * @param packages Package names.
   * @return This module.
   */
  public @NonNull HibernateModule scan(@NonNull List<String> packages) {
    this.packages = packages;
    return this;
  }

  /**
   * Allow to customize a {@link Session} before opening it.
   *
   * @param sessionProvider Session customizer.
   * @return This module.
   */
  public @NonNull HibernateModule with(@NonNull SessionProvider sessionProvider) {
    this.sessionBuilder = sessionProvider;
    return this;
  }

  /**
   * Allow to customize a {@link StatelessSession} before opening it.
   *
   * @param sessionProvider Session customizer.
   * @return This module.
   */
  public @NonNull HibernateModule with(@NonNull StatelessSessionProvider sessionProvider) {
    this.statelessSessionProvider = sessionProvider;
    return this;
  }

  /**
   * Hook into Hibernate bootstrap components and allow to customize them.
   *
   * @param configurer Configurer.
   * @return This module.
   */
  public @NonNull HibernateModule with(@NonNull HibernateConfigurer configurer) {
    this.configurer = configurer;
    return this;
  }

  @Override
  public void install(@NonNull Jooby application) {
    var env = application.getEnvironment();
    var config = application.getConfig();
    var registry = application.getServices();
    var dataSource = registry.getOrNull(ServiceKey.key(DataSource.class, name));
    boolean fallback = false;
    if (dataSource == null) {
      // TODO: replace with usage exception
      dataSource = registry.require(DataSource.class);
      fallback = true;
    }
    var bsrb = new BootstrapServiceRegistryBuilder();
    boolean defaultDdlAuto = env.isActive("dev", "test");

    var flyway = isFlywayPresent(env, registry, name, fallback);
    var ddlAuto = flyway ? "none" : (defaultDdlAuto ? "update" : "none");

    configurer.configure(bsrb, config);

    var bsr = bsrb.build();
    var ssrb = new StandardServiceRegistryBuilder(bsr);

    ssrb.applySetting(AvailableSettings.HBM2DDL_AUTO, ddlAuto);
    ssrb.applySetting(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed");
    // apply application.conf
    var base = env.getProperties("hibernate");
    var custom = env.getProperties(name + ".hibernate", "hibernate");
    var javax = env.getProperties("hibernate.javax", "javax");
    var jakarta = env.getProperties("hibernate.jakarta", "jakarta");

    var settings = new HashMap<String, Object>();
    settings.putAll(base);
    settings.putAll(custom);
    settings.putAll(javax);
    settings.putAll(jakarta);

    ssrb.applySettings(settings);
    ssrb.applySetting(AvailableSettings.JAKARTA_JTA_DATASOURCE, dataSource);
    ssrb.applySetting(AvailableSettings.DELAY_CDI_ACCESS, true);

    configurer.configure(ssrb, config);

    StandardServiceRegistry serviceRegistry = ssrb.build();
    if (packages.isEmpty() && classes.isEmpty()) {
      packages =
          Stream.of(application.getBasePackage())
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
    }

    MetadataSources sources = new MetadataSources(serviceRegistry);
    packages.forEach(sources::addPackage);
    classes.forEach(sources::addAnnotatedClass);

    configurer.configure(sources, config);

    /* Scan package? */
    var classLoader = env.getClassLoader();
    var packages =
        sources.getAnnotatedPackages().stream()
            .map(pkg -> classLoader.getResource(pkg.replace('.', '/')))
            .toList();

    var metadataBuilder = sources.getMetadataBuilder();
    if (!packages.isEmpty()) {
      metadataBuilder.applyScanEnvironment(new ScanEnvImpl(packages));
    }

    configurer.configure(metadataBuilder, config);

    var metadata = metadataBuilder.build();

    var sfb = metadata.getSessionFactoryBuilder();
    sfb.applyName(name);
    sfb.applyNameAsJndiName(false);
    /*
    Bind Validator instance, so hibernate doesn't create a new factory.
    Need to scan due hibernate doesn't depend on validation classes
    */
    registry.entrySet().stream()
        .filter(
            it ->
                it.getKey()
                    .getType()
                    .getName()
                    .equals("jakarta.validation.ConstraintValidatorFactory"))
        .findFirst()
        .ifPresent(it -> sfb.applyValidatorFactory(it.getValue().get()));

    configurer.configure(sfb, config);

    var sf = sfb.build();

    /* Session and EntityManager. */
    Provider sessionServiceProvider = new SessionServiceProvider(sf, sessionBuilder);
    registry.putIfAbsent(Session.class, sessionServiceProvider);
    registry.put(ServiceKey.key(Session.class, name), sessionServiceProvider);

    registry.putIfAbsent(EntityManager.class, sessionServiceProvider);
    registry.put(ServiceKey.key(EntityManager.class, name), sessionServiceProvider);

    /* StatelessSession. */
    registry.putIfAbsent(
        StatelessSession.class, new StatelessSessionServiceProvider(sf, statelessSessionProvider));

    /* SessionFactory and EntityManagerFactory. */
    registry.putIfAbsent(SessionFactory.class, sf);
    registry.put(ServiceKey.key(SessionFactory.class, name), sf);

    registry.putIfAbsent(EntityManagerFactory.class, sf);
    registry.put(ServiceKey.key(EntityManagerFactory.class, name), sf);

    /* Session Provider: */
    registry.putIfAbsent(SessionProvider.class, sessionBuilder);
    registry.put(ServiceKey.key(SessionProvider.class, name), sessionBuilder);

    /* StatelessSession Provider: */
    registry.putIfAbsent(StatelessSessionProvider.class, this.statelessSessionProvider);
    registry.put(
        ServiceKey.key(StatelessSessionProvider.class, name), this.statelessSessionProvider);

    /* UnitOfWork Provider: */
    UnitOfWorkProvider unitOfWorkProvider = new UnitOfWorkProvider(sf, sessionBuilder);
    registry.putIfAbsent(UnitOfWork.class, unitOfWorkProvider);
    registry.put(ServiceKey.key(UnitOfWork.class, name), unitOfWorkProvider);

    application.onStop(sf);
  }

  private static boolean isFlywayPresent(
      Environment env, ServiceRegistry registry, String key, boolean fallback) {
    Optional<Class> flyway = env.loadClass("org.flywaydb.core.Flyway");
    if (flyway.isPresent()) {
      if (registry.getOrNull(ServiceKey.key(flyway.get(), key)) != null) {
        return true;
      }
      return fallback && registry.getOrNull(flyway.get()) != null;
    }
    return false;
  }
}
