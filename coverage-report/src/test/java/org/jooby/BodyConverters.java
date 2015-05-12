package org.jooby;

public class BodyConverters {

  public static final Parser fromJson = (type, ctx) -> ctx.body(body -> body.text());

  public static final Renderer toJson = (body, ctx) -> {
    if (ctx.accepts("json")) {
      ctx.type(MediaType.json);
      ctx.text(w -> w.write("{\"body\": \"" + body + "\"}"));
    }
  };

  public static final Renderer toHtml = (viewable, ctx) -> {
    if (ctx.accepts("html")) {
      ctx.type(MediaType.html);
      ctx.text(w -> w.write("<html><body>" + viewable + "</body></html>"));
    }
  };
}
