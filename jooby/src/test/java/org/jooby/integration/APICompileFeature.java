package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;

/**
 * Not a test, just a class to make sure API doesn't break java compiler... silly but necessary to
 * figure it out some compilation error from lambdas.
 *
 * @author edgar
 *
 */
public class APICompileFeature extends ServerFeature {

  {

    // App module
    use((mode, config, binder) -> {
      assertEquals("dev", mode.name());
    });

    use(ConfigFactory.empty());
  }

  @Test
  public void empty() {}
}
