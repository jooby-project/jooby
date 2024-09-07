/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class BeanValidationGeneratorTest {

  @Test
  public void generate_validation_forBean() throws Exception {
    new ProcessorRunner(new BeanValidationsController())
        .withSource(
            false,
            source -> {
              assertTrue(
                  source.contains(
                      "c.validateQueryBean(io.jooby.validation.BeanValidator.apply(ctx,"
                          + " ctx.query(\"bean\").isMissing() ?"
                          + " ctx.query().toNullable(tests.validation.Bean.class) :"
                          + " ctx.query(\"bean\").toNullable(tests.validation.Bean.class)))"));

              assertTrue(
                  source.contains(
                      "c.validateFormBean(io.jooby.validation.BeanValidator.apply(ctx,"
                          + " ctx.form(\"bean\").isMissing() ?"
                          + " ctx.form().toNullable(tests.validation.Bean.class) :"
                          + " ctx.form(\"bean\").toNullable(tests.validation.Bean.class)))"));

              assertTrue(
                  source.contains(
                      "c.validateBindParamBean(io.jooby.validation.BeanValidator.apply(ctx,"
                          + " tests.validation.Bean.map(ctx)))"));

              assertTrue(
                  source.contains(
                      "c.validateBodyBean(io.jooby.validation.BeanValidator.apply(ctx,"
                          + " ctx.body(tests.validation.Bean.class)))"));

              assertTrue(
                  source.contains(
                      "c.validateListOfBodyBeans(io.jooby.validation.BeanValidator.apply(ctx,"
                          + " ctx.body(io.jooby.Reified.list(tests.validation.Bean.class).getType())))"));
            });
  }
}
