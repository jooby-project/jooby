package io.jooby

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoroutineUnitTest {
  @Test
  fun shouldSupportUnitTestWhileUsingCoroutineRouter() {
    val app = Kooby {
      coroutine {
        get("/coroutine") {
          withContext(coroutineScope.coroutineContext) {
            ctx.requestPath
          }
        }

        get("/delay") {
          delay(100)
          ctx.requestPath
        }

        get("/heavy-task") {
          withContext(Dispatchers.IO) {
            ctx.responseType = MediaType.text
            ctx.responseCode = StatusCode.CREATED
            ctx.send(ctx.requestPath)
          }
          ctx
        }
      }
    }

    val router = MockRouter(app)

    assertEquals("/coroutine", router["/coroutine"].value())
    assertEquals("/delay", router["/delay"].value())

    router.get("/heavy-task") { rsp ->
      assertEquals("/heavy-task", rsp.value())
      assertEquals(MediaType.text, rsp.contentType)
      assertEquals(StatusCode.CREATED, rsp.statusCode)
    }
  }
}
