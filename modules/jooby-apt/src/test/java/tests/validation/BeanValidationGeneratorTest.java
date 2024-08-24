package tests.validation;

import io.jooby.apt.ProcessorRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeanValidationGeneratorTest {

    @Test
    public void generate_validation_forBean() throws Exception {
        new ProcessorRunner(new BeanValidationsController()).withSource(
                false,
                source -> {
                    assertTrue(source.contains(
                            "c.validateQueryBean(io.jooby.validation.ValidationHelper.validate(ctx, ctx.query(\"bean\").isMissing() ? ctx.query().toNullable(tests.validation.Bean.class) : ctx.query(\"bean\").toNullable(tests.validation.Bean.class)))")
                    );

                    assertTrue(source.contains(
                            "c.validateFormBean(io.jooby.validation.ValidationHelper.validate(ctx, ctx.form(\"bean\").isMissing() ? ctx.form().toNullable(tests.validation.Bean.class) : ctx.form(\"bean\").toNullable(tests.validation.Bean.class)))")
                    );

                    assertTrue(source.contains(
                            "c.validateBodyBean(io.jooby.validation.ValidationHelper.validate(ctx, ctx.body(tests.validation.Bean.class)))")
                    );

                });
    }
}
