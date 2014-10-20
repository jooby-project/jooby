package org.jooby;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

/**
 * Found a {@link MediaType} using a file extension.
 * Media types are defined in a <code>*.conf</code> file. If you need/want to add/override a media
 * type just add an entry to your <code>*.conf</code> file, like:
 * <pre>
 *  // application.conf
 *  mime.[myext] = mime/type
 * </pre>

 * @author edgar
 * @since 0.1.0
 */
@Singleton
public class MediaTypeProvider {

  /**
   * The application config object.
   */
  private Config config;

  /**
   * Creates a new {@link MediaTypeProvider}.
   *
   * @param config A configuration object.
   */
  @Inject
  public MediaTypeProvider(@Nonnull final Config config) {
    this.config = requireNonNull(config, "A config is required.");
  }

  /**
   * Produces a map where keys are file extensions and values are {@link MediaType mediaTypes}.
   *
   * @return A map with file extensions and media types.
   */
  public Map<String, MediaType> types() {
    Config $ = config.getConfig("mime");
    Map<String, MediaType> types = new HashMap<>();
    $.entrySet().forEach(entry -> {
      types.put(entry.getKey(), MediaType.valueOf(entry.getValue().unwrapped().toString()));
    });
    return types;
  }

  /**
   * Get a {@link MediaType} for a file. It returns an associated {@link MediaType} or
   * {@link MediaType#octetstream} if none is found.
   *
   * @param file A candidate file.
   * @return A {@link MediaType} or {@link MediaType#octetstream} for unknown file extensions.
   */
  public MediaType forFile(@Nonnull final File file) {
    requireNonNull(file, "A file is required.");
    return forPath(file.getAbsolutePath());
  }

  /**
   * Get a {@link MediaType} for a file path. It returns an associated {@link MediaType} or
   * {@link MediaType#octetstream} if none is found.
   *
   * @param path A candidate file path.
   * @return A {@link MediaType} or {@link MediaType#octetstream} for unknown file extensions.
   */
  public MediaType forPath(final String path) {
    requireNonNull(path, "A path is required.");
    try {
      int idx = path.lastIndexOf('.');
      String ext = path.substring(idx + 1);
      return MediaType.valueOf(config.getString("mime." + ext));
    } catch (IndexOutOfBoundsException | ConfigException.Missing ex) {
      return MediaType.octetstream;
    }
  }

  /**
   * Get a {@link MediaType} for a file extension. It returns an associated {@link MediaType} or
   * {@link MediaType#octetstream} if none is found.
   *
   * @param ext A file extension, like <code>js</code> or <code>css</code>.
   * @return A {@link MediaType} or {@link MediaType#octetstream} for unknown file extensions.
   */
  public MediaType forExtension(final String ext) {
    requireNonNull(ext, "An ext is required.");
    try {
      return MediaType.valueOf(config.getString("mime." + ext));
    } catch (ConfigException.Missing ex) {
      return MediaType.octetstream;
    }
  }

}
