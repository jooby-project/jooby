package org.jooby;

public class BodyConverters {

  public static final Parser fromJson = (type, ctx) -> ctx.body(body -> body.text());

  public static final Renderer toJson = (body, ctx) -> {
    if (ctx.accepts("json")) {
      ctx.type("json")
          .send("{\"body\": \"" + body + "\"}");
    }
  };

  public static final Renderer toHtml = (viewable, ctx) -> {
    if (ctx.accepts("html")) {
      ctx.type("html")
          .send("<html><body>" + viewable + "</body></html>");
    }
  };
}
