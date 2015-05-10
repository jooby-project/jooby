package org.jooby;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class BodyConverters {

  public static final Parser fromJson = (type, ctx) -> ctx.body(body -> body.text());

  public static final BodyFormatter toJson = new BodyFormatter() {

    @Override
    public boolean canFormat(final Class<?> type) {
      return true;
    }

    @Override
    public void format(final Object body, final BodyFormatter.Context writer)
        throws Exception {
      writer.text(w -> w.write("{\"body\": \"" + body + "\"}"));
    }

    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.json);
    }
  };

  public static final BodyFormatter toHtml = new View.Engine() {
    @Override
    public void render(final View viewable, final BodyFormatter.Context writer) throws Exception {
      writer.text(w -> w.write("<html><body>" + viewable + "</body></html>"));
    }
  };
}
