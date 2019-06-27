/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import com.typesafe.config.Config;
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

import static org.hibernate.cfg.AvailableSettings.*;

public class HibernateModule implements Extension {

  private final String name;
  private List<String> packages = Collections.emptyList();
  private List<Class> classes;

  public HibernateModule(@Nonnull String name, @Nonnull Class... classes) {
    this.name = name;
    this.classes = Arrays.asList(classes);
  }

  public HibernateModule(Class... classes) {
    this("db", classes);
  }

  public HibernateModule scan(String... packages) {
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
    boolean default_ddl_auto = env.isActive("dev", "test");

    boolean flyway = isFlywayPresent(env, registry, name, fallback);
    String ddl_auto = flyway ? "none" : (default_ddl_auto ? "update" : "none");

    BootstrapServiceRegistry bsr = bsrb.build();
    StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder(bsr);

    ssrb.applySetting(HBM2DDL_AUTO, ddl_auto);
    ssrb.applySetting(CURRENT_SESSION_CONTEXT_CLASS, "managed");
    // apply application.conf
    Map<String, String> base = env.getProperties("hibernate");
    Map<String, String> custom = env.getProperties(name + ".hibernate", "hibernate");
    Map<String, String> settings = new HashMap<>();
    settings.putAll(base);
    settings.putAll(custom);
    ssrb.applySettings(settings);
    ssrb.applySetting(DATASOURCE, dataSource);
    ssrb.applySetting(DELAY_CDI_ACCESS, true);

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
