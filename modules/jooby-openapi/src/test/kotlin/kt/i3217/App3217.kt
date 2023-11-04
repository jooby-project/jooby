/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package kt.i3217

import io.jooby.annotation.GET
import io.jooby.kt.Kooby

class App3217 :
  Kooby({
    // mount the script router
    mount(ScriptRouter3217())
    // mount the mvc router
    mvc(MvcRouter3217())
  })

// a router using script api
class ScriptRouter3217 : Kooby({ get("/api-script/beans") { listOf(SomeBean3217("foo")) } })

// a router using mvc api for comparison
class MvcRouter3217 {

  @GET("/api-mvc/beans") fun getBeans() = listOf(SomeBean3217("bar"))
}
