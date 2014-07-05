package jooby;

import java.io.IOException;
import java.util.List;

import jooby.mvc.Viewable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public abstract class TemplateProcessor implements MessageConverter {

  public static final String VIEW_NAME = "@" + TemplateProcessor.class.getName() + "#vieName";

  private final List<MediaType> types;

  public TemplateProcessor(final MediaType... mediaTypes) {
    this.types = ImmutableList.copyOf(mediaTypes);
  }

  public TemplateProcessor() {
    this.types = MediaType.HTML;
  }

  @Override
  public final List<MediaType> types() {
    return types;
  }

  @Override
  public final <T> T read(final Class<T> type, final MessageReader reader) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(final Object message, final MessageWriter writer,
      final Multimap<String, String> headers) throws IOException {
    // wrap a viewable if need it
    final Viewable viewable;
    if (message instanceof Viewable) {
      viewable = (Viewable) message;
    } else {
      viewable = new Viewable(headers.get(VIEW_NAME).iterator().next(), message);
    }
    render(viewable, writer);
  }

  public abstract void render(final Viewable viewable, final MessageWriter writer)
      throws IOException;

}
