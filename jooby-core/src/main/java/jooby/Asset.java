package jooby;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

/**
 * An asset is a public file or resource like javascript, css, images files, etc...
 * An asset must provide a content type, stream and last modified since, between others.
 *
 * @author edgar
 * @since 0.1.0
 * @see Jooby#assets(String)
 */
public interface Asset {

  /**
   * @return The asset name (without path).
   */
  @Nonnull String name();

  /**
   * @return The last modified date if possible or -1 when isn't.
   */
  long lastModified();

  /**
   * @return The content of this asset.
   * @throws IOException If content can't be read it.
   */
  @Nonnull InputStream stream() throws IOException;

  /**
   * @return Asset media type.
   */
  @Nonnull MediaType type();
}
