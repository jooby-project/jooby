package org.jooby.integration;

import java.util.List;

import org.jooby.Body;
import org.jooby.MediaType;
import org.jooby.View;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.inject.TypeLiteral;

public class BodyConverters {

  public static final Body.Parser fromJson = new Body.Parser() {

    @Override
    public boolean canParse(final TypeLiteral<?> type) {
      return true;
    }

    @Override
    public <T> T parse(final TypeLiteral<T> type, final Body.Reader reader) throws Exception {
      return reader.text(r -> CharStreams.toString(r));
    }

    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.json);
    }

  };

  public static final Body.Formatter toJson = new Body.Formatter() {

    @Override
    public boolean canFormat(final Class<?> type) {
      return true;
    }

    @Override
    public void format(final Object body, final Body.Writer writer)
        throws Exception {
      writer.text(w -> w.write("{\"body\": \"" + body + "\"}"));
    }

    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.json);
    }
  };

  public static final Body.Formatter toHtml = new View.Engine() {
    @Override
    public void render(final View viewable, final Body.Writer writer) throws Exception {
      writer.text(w -> w.write("<html><body>" + viewable + "</body></html>"));
    }
  };
}
