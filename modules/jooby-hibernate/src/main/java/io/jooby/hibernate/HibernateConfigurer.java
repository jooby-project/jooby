/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import com.typesafe.config.Config;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import javax.annotation.Nonnull;

/**
 * Allow to customize Hibernate bootstrap components.
 *
 * @author edgar
 * @since 2.5.1
 */
public class HibernateConfigurer {

  /**
   * Hook into bootstrap registry and customize it.
   *
   * @param builder Builder.
   * @param config Configuration.
   */
  public void configure(@Nonnull BootstrapServiceRegistryBuilder builder, @Nonnull Config config) {
  }

  /**
   * Hook into service registry and customize it.
   *
   * @param builder Builder.
   * @param config Configuration.
   */
  public void configure(@Nonnull StandardServiceRegistryBuilder builder, @Nonnull Config config) {
  }

  /**
   * Hook into metadata sources and customize it.
   *
   * @param sources Sources.
   * @param config Configuration.
   */
  public void configure(@Nonnull MetadataSources sources, @Nonnull Config config) {
  }

  /**
   * Hook into metadata builder and customize it.
   *
   * @param builder Builder.
   * @param config Configuration.
   */
  public void configure(@Nonnull MetadataBuilder builder, @Nonnull Config config) {
  }

  /**
   * Hook into SessionFactory creation and customize it.
   *
   * @param builder Builder.
   * @param config Configuration.
   */
  public void configure(@Nonnull SessionFactoryBuilder builder, @Nonnull Config config) {
  }
}
