/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i3705

import io.jooby.Jooby
import io.jooby.MvcExtension
import io.jooby.SneakyThrows
import io.jooby.annotation.GET
import io.jooby.annotation.Path
import io.jooby.kt.Kooby

@Path("/")
class C3705 {
  @GET("/search")
  fun search(): String {
    return "Hello"
  }
}

class BeanScope {
  fun <T> get(type: Class<T>): T {
    return type.newInstance()
  }
}

@io.jooby.annotation.Generated(C3705::class)
class C3705_ : MvcExtension {

  constructor() {}

  constructor(provider: SneakyThrows.Function<Class<C3705>, C3705>) {}

  override fun install(application: Jooby) {
    TODO("Not yet implemented")
  }
}

class App3705 : Kooby({ mvc(C3705_()) })

class App3705b :
  Kooby({
    val beanScope = BeanScope()
    mvc(C3705_(beanScope::get))
  })
