package ${packageName};
${imports}
@io.jooby.annotation.Generated(${className}.class)
public class ${generatedClassName} implements io.jooby.Extension {
    protected java.util.function.Function<io.jooby.Context, ${className}> factory;
${constructors}
    public ${generatedClassName}(${className} instance) {
       setup(ctx -> instance);
    }

    public ${generatedClassName}(io.jooby.SneakyThrows.Supplier<${className}> provider) {
       setup(ctx -> (${className}) provider.get());
    }

    public ${generatedClassName}(io.jooby.SneakyThrows.Function<Class<${className}>, ${className}> provider) {
       setup(ctx -> provider.apply(${className}.class));
    }

    private void setup(java.util.function.Function<io.jooby.Context, ${className}> factory) {
        this.factory = factory;
    }

${methods}
}
