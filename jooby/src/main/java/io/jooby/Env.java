package io.jooby;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

public class Env extends Value.Object {

  private final String name;

  private final Map<String, String> properties = new LinkedHashMap<>();

  public Env(final String name) {
    this.name = name;
  }

  @Override public Value get(@Nonnull String name) {
    Value result = super.get(name);
    if (result.isMissing()) {
      // fallback to full path access
      String value = properties.get(name);
      if (value != null) {
        result = Value.value(name, value);
      }
    }
    return result;
  }

  public @Nonnull String name() {
    return name;
  }

  public static Map<String, String> parse(String... args) {
    if (args == null || args.length == 0) {
      return Collections.emptyMap();
    }
    Map<String, String> conf = new HashMap<>();
    for (String arg : args) {
      int eq = arg.indexOf('=');
      if (eq > 0) {
        conf.put(arg.substring(0, eq).trim(), arg.substring(eq + 1).trim());
      } else {
        // must be the environment name
        conf.putIfAbsent("application.env", arg);
      }
    }
    return conf;
  }

  public static Map<String, String> load(ClassLoader loader, String filename) {
    return load(loader, filename, stream -> {
      Map<String, String> result = new LinkedHashMap<>();
      Properties properties = new Properties();
      properties.load(stream);
      properties.forEach((k, v) -> result.put(k.toString(), v.toString()));
      return result;
    });
  }

  public static Map<String, String> load(ClassLoader loader, String filename,
      Throwing.Function<InputStream, Map<String, String>> propertyLoader) {
    InputStream stream = findProperties(loader, filename);
    if (stream != null) {
      try {
        return propertyLoader.apply(stream);
      } finally {
        try {
          stream.close();
        } catch (IOException x) {
          throw Throwing.sneakyThrow(x);
        }
      }
    }
    return Collections.emptyMap();
  }

  public static Env defaultEnvironment(String... args) {
    ClassLoader classLoader = Env.class.getClassLoader();
    Map<String, String> argMap = parse(args);
    String env = argMap.getOrDefault("application.env", "dev");
    List<Map> sources = new LinkedList<>();
    Stream
        .of("application.properties", "application." + env + ".properties")
        .map(filename -> load(classLoader, filename))
        .filter(props -> props.size() > 0)
        .forEach(sources::add);
    sources.add(System.getProperties());
    sources.add(System.getenv());
    if (argMap.size() > 0) {
      sources.add(argMap);
    }
    return build(sources.toArray(new Map[sources.size()]));
  }

  public static Env build(Map<String, String>... sources) {
    Env env = new Env(environmentName(sources));
    for (Map<String, String> source : sources) {
      env.properties.putAll(source);
    }
    env.properties.forEach((k, v) -> {
      try {
        String[] values = v.split(",");
        for (String value : values) {
          env.put(k, value.trim());
        }
      } catch (ClassCastException x) {
        // Caused by system properties like:
        // java.vendor.url vs java.vendor.url.bug
      }
    });
    return env;
  }

  private static InputStream findProperties(ClassLoader loader, String filename) {
    try {
      String envdir = System.getProperty("env.dir", "conf");
      Path file = Paths.get(System.getProperty("user.dir"), envdir, filename).toAbsolutePath();
      if (Files.exists(file)) {
        return new FileInputStream(file.toFile());
      }
      return loader.getResourceAsStream(envdir + "/" + filename);
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  private static String environmentName(Map<String, String>[] sources) {
    for (int i = sources.length - 1; i >= 0; i--) {
      Map<String, String> properties = sources[i];
      String name = properties.get("application.env");
      if (name != null) {
        return name.toLowerCase();
      }
    }
    return "dev";
  }

}
