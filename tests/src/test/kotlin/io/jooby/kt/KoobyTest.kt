/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kt

import io.jooby.*
import io.jooby.value.Value
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KoobyTest {

  @Test
  fun `Registry extensions should delegate correctly`() {
    val registry = mockk<Registry>()
    every { registry.require(String::class.java) } returns "foo"
    every { registry.require(String::class.java, "bar") } returns "baz"

    assertEquals("foo", registry.require<String>())
    assertEquals("baz", registry.require<String>("bar"))
    assertEquals("foo", registry.require(String::class))
    assertEquals("baz", registry.require(String::class, "bar"))
  }

  @Test
  fun `ServiceRegistry extensions should delegate correctly`() {
    val services = mockk<ServiceRegistry>()
    val service = "myService"

    every { services.get(String::class.java) } returns service
    every { services.getOrNull(String::class.java) } returns null
    every { services.put(String::class.java, service) } returns null
    every { services.putIfAbsent(String::class.java, service) } returns service

    assertEquals(service, services.get(String::class))
    assertNull(services.getOrNull(String::class))
    assertNull(services.put(String::class, service))
    assertEquals(service, services.putIfAbsent(String::class, service))
  }

  @Test
  fun `Value extensions and property delegates should work`() {
    val value = mockk<Value>()
    val subValue = mockk<Value>()

    // 1. Property delegate: val myProp: String by value
    // This calls value.get("myProp") -> returns Value -> calls .to(String.class)
    every { value.get("myProp") } returns subValue
    every { subValue.to(String::class.java) } returns "resolved"

    // 2. Stub for both primitive (int) and boxed (Integer)
    // This covers both the reified to<Int>() and the KClass to(Int::class)
    every { value.to(Int::class.java) } returns (42)
    every { value.to(Int::class.javaObjectType) } returns (42)

    // Verification: Property delegate
    val myProp: String by value
    assertEquals("resolved", myProp)

    // Verification: Reified extension
    val result: Int = value.to()
    assertEquals(42, result)

    // Verification: KClass extension
    assertEquals(42, value.to(Int::class))
  }

  @Test
  fun `Context extensions should provide DSL access`() {
    val ctx = mockk<Context>()
    val query = mockk<QueryString>()
    val form = mockk<Formdata>()
    val body = mockk<Body>()

    every { ctx.query() } returns query
    every { ctx.form() } returns form
    every { ctx.body() } returns body
    every { ctx.body(String::class.java) } returns "body"
    every { ctx.form(String::class.java) } returns "form"
    every { ctx.query(String::class.java) } returns "query"

    assertEquals(query, ctx.query)
    assertEquals(form, ctx.form)
    assertEquals(body, ctx.body)
    assertEquals("body", ctx.body(String::class))
    assertEquals("form", ctx.form(String::class))
    assertEquals("query", ctx.query(String::class))
  }

  @Test
  fun `Kooby DSL should register routes correctly`() {
    val app = Kooby {
      get("/") { "get" }
      post("/") { "post" }
      put("/") { "put" }
      delete("/") { "delete" }
      patch("/") { "patch" }
      head("/") { "head" }
      trace("/") { "trace" }
      options("/") { "options" }
    }

    val routes = app.routes
    assertEquals(8, routes.size)
    assertEquals("GET", routes[0].method)
    assertEquals("POST", routes[1].method)
  }

  @Test
  fun `Kooby coroutine router should set attributes`() {
    val app = Kooby()
    app.coroutine { get("/coro") { "hi" } }

    val route = app.routes.find { it.pattern == "/coro" }!!
    assertTrue(route.isNonBlocking)
    assertEquals(true, route.attributes["coroutine"])
  }

  @Test
  fun `Kooby options should be configurable`() {
    val app = Kooby()
    val routerOptions = RouterOptions()

    // Test router options
    app.routerOptions(routerOptions)
    assertEquals(routerOptions, app.routerOptions)

    // Test environment options
    // We use a property that doesn't require a real file to exist on disk
    val env = app.environmentOptions { setActiveNames(listOf("test")) }

    // Verify the environment was set on the app
    assertEquals(env, app.environment)
    // Verify the option was applied to the resulting environment
    assertTrue(env.activeNames.contains("test"))
  }

  @Test
  fun `Cors helper should initialize`() {
    val c = cors { setOrigin("*") }
    // Access internal field via verify if possible or just check return
    assertNotNull(c)
  }
}
