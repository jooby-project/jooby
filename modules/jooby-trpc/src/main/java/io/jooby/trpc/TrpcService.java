/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc;

import io.jooby.Jooby;

/**
 * Generated tRPC service. Generated service ends with <code>Trpc_</code> suffix.
 *
 * @author edgar
 * @since 4.3.0
 */
public interface TrpcService {

  /**
   * Install generate tRPC routes.
   *
   * @param path Base path. Defaults to <code>/trpc</code>.
   * @param application Main application.
   * @throws Exception If something goes wrong.
   */
  void install(String path, Jooby application) throws Exception;
}
