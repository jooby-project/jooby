/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import io.jooby.Context;
import io.jooby.ServerSentEmitter;
import io.jooby.StatusCode;
import io.jooby.apt.JoobyProcessor;
import io.jooby.apt.ProcessorRunner;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;
import org.junit.jupiter.api.Test;

public class SseTest {
  @Test
  public void shouldGenerateSseRoutesAndInvokeEmitter() throws Exception {
    new ProcessorRunner(new SseController())
        .withSourceCode(
            source -> {
              assertThat(source).contains("app.sse(\"/events/sse\", this::sse)");
              assertThat(source).contains("app.sse(\"/events/alias\", this::sse)");
              assertThat(source).contains("app.sse(\"/events/{name}\", this::named)");
              assertThat(source).contains("public void sse(io.jooby.ServerSentEmitter emitter)");
              assertThat(source).doesNotContain("return ctx.getResponseCode()");
            })
        .withRouter(
            app -> {
              assertThat(app.getRoutes()).hasSize(3);
              var route = app.getRoutes().getFirst();
              assertThat(route.getMethod()).isEqualTo("SSE");
              var ctx = mock(Context.class, RETURNS_SELF);
              var emitter = mock(ServerSentEmitter.class);
              when(emitter.getContext()).thenReturn(ctx);
              doAnswer(
                      call -> {
                        call.getArgument(0, ServerSentEmitter.Handler.class).handle(emitter);
                        return ctx;
                      })
                  .when(ctx)
                  .upgrade(any(ServerSentEmitter.Handler.class));

              route.getHandler().apply(ctx);

              verify(ctx).setResponseType("text/event-stream; charset=utf-8");
              verify(emitter).send("Welcome");
              verify(emitter).onClose(any());
              verify(emitter).close();
              verify(ctx).setResponseCode(StatusCode.OK);

              when(ctx.path("name")).thenReturn(Value.value(new ValueFactory(), "name", "Ada"));
              when(ctx.getRequestPath()).thenReturn("/events/Ada");
              app.getRoutes().get(2).getHandler().apply(ctx);
              verify(emitter).send("Ada:/events/Ada");
            });
  }

  @Test
  public void shouldGenerateKotlinEmitterHandler() throws Exception {
    new ProcessorRunner(new SseController())
        .withSourceCode(
            true,
            source -> {
              assertThat(source).contains("app.sse(\"/events/sse\", this::sse)");
              assertThat(source).contains("fun sse(emitter: io.jooby.ServerSentEmitter)");
              assertThat(source).contains("val ctx = emitter.getContext()");
            });
  }

  @Test
  public void shouldRejectNonVoidSseMethods() {
    var compilation =
        Compiler.javac()
            .withProcessors(new JoobyProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "InvalidSse",
                    """
                    import io.jooby.annotation.SSE;
                    public class InvalidSse {
                      @SSE("/sse")
                      public String sse(io.jooby.ServerSentEmitter emitter) {
                        return "invalid";
                      }
                    }
                    """));
    CompilationSubject.assertThat(compilation).failed();
    CompilationSubject.assertThat(compilation).hadErrorContaining("@SSE method must return void");
  }
}
