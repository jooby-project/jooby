/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i4045;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue4045 {
  @Test
  public void shouldGenerateValidKotlinForDefaultPackageAndPrimitiveTypes() throws Exception {
    var controller = Class.forName("source.Controller4045").getConstructor().newInstance();
    new ProcessorRunner(controller)
        .withSourceCode(
            true,
            source -> {
              assertThat(source).doesNotContain("package ;");
              assertThat(source).contains("fun test(ctx: io.jooby.Context): ByteArray");
              assertThat(source).contains("Reified.set<Int>(Int::class.javaObjectType).getType()");
            });
  }
}
