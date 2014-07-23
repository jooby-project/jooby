package jooby;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

@Singleton
public class FileMediaTypeProvider {

  private Config config;

  @Inject
  public FileMediaTypeProvider(final Config config) {
    this.config = requireNonNull(config, "A config is required.");
  }

  public Map<String, MediaType> types() {
    Config $ = config.getConfig("mime");
    Map<String, MediaType> types = new HashMap<>();
    $.entrySet().forEach(entry -> {
      types.put(entry.getKey(), MediaType.valueOf(entry.getValue().unwrapped().toString()));
    });
    return types;
  }

  public MediaType typeFor(final File file) {
    requireNonNull(file, "A file is required.");
    return typeFor(file.getAbsolutePath());
  }

  public MediaType typeFor(final String path) {
    requireNonNull(path, "A path is required.");
    try {
      int idx = path.lastIndexOf('.');
      String ext = path.substring(idx + 1);
      return MediaType.valueOf(config.getString("mime." + ext));
    } catch (IndexOutOfBoundsException | ConfigException.Missing ex) {
      return MediaType.octetstream;
    }
  }

}
