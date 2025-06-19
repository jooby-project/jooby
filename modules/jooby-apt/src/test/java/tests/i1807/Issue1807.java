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

import io.jooby.Formdata;
import io.jooby.Value;
import io.jooby.apt.ProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue1807 {

  @Test
  public void shouldGenerateValidByteCode() throws Exception {
    new ProcessorRunner(new C1807())
        .withRouter(
            app -> {
              Word1807 word = new Word1807();
              MockRouter router = new MockRouter(app);
              Formdata formdata = mock(Formdata.class);
              Value missing = mock(Value.class);
              when(missing.isMissing()).thenReturn(true);
              when(formdata.get("data")).thenReturn(missing);
              when(formdata.to(Word1807.class)).thenReturn(word);
              MockContext ctx = new MockContext();
              ctx.setForm(formdata);

              assertEquals(word, router.post("/test/bug", ctx).value());
            });
  }
}
