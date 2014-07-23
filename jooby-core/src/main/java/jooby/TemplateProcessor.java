package jooby;

import java.lang.reflect.Type;
import java.util.List;

import com.google.common.collect.ImmutableList;

public abstract class TemplateProcessor implements BodyConverter {

  public static final String VIEW_NAME = "@" + TemplateProcessor.class.getName() + "#vieName";

  private final List<MediaType> types;

  public TemplateProcessor(final MediaType... mediaTypes) {
    this.types = ImmutableList.copyOf(mediaTypes);
  }

  public TemplateProcessor() {
    this.types = ImmutableList.of(MediaType.html);
  }

  @Override
  public boolean canRead(final Type type) {
    return false;
  }

  @Override
  public boolean canWrite(final Class<?> type) {
    return true;
  }

  @Override
  public final List<MediaType> types() {
    return types;
  }

  @Override
  public final <T> T read(final Class<T> type, final BodyReader reader) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(final Object message, final BodyWriter writer) throws Exception {
    // wrap a viewable if need it
    final Viewable viewable;
    if (message instanceof Viewable) {
      viewable = (Viewable) message;
    } else {
      String viewName = writer.header(VIEW_NAME).getOptional(String.class)
          .orElseThrow(() -> new IllegalStateException("Unable to rendering: '" + message
              + "' as a view"));
      viewable = new Viewable(viewName, message);
    }
    render(viewable, writer);
  }

  public abstract void render(final Viewable viewable, final BodyWriter writer)
      throws Exception;

}
