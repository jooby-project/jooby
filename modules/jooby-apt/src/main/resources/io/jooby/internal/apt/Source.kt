package ${packageName}
${imports}
@io.jooby.annotation.Generated(${packageName}.${className}::class)
open class ${generatedClassName}(protected val factory: java.util.function.Function<io.jooby.Context, ${className}>) : io.jooby.MvcExtension, io.jooby.MvcFactory<${className}?> {
    ${constructors}
    constructor(instance: ${className}) : this(java.util.function.Function<io.jooby.Context, ${className}> { instance })

    constructor(provider: java.util.function.Supplier<${className}?>) : this(java.util.function.Function<io.jooby.Context, ${className}> { provider.get()!! })

    @Throws(Exception::class)
    override fun install(app: io.jooby.Jooby) {
${bindings}
    }

${methods}

    override fun supports(type: Class<${className}?>): Boolean {
        return type == ${className}::class.java
    }

    override fun create(provider: java.util.function.Supplier<${className}?>): io.jooby.Extension {
        return ${generatedClassName}(provider)
    }
}