/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Allow to customize Hibernate bootstrap components.
 *
 * @author edgar
 * @since 2.5.1
 */
public class HibernateConfigurer {

  /** Default constructor. */
  public HibernateConfigurer() {}

  /**
   * Hook into bootstrap registry and customize it.
   *
   * @param builder Builder.
   * @param config Configuration.
   */
  public void configure(@NonNull BootstrapServiceRegistryBuilder builder, @NonNull Config config) {}

  /**
   * Hook into service registry and customize it.
   *
   * @param builder Builder.
   * @param config Configuration.
   */
  public void configure(@NonNull StandardServiceRegistryBuilder builder, @NonNull Config config) {}

  /**
   * Hook into metadata sources and customize it.
   *
   * @param sources Sources.
   * @param config Configuration.
   */
  public void configure(@NonNull MetadataSources sources, @NonNull Config config) {}

  /**
   * Hook into metadata builder and customize it.
   *
   * @param builder Builder.
   * @param config Configuration.
   */
  public void configure(@NonNull MetadataBuilder builder, @NonNull Config config) {}

  /**
   * Hook into SessionFactory creation and customize it.
   *
   * @param builder Builder.
   * @param config Configuration.
   */
  public void configure(@NonNull SessionFactoryBuilder builder, @NonNull Config config) {}
}
