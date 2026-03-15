/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.langchain4j;

/** Listener for failover events in a model chain. */
@FunctionalInterface
public interface FailoverListener {
  /**
   * Called when a primary model fails and the system switches to a fallback.
   *
   * @param modelName The name of the model that failed.
   * @param error The exception that triggered the fallback.
   */
  void onFailover(String modelName, Throwable error);
}
