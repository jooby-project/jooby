package org.jooby;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import com.google.common.annotations.Beta;

/**
 * Usually a public file/resource like javascript, css, images files, etc...
 * An asset consist of content type, stream and last modified since, between others.
 *
 * @author edgar
 * @since 0.1.0
 * @see Jooby#assets(String)
 */
@Beta
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
