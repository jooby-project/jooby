package ${packageName}
${imports}
@io.jooby.annotation.Generated(${packageName}.${className}::class)
open class ${generatedClassName}(protected val factory: java.util.function.Function<io.jooby.Context, ${className}>) : io.jooby.MvcExtension, io.jooby.MvcFactory<${className}?> {

    constructor() : this(${defaultInstance})

    constructor(instance: ${className}) : this(java.util.function.Function<io.jooby.Context, ${className}> { instance })

    constructor(type: Class<${className}?>) : this(java.util.function.Function<io.jooby.Context, ${className}> { ctx: io.jooby.Context -> ctx.require<${className}>(type) })

    constructor(provider: jakarta.inject.Provider<${className}?>) : this(java.util.function.Function<io.jooby.Context, ${className}> { provider.get()!! })

    @Throws(Exception::class)
    override fun install(app: io.jooby.Jooby) {
${bindings}
    }

${routes}

    override fun supports(type: Class<${className}?>): Boolean {
        return type == ${className}::class.java
    }

    override fun create(provider: jakarta.inject.Provider<${className}?>): io.jooby.Extension {
        return ${generatedClassName}(provider)
    }
}