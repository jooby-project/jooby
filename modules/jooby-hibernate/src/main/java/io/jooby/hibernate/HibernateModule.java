/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.internal.hibernate.ScanEnvImpl;
import io.jooby.internal.hibernate.SessionProvider;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hibernate ORM module: https://jooby.io/modules/hibernate.
 *
 * Usage:
 *
 * - Add hikari and hibernate dependency
 *
 * - Install them
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
 * <pre>{code
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
 * <pre>{code
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
 * To turn it off you need to specify all the persistent classes at creation time, using the
 * {@link HibernateModule#HibernateModule(Class[])} constructor.
 *
 * It is important to close either an {@link EntityManager} or {@link Session} created manually
 * from {@link javax.persistence.EntityManagerFactory} and {@link SessionFactory}.
 *
 * So code around session/entityManager looks like:
 *
 * <pre>{@code
 *   get("/", ctx -> {
 *     EntityManager em = require(EntityManager.class);
 *     Transaction trx = em.getTransaction();
 *     try {
 *       trx.begin();
 *
 *       // work with EntityManager compute a result
 *
 *       trx.commit();
 *
 *       return result;
 *     } catch(Exception x) {
 *       trx.rollback();
 *       throw x;
 *     } finally {
 *       em.close();
 *     }
 *   });
 * }</pre>
 *
 * To avoid all these lines of code we do provide a {@link TransactionalRequest} decorator so code
 * looks more simple:
 *
 * <pre>{@code
 *   decorator(new TransactionalRequest());
 *
 *   get("/", ctx -> {
 *     EntityManager em = require(EntityManager.class);
 *     // work with EntityManager compute a result
 *     return result;
 *   });
 * }</pre>
 *
 * Transaction and lifecycle of session/entityManager is managed by {@link TransactionalRequest}.
 *
 * Complete documentation is available at: https://jooby.io/modules/hibernate.
 *
 * @author edgar
 * @since 2.0.0
 */
public class HibernateModule implements Extension {

  private final String name;
  private List<String> packages = Collections.emptyList();
  private List<Class> classes;

  /**
   * Creates a Hibernate Module. The database parameter can be one of:
   *
   * - A property key defined in your application configuration file, like <code>db</code>.
   * - A special h2 database: mem, local or tmp.
   * - A jdbc connection string, like: <code>jdbc:mysql://localhost/db</code>
   *
   * @param name Database key, database type or jdbc url.
   * @param classes Persistent classes.
   */
  public HibernateModule(@Nonnull String name, Class... classes) {
    this.name = name;
    this.classes = Arrays.asList(classes);
  }

  /**
   * Creates a new Hikari module using the <code>db</code> property key. This key must be
   * present in the application configuration file, like:
   *
   * <pre>{@code
   *  db.url = "jdbc:url"
   *  db.user = dbuser
   *  db.password = dbpass
   * }</pre>
   *
   * @param classes Persistent classes.
   */
  public HibernateModule(Class... classes) {
    this("db", classes);
  }

  /**
   * Scan packages and look for persistent classes.
   *
   * @param packages Package names.
   * @return This module.
   */
  public @Nonnull HibernateModule scan(@Nonnull String... packages) {
    this.packages = Arrays.asList(packages);
    return this;
  }

  @Override public void install(@Nonnull Jooby application) {
    Environment env = application.getEnvironment();
    ServiceRegistry registry = application.getServices();
    DataSource dataSource = registry.getOrNull(ServiceKey.key(DataSource.class, name));
    boolean fallback = false;
    if (dataSource == null) {
      // TODO: replace with usage exception
      dataSource = registry.require(DataSource.class);
      fallback = true;
    }
    BootstrapServiceRegistryBuilder bsrb = new BootstrapServiceRegistryBuilder();
    boolean defaultDdlAuto = env.isActive("dev", "test");

    boolean flyway = isFlywayPresent(env, registry, name, fallback);
    String ddlAuto = flyway ? "none" : (defaultDdlAuto ? "update" : "none");

    BootstrapServiceRegistry bsr = bsrb.build();
    StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder(bsr);

    ssrb.applySetting(AvailableSettings.HBM2DDL_AUTO, ddlAuto);
    ssrb.applySetting(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed");
    // apply application.conf
    Map<String, String> base = env.getProperties("hibernate");
    Map<String, String> custom = env.getProperties(name + ".hibernate", "hibernate");
    Map<String, String> settings = new HashMap<>();
    settings.putAll(base);
    settings.putAll(custom);
    ssrb.applySettings(settings);
    ssrb.applySetting(AvailableSettings.DATASOURCE, dataSource);
    ssrb.applySetting(AvailableSettings.DELAY_CDI_ACCESS, true);

    StandardServiceRegistry serviceRegistry = ssrb.build();
    if (packages.isEmpty() && classes.isEmpty()) {
      packages = Stream.of(application.getBasePackage())
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }

    MetadataSources sources = new MetadataSources(serviceRegistry);
    packages.forEach(sources::addPackage);
    classes.forEach(sources::addAnnotatedClass);

    /** Scan package? */
    ClassLoader classLoader = env.getClassLoader();
    List<URL> packages = sources.getAnnotatedPackages().stream()
        .map(pkg -> classLoader.getResource(pkg.replace('.', '/')))
        .collect(Collectors.toList());

    MetadataBuilder metadataBuilder = sources.getMetadataBuilder();
    if (packages.size() > 0) {
      metadataBuilder.applyScanEnvironment(new ScanEnvImpl(packages));
    }
    Metadata metadata = metadataBuilder.build();

    SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
    sfb.applyName(name);
    sfb.applyNameAsJndiName(false);

    SessionFactory sf = sfb.build();

    registry.putIfAbsent(SessionFactory.class, sf);
    registry.put(ServiceKey.key(SessionFactory.class, name), sf);

    /** Session and EntityManager. */
    Provider sessionProvider = new SessionProvider(sf);
    registry.putIfAbsent(Session.class, sessionProvider);
    registry.put(ServiceKey.key(Session.class, name), sessionProvider);

    registry.putIfAbsent(EntityManager.class, sessionProvider);
    registry.put(ServiceKey.key(EntityManager.class, name), sessionProvider);

    application.onStop(sf::close);
  }

  private static boolean isFlywayPresent(Environment env, ServiceRegistry registry, String key,
      boolean fallback) {
    Optional<Class> flyway = env.loadClass("org.flywaydb.core.Flyway");
    if (flyway.isPresent()) {
      if (registry.getOrNull(ServiceKey.key(flyway.get(), key)) != null) {
        return true;
      }
      if (fallback && registry.getOrNull(flyway.get()) != null) {
        return true;
      }
    }
    return false;
  }
}
