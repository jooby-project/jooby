package ${packageName};
${imports}
@io.jooby.annotation.Generated(${className}.class)
public class ${generatedClassName} implements io.jooby.MvcExtension {
    protected final java.util.function.Function<io.jooby.Context, ${className}> factory;
${constructors}
    public ${generatedClassName}(${className} instance) {
        this(ctx -> instance);
    }

    public ${generatedClassName}(java.util.function.Supplier<${className}> provider) {
        this(ctx -> provider.get());
    }

    public ${generatedClassName}(java.util.function.Function<io.jooby.Context, ${className}> factory) {
        this.factory = factory;
    }

${methods}
}
