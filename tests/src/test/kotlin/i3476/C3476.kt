/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i3476

import io.jooby.annotation.GET
import io.jooby.annotation.QueryParam

class C3476 {
  @GET("/3476") fun <E> shouldGenerateGenerics(@QueryParam bean: List<E>): List<E> = listOf()
}
