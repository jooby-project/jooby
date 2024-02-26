/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i3228

import io.jooby.junit.ServerTest
import io.jooby.junit.ServerTestRunner
import io.jooby.kt.Kooby
import java.util.concurrent.Executor
import org.junit.jupiter.api.Assertions

class RouterWithoutWorker :
  Kooby({
    coroutine {
      get("/i3228/without-worker") {
        "nonBlocking: " +
          ctx.route.isNonBlocking +
          ", coroutine: " +
          ctx.route.attributes["coroutine"]
      }
    }
  })

class RouterWithoutWorkerNoCoroutine :
  Kooby({
    get("/i3228/without-worker-no-coroutine") {
      "nonBlocking: " +
        ctx.route.isNonBlocking +
        ", coroutine: " +
        ctx.route.attributes["coroutine"]
    }
  })

class RouterWithWorker(worker: Executor) :
  Kooby({
    this.worker = worker
    coroutine {
      get("/i3228/with-worker") {
        "nonBlocking: " +
          ctx.route.isNonBlocking +
          ", coroutine: " +
          ctx.route.attributes["coroutine"]
      }
    }
  })

class Issue3228 {
  @ServerTest
  fun shouldCopyCoroutineState(runner: ServerTestRunner) =
    runner
      .use {
        Kooby { ->
          mount(RouterWithWorker(this.worker))
          mount(RouterWithoutWorker())
          coroutine { mount(RouterWithoutWorkerNoCoroutine()) }
        }
      }
      .ready { client ->
        client.get("/i3228/without-worker") { rsp ->
          Assertions.assertEquals("nonBlocking: true, coroutine: true", rsp.body!!.string())
        }
        client.get("/i3228/without-worker-no-coroutine") { rsp ->
          Assertions.assertEquals("nonBlocking: true, coroutine: true", rsp.body!!.string())
        }
        client.get("/i3228/with-worker") { rsp ->
          Assertions.assertEquals("nonBlocking: true, coroutine: true", rsp.body!!.string())
        }
      }
}
