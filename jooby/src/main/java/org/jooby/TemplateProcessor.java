package org.jooby;

import java.util.List;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.inject.TypeLiteral;

/**
 * Special {@link BodyConverter} for dealing template engines.
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
public abstract class TemplateProcessor implements BodyConverter {

  private final List<MediaType> types;

  public TemplateProcessor(final MediaType... mediaTypes) {
    this.types = ImmutableList.copyOf(mediaTypes);
  }

  public TemplateProcessor() {
    this.types = ImmutableList.of(MediaType.html);
  }

  @Override
  public final boolean canRead(final TypeLiteral<?> type) {
    return false;
  }

  @Override
  public final boolean canWrite(final Class<?> type) {
    return type.isAssignableFrom(Viewable.class);
  }

  @Override
  public final List<MediaType> types() {
    return types;
  }

  @Override
  public final <T> T read(final TypeLiteral<T> type, final BodyReader reader) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(final Object body, final BodyWriter writer) throws Exception {
    final Viewable viewable = (Viewable) body;
    render(viewable, writer);
  }

  /**
   * Render a view.
   *
   * @param viewable View to render.
   * @param writer A body writer.
   * @throws Exception If view rendering fails.
   */
  public abstract void render(final Viewable viewable, final BodyWriter writer) throws Exception;

}
