package io.jooby

import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeaturedKotlinTest {

  @Test
  fun explicitContext() {
    JoobyRunner { app ->
      app.get("/") { ctx ->
           "Hello World!"
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("Hello World!", rsp.body()!!.string())
      }
    }
  }

  @Test
  fun coroutineNoSuspend() {
    JoobyRunner { ->
      Kooby {
        get("/") {
          ctx.pathString() + "coroutine"
        }
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("/coroutine", rsp.body()!!.string())
      }
    }
  }

  @Test
  fun coroutineSuspend() {
    JoobyRunner { ->
      Kooby {
        get("/") {
          delay(100)
          ctx.pathString() + "coroutine"
        }
      }
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("/coroutine", rsp.body()!!.string())
      }
    }
  }

  @Test
  fun javaApiWithReactiveType() {
    JoobyRunner { ->
      val app = Jooby().apply {
        get("/") {
          println(Thread.currentThread())
          Flowable.range(1, 10)
              .map { i -> i.toString() + "," }
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
        }
      }
      app
    }.ready { client ->
      client.get("/") { rsp ->
        assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body()!!.string())
      }
    }
  }
}
