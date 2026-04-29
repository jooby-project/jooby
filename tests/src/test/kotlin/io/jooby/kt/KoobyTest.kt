/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.kt

import io.jooby.*
import io.jooby.value.Value
import io.mockk.*
import java.util.function.Supplier
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Top-level variables and functions for runApp coverage.
// Defining these at the file level ensures their generated class names contain "Kt$"
// which allows `configurePackage` to correctly extract the application name.
val dummyServer = mockk<Server>(relaxed = true)
val dummyMode = ExecutionMode.WORKER

fun callRunAppConsumers() {
  runApp(emptyArray()) { get("/") { "ok" } }
  runApp(emptyArray(), dummyMode) { get("/") { "ok" } }
  runApp(emptyArray(), dummyServer) { get("/") { "ok" } }
  runApp(emptyArray(), dummyServer, dummyMode) { get("/") { "ok" } }
}

fun callRunAppSuppliers() {
  val provider = { Kooby() }
  runApp(emptyArray(), provider)
  runApp(emptyArray(), dummyMode, provider)
  runApp(emptyArray(), dummyServer, provider)
  runApp(emptyArray(), dummyServer, dummyMode, provider)
}

fun callRunAppVarargs() {
  val provider = { Kooby() }
  runApp(emptyArray(), provider, provider)
  runApp(emptyArray(), dummyMode, provider, provider)
  runApp(emptyArray(), dummyServer, provider, provider)
  runApp(emptyArray(), dummyServer, dummyMode, provider, provider)
}

class KoobyTest {

  @BeforeEach
  fun setup() {
    // Mock the static runApp methods and server loading to prevent real HTTP servers from starting
    mockkStatic(Jooby::class)
    mockkStatic(Server::class)
    every { Server.loadServer() } returns dummyServer
    every {
      Jooby.runApp(
        any<Array<String>>(),
        any<Server>(),
        any<ExecutionMode>(),
        any<Supplier<Jooby>>(),
      )
    } returns mockk()
    every {
      Jooby.runApp(
        any<Array<String>>(),
        any<Server>(),
        any<ExecutionMode>(),
        any<List<Supplier<Jooby>>>(),
      )
    } returns mockk()
  }

  @AfterEach
  fun teardown() {
    unmockkStatic(Jooby::class)
    unmockkStatic(Server::class)
    unmockkAll()
    clearAllMocks()
  }

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
    every { value.get("myProp") } returns subValue
    every { subValue.to(String::class.java) } returns "resolved"

    // 2. Stub for both primitive (int) and boxed (Integer)
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
  fun `Kooby filters (use, before, after) should delegate correctly`() {
    val app = spyk(Kooby())
    val ctx = mockk<Context>(relaxed = true)

    // use
    val filterSlot = slot<Route.Filter>()
    every { app.use(capture(filterSlot)) } returns app
    var useCalled = false
    app.use {
      useCalled = true
      "use"
    }
    val next = mockk<Route.Handler>(relaxed = true)
    filterSlot.captured.apply(next).apply(ctx)
    assertTrue(useCalled)

    // before
    val beforeSlot = slot<Route.Before>()
    every { app.before(capture(beforeSlot)) } returns app
    var beforeCalled = false
    app.before { beforeCalled = true }
    beforeSlot.captured.apply(ctx)
    assertTrue(beforeCalled)

    // after
    val afterSlot = slot<Route.After>()
    every { app.after(capture(afterSlot)) } returns app
    var afterCalled = false
    app.after { afterCalled = true }
    afterSlot.captured.apply(ctx, "res", null)
    assertTrue(afterCalled)
  }

  @Test
  fun `Kooby routing standard methods should delegate correctly`() {
    val app = Kooby {
      // Nested DSL Groupings
      path("/api") {
        get("/g") { "get" }
        post("/po") { "post" }
        put("/pu") { "put" }
        delete("/d") { "delete" }
        patch("/pa") { "patch" }
        head("/h") { "head" }
        trace("/tr") { "trace" }
        options("/o") { "options" }
        route("CUSTOM", "/c") { "custom" }
      }
      routes { get("/routes") { "routes" } }
      // Standard Route.Handler overrides
      get("/std/g", Route.Handler { "std.get" })
      post("/std/po", Route.Handler { "std.post" })
      put("/std/pu", Route.Handler { "std.put" })
      delete("/std/d", Route.Handler { "std.delete" })
      patch("/std/pa", Route.Handler { "std.patch" })
      head("/std/h", Route.Handler { "std.head" })
      trace("/std/tr", Route.Handler { "std.trace" })
      options("/std/o", Route.Handler { "std.options" })
      route("CUSTOM", "/std/c", Route.Handler { "std.custom" })
    }

    val ctx = mockk<Context>(relaxed = true)
    // Verify all routes are correctly registered
    assertEquals(19, app.routes.size)
    // Verify all bound handlers can be executed without error to cover lambdas
    app.routes.forEach { assertNotNull(it.handler.apply(ctx)) }
  }

  @Test
  fun `Websocket and SSE extensions should delegate correctly`() {
    val app = spyk(Kooby())
    val ctx = mockk<Context>(relaxed = true)

    val wsSlot = slot<WebSocket.Initializer>()
    every { app.ws(any(), capture(wsSlot)) } returns mockk(relaxed = true)
    var wsCalled = false
    app.ws("/ws") {
      wsCalled = true
      "ws"
    }
    wsSlot.captured.init(ctx, mockk(relaxed = true))
    assertTrue(wsCalled)

    val sseSlot = slot<ServerSentEmitter.Handler>()
    every { app.sse(any(), capture(sseSlot)) } returns mockk(relaxed = true)
    var sseCalled = false
    app.sse("/sse") {
      sseCalled = true
      "sse"
    }
    val sse = mockk<ServerSentEmitter>(relaxed = true)
    every { sse.context } returns ctx
    sseSlot.captured.handle(sse)
    assertTrue(sseCalled)
  }

  @Test
  fun `Kooby coroutine router should set attributes and use cache`() {
    val app = Kooby()
    val router1 = app.coroutine { get("/coro") { "hi" } }
    val router2 = app.coroutine { get("/coro2") { "hi2" } }

    // Ensures computeIfAbsent cache branch is covered
    assertSame(router1, router2)

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
    val env = app.environmentOptions { setActiveNames(listOf("test")) }

    // Verify the environment was set on the app
    assertEquals(env, app.environment)
    assertTrue(env.activeNames.contains("test"))
  }

  @Test
  fun `Cors helper should initialize`() {
    val c = cors { setOrigin("*") }
    assertNotNull(c)
  }

  @Test
  fun `runApp overloads should execute Jooby runApp and configure internal package properties`() {
    callRunAppConsumers()
    callRunAppSuppliers()
    callRunAppVarargs()

    // Verify all the internal overloaded runApp variations hit the base Jooby implementation
    verify(exactly = 8) {
      Jooby.runApp(
        any<Array<String>>(),
        any<Server>(),
        any<ExecutionMode>(),
        any<Supplier<Jooby>>(),
      )
    }
    verify(exactly = 4) {
      Jooby.runApp(
        any<Array<String>>(),
        any<Server>(),
        any<ExecutionMode>(),
        any<List<Supplier<Jooby>>>(),
      )
    }
  }
}
