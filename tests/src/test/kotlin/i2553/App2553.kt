/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i2553

import io.jooby.Kooby
import java.util.concurrent.CompletableFuture

class App2553 :
  Kooby({
    get("/2553/line") { CompletableFuture.supplyAsync { "line" } }

    get("/2553/var") {
      val future: CompletableFuture<String> = CompletableFuture.supplyAsync { "var" }
      future
    }
  })
