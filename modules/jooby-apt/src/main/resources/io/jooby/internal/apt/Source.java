package ${packageName};
${imports}
@io.jooby.annotation.Generated(${packageName}.${className}.class)
public class ${generatedClassName} implements io.jooby.MvcExtension, io.jooby.MvcFactory<${className}> {
    protected final java.util.function.Function<io.jooby.Context, ${className}> factory;

    public ${generatedClassName}() {
        this(${defaultInstance});
    }

    public ${generatedClassName}(${className} instance) {
        this(ctx -> instance);
    }

    public ${generatedClassName}(Class<${className}> type) {
        this(ctx -> ctx.require(type));
    }

    public ${generatedClassName}(java.util.function.Supplier<${className}> provider) {
        this(ctx -> provider.get());
    }

    public ${generatedClassName}(java.util.function.Function<io.jooby.Context, ${className}> factory) {
        this.factory = factory;
    }

    public void install(io.jooby.Jooby app) throws Exception {
${bindings}
    }

${methods}

    public boolean supports(Class<${className}> type) {
        return type == ${className}.class;
    }

    public io.jooby.Extension create(java.util.function.Supplier<${className}> provider) {
        return new ${generatedClassName}(provider);
    }
}
