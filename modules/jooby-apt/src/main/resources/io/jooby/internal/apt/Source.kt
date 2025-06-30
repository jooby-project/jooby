package ${packageName}
${imports}
@io.jooby.annotation.Generated(${className}::class)
open class ${generatedClassName} : io.jooby.MvcExtension {
    private lateinit var factory: java.util.function.Function<io.jooby.Context, ${className}>

    ${constructors}
    constructor(instance: ${className}) { setup { instance } }

    constructor(provider: io.jooby.SneakyThrows.Supplier<${className}>) { setup { provider.get() } }

    constructor(provider: (Class<${className}>) -> ${className}) { setup { provider(${className}::class.java) } }

    constructor(provider:  io.jooby.SneakyThrows.Function<Class<${className}>, ${className}>) { setup { provider.apply(${className}::class.java) } }

    private fun setup(factory: java.util.function.Function<io.jooby.Context, ${className}>) {
      this.factory = factory
    }
${methods}
}
