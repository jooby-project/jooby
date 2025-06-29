/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.issues.i2004

import io.jooby.kt.Kooby
import io.jooby.openapi.MvcExtensionGenerator

class App2004 : Kooby({ mvc(MvcExtensionGenerator.toMvcExtension(Controller2004::class.java)) })
