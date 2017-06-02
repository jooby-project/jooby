package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.junit.Test;

import apps.App611;

public class Issue611Test extends RouteSpecTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void postWithQueryParamereters() throws Exception {
    routes(new RouteProcessor().process(new App611(), basedir))
        .next(r -> {
          assertEquals("POST", r.method());
          assertEquals("/friends/email", r.pattern());

          params(r.params())
              .next(p -> {
                assertEquals("userId", p.name());
                assertEquals("User ID.", p.doc().get());
                assertEquals(RouteParamType.QUERY, p.paramType());
                assertEquals(false, p.optional());
                assertEquals(Long.class, p.type());
              })
              .next(p -> {
                assertEquals("context", p.name());
                assertEquals("Context value.", p.doc().get());
                assertEquals(RouteParamType.QUERY, p.paramType());
                assertEquals(true, p.optional());
                assertEquals("java.util.Optional<java.lang.String>", p.type().getTypeName());
              }).next(p -> {
                assertEquals("emails", p.name());
                assertEquals("{@link List} of {@link String mails}.", p.doc().get());
                assertEquals(RouteParamType.BODY, p.paramType());
                assertEquals(false, p.optional());
                assertEquals("java.util.List<java.lang.String>",
                    p.type().getTypeName());
              });

          RouteResponse rsp = r.response();
          assertEquals("java.util.List<org.jooby.spec.User611>", rsp.type().getTypeName());
          assertEquals("Returns a {@link List} of {@link User611}.", rsp.doc().get());
        });
  }
}
