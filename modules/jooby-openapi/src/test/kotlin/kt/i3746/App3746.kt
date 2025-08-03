/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i3746

import io.jooby.kt.Kooby

class App3746 : Kooby({ get("/3746") { ctx.requestPath } })
