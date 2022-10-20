/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i1807;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.jooby.Multipart;
import io.jooby.ValueNode;
import io.jooby.apt.MvcModuleCompilerRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue1807 {

  @Test
  public void shouldGenerateValidByteCode() throws Exception {
    new MvcModuleCompilerRunner(new C1807())
        .example(Expected1807.class)
        .module(
            app -> {
              Word1807 word = new Word1807();
              MockRouter router = new MockRouter(app);
              Multipart multipart = mock(Multipart.class);
              ValueNode missing = mock(ValueNode.class);
              when(missing.isMissing()).thenReturn(true);
              when(multipart.get("data")).thenReturn(missing);
              when(multipart.to(Word1807.class)).thenReturn(word);
              MockContext ctx = new MockContext();
              ctx.setMultipart(multipart);

              assertEquals(word, router.post("/test/bug", ctx).value());
            });
  }
}
