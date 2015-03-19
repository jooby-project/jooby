package org.jooby;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.inject.TypeLiteral;

public class BodyConverters {

  public static final BodyParser fromJson = new BodyParser() {

    @Override
    public boolean canParse(final TypeLiteral<?> type) {
      return true;
    }

    @Override
    public <T> T parse(final TypeLiteral<T> type, final BodyParser.Context ctx) throws Exception {
      return ctx.text(r -> CharStreams.toString(r));
    }

    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.json);
    }

  };

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
