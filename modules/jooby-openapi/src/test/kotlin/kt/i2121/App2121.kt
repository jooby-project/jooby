/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i2121

import io.jooby.kt.Kooby
import io.jooby.openapi.MvcExtensionGenerator.toMvcExtension

class App2121 : Kooby({ coroutine { mvc(toMvcExtension(Controller2121::class.java)) } })
