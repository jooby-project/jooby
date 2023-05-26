/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i1905

import io.jooby.Context
import io.jooby.kt.Kooby

class SubApp1905 : Kooby({ get("/sub") { _: Context? -> "null context" } })
