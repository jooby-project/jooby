package io.jooby

import io.jooby.Router.GET
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class CoroutineRouterTest {
  private val router = mock(Router::class.java, RETURNS_DEEP_STUBS)
  private val ctx = mock(Context::class.java)

  @Test
  fun withoutLaunchContext() {
    CoroutineRouter(CoroutineStart.UNDISPATCHED, router).apply {
      get("/path") { "Result" }
    }

    val handlerCaptor = ArgumentCaptor.forClass(Route.Handler::class.java)
    verify(router).route(eq(GET), eq("/path"), handlerCaptor.capture())
    handlerCaptor.value.apply(ctx)

    verify(ctx).render("Result")
  }

  @Test
  fun launchContext_isRunEveryTime() {
    var coroutineRouteCalled = false
    CoroutineRouter(CoroutineStart.UNDISPATCHED, router).apply {
      launchContext { SampleCoroutineContext(ctx) }
      get("/path") {
        coroutineScope {
          assertSame(ctx, coroutineContext[SampleCoroutineContext.Key]!!.ctx)
          coroutineRouteCalled = true
        }
      }
    }

    val handlerCaptor = ArgumentCaptor.forClass(Route.Handler::class.java)
    verify(router).route(eq(GET), eq("/path"), handlerCaptor.capture())
    handlerCaptor.value.apply(ctx)
    assertTrue(coroutineRouteCalled)
  }

  class SampleCoroutineContext(val ctx: Context) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<SampleCoroutineContext>
  }
}
