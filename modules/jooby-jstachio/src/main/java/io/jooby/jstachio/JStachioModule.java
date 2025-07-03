/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jstachio;

import java.util.ServiceLoader;
import java.util.function.BiFunction;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Context;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jstach.jstachio.JStachio;
import io.jstach.jstachio.spi.JStachioExtension;
import io.jstach.jstachio.spi.JStachioFactory;

/**
 * Allows Jooby to render JStachio models. If a JStachio is not set on this module before calling
 * install then a ServiceLoader based JStachio will be used. See <a
 * href="https://jstach.io/doc/jstachio/1.x/apidocs">JStachio documentation</a>.
 *
 * <p>For development mode to allow hot reloading add (currently experimental) the dependency of
 * <code>io.jstach:jstachio-jmustache:VERSION</code> (Maven GAV). <strong>Development mode is
 * enabled by default if the extension is found. to disable set <code>JSTACHIO_JMUSTACHE_DISABLE
 * </code> to <code>true</code>!</strong>
 *
 * @author agentgt
 * @since 3.0.0
 */
public class JStachioModule implements Extension {

  private @Nullable JStachio jstachio;
  private int bufferSize = 8 * 1024;
  private BiFunction<Context, String, String> contextFunction = Context::getAttribute;

  /**
   * Sets the jstachio to use instead of the default.
   *
   * @param jstachio the jstachio instance to be used instead of the default.
   * @return this
   */
  public @NonNull JStachioModule jstachio(@Nullable JStachio jstachio) {
    this.jstachio = jstachio;
    return this;
  }

  /**
   * Sets the initial buffer size to be used for rendering. Default is <code>8 * 1024</code>.
   *
   * @param bufferSize an integer 0 or greater.
   * @return this
   * @throws IllegalArgumentException if the bufferSize is less than <code>0</code>.
   */
  public @NonNull JStachioModule bufferSize(int bufferSize) {
    if (bufferSize < 0) {
      throw new IllegalArgumentException("bufferSize should be greater than 0");
    }
    this.bufferSize = bufferSize;
    return this;
  }

  /**
   * JStachio will by default bind {@linkplain Context#getAttributes() Context attributes} to <code>
   * &#64;context</code>. This configuration option allows fetching context keys from something
   * else. <a
   * href="https://jstach.io/jstachio/io.jstach.jstachio/io/jstach/jstachio/context/package-summary.html">
   * See JStachio doc on context. </a>
   *
   * @param contextFunction
   * @return This module.
   */
  public JStachioModule contextFunction(BiFunction<Context, String, String> contextFunction) {
    this.contextFunction = contextFunction;
    return this;
  }

  /**
   * Installs JStachio into Jooby and provides the JStachio service to the {@link ServiceRegistry}.
   * {@inheritDoc}
   */
  @Override
  public void install(@NonNull Jooby application) throws Exception {
    JStachio j = this.jstachio;
    ServiceRegistry services = application.getServices();
    if (j == null) {
      j = services.getOrNull(JStachio.class);
      if (j == null) {
        final JStachio joobyJStachio =
            JStachioFactory.builder()
                .add(extensions(application))
                .add(new JoobyJStachioConfig(application.getEnvironment()))
                .build();
        j = joobyJStachio;
        this.jstachio = j;
        services.put(JStachio.class, joobyJStachio);
      }
    } else {
      services.put(JStachio.class, this.jstachio);
    }
    JStachioBuffer buffer = JStachioBuffer.of(bufferSize);

    JStachioMessageEncoder encoder = new JStachioMessageEncoder(j, buffer, contextFunction);
    application.encoder(encoder);
  }

  /**
   * Default implementation uses the ServiceLoader to load extensions.
   *
   * @param application used to get the environments classloader.
   * @return found extensions.
   */
  protected Iterable<JStachioExtension> extensions(Jooby application) {
    return ServiceLoader.load(
        JStachioExtension.class, application.getEnvironment().getClassLoader());
  }
}
