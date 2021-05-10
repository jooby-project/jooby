package io.jooby

import io.jooby.Router.GET
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.argThat
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
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
    val mockCoroutineContext = mock(CoroutineContext::class.java)
    `when`(mockCoroutineContext.plus(any()
        ?: mockCoroutineContext)).thenReturn(mockCoroutineContext, ExtraContext())

    CoroutineRouter(CoroutineStart.DEFAULT, router).apply {
      launchContext { mockCoroutineContext + it + ExtraContext() }
      get("/path") { "Result" }
    }

    val handlerCaptor = ArgumentCaptor.forClass(Route.Handler::class.java)
    verify(router).route(eq(GET), eq("/path"), handlerCaptor.capture())
    verifyNoInteractions(mockCoroutineContext)

    handlerCaptor.value.apply(ctx)
    verify(mockCoroutineContext).plus(argThat { it is CoroutineExceptionHandler }
        ?: mockCoroutineContext)
    verify(mockCoroutineContext).plus(argThat { it is ExtraContext } ?: mockCoroutineContext)
  }

  class ExtraContext : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ExtraContext>
  }
}
