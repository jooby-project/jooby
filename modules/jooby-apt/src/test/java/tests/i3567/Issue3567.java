/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3567;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.jooby.apt.ProcessorRunner;

public class Issue3567 {

  @Test
  public void shouldSupportGoogleInjectAnnotation() throws Exception {
    var value = "bar";
    var conf = ConfigFactory.empty().withValue("foo", ConfigValueFactory.fromAnyRef(value));
    new ProcessorRunner(new C3567(conf))
        .withSourceCode(
            source -> {
              assertTrue(source.contains("this(C3567.class);"));
              assertTrue(source.contains("public C3567_(Class<C3567> type) {"));
              assertTrue(source.contains("this(ctx -> ctx.require(type));"));
            });
  }
}
