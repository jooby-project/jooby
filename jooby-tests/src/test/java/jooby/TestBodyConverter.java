package jooby;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.inject.TypeLiteral;

public class TestBodyConverter {

  public static final BodyConverter JSON = new BodyConverter() {
    @Override
    public boolean canRead(final TypeLiteral<?> type) {
      return true;
    }

    @Override
    public boolean canWrite(final Class<?> type) {
      return true;
    }

    @Override
    public <T> T read(final TypeLiteral<T> type, final BodyReader reader) throws Exception {
      return reader.text(r -> CharStreams.toString(r));
    }

    @Override
    public void write(final Object body, final BodyWriter writer)
        throws Exception {
      writer.text(w -> w.write("{\"body\": \"" + body + "\"}"));
    }

    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.json);
    }
  };

  public static final BodyConverter HTML = new TemplateProcessor() {
    @Override
    public void render(final Viewable viewable, final BodyWriter writer) throws Exception {
      writer.text(w -> w.write("<html><body>" + viewable + "</body></html>"));
    }
  };
}
