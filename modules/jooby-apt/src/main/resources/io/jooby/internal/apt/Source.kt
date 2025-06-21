package ${packageName}
${imports}
@io.jooby.annotation.Generated(${className}::class)
open class ${generatedClassName}(protected val factory: java.util.function.Function<io.jooby.Context, ${className}>) : io.jooby.MvcExtension {
    ${constructors}
    constructor(instance: ${className}) : this(java.util.function.Function<io.jooby.Context, ${className}> { instance })

    constructor(provider: java.util.function.Supplier<${className}?>) : this(java.util.function.Function<io.jooby.Context, ${className}> { provider.get()!! })

${methods}
}
