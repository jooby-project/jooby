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
                            "c.validateQueryBean(io.jooby.validation.BeanValidator.validate(ctx, ctx.query(\"bean\").isMissing() ? ctx.query().toNullable(tests.validation.Bean.class) : ctx.query(\"bean\").toNullable(tests.validation.Bean.class)))")
                    );

                    assertTrue(source.contains(
                            "c.validateFormBean(io.jooby.validation.BeanValidator.validate(ctx, ctx.form(\"bean\").isMissing() ? ctx.form().toNullable(tests.validation.Bean.class) : ctx.form(\"bean\").toNullable(tests.validation.Bean.class)))")
                    );

                    assertTrue(source.contains(
                            "c.validateBindParamBean(io.jooby.validation.BeanValidator.validate(ctx, tests.validation.Bean.map(ctx)))")
                    );

                    assertTrue(source.contains(
                            "c.validateBodyBean(io.jooby.validation.BeanValidator.validate(ctx, ctx.body(tests.validation.Bean.class)))")
                    );

                    assertTrue(source.contains(
                            "c.validateListOfBodyBeans(io.jooby.validation.BeanValidator.validate(ctx, ctx.body(io.jooby.Reified.list(tests.validation.Bean.class).getType())))")
                    );
                });
    }
}
