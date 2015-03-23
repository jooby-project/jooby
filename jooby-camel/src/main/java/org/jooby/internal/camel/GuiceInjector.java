package org.jooby.internal.camel;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;

import org.apache.camel.spi.Injector;

public class GuiceInjector implements Injector {

  private com.google.inject.Injector guice;

  @Inject
  public GuiceInjector(final com.google.inject.Injector guice) {
    this.guice = requireNonNull(guice, "An injector is required.");
  }

  @Override
  public <T> T newInstance(final Class<T> type) {
    return guice.getInstance(type);
  }

  @Override
  public <T> T newInstance(final Class<T> type, final Object instance) {
    return newInstance(type);
  }

}
