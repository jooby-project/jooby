package tests.i1807;

import io.jooby.MockContext;
import io.jooby.MockRouter;
import io.jooby.Multipart;
import io.jooby.apt.MvcModuleCompilerRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Issue1807 {

  @Test
  public void shouldGenerateValidByteCode() throws Exception {
    new MvcModuleCompilerRunner(new C1807())
        .example(Expected1807.class)
        .module(app -> {
          Word1807 word = new Word1807();
          MockRouter router = new MockRouter(app);
          Multipart multipart = mock(Multipart.class);
          when(multipart.to(Word1807.class)).thenReturn(word);
          MockContext ctx = new MockContext();
          ctx.setMultipart(multipart);

          assertEquals(word, router.post("/test/bug", ctx).value());
        });
  }

}
