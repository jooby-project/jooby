/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.gradle;

/**
 * Gradle plugin for Jooby run with test lifecycle.
 *
 * @author edgar
 * @since 2.0.0
 */
public class TestRunTask extends RunTask {
  public TestRunTask() {
    setUseTestScope(true);
  }
}
