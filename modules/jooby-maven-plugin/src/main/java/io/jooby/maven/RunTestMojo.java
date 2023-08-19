/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.TEST_COMPILE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Maven plugin for jooby run using the test scope.
 *
 * @author edgar
 * @since 2.0.0
 */
@Mojo(name = "testRun", threadSafe = true, requiresDependencyResolution = TEST, aggregator = true)
@Execute(phase = TEST_COMPILE)
public class RunTestMojo extends RunMojo {

  public RunTestMojo() {
    setUseTestScope(true);
  }

  @Override
  protected String mojoName() {
    return "testRun";
  }
}
