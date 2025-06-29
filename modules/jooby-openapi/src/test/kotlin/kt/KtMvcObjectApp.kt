/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt

import io.jooby.kt.Kooby
import io.jooby.openapi.MvcExtensionGenerator.toMvcExtension

class KtMvcObjectApp : Kooby({ mvc(toMvcExtension(KtObjectController::class.java)) })
