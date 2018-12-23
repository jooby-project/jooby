/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Env extends Value.Object {

  public static class PropertySource {
    private String name;

    private Map<String, String> properties;

    public PropertySource(String name, Map properties) {
      this.name = name;
      this.properties = properties;
    }

    public String name() {
      return name;
    }

    public Map<String, String> properties() {
      return properties;
    }

    @Override public String toString() {
      return name;
    }
  }

  private final String name;

  private final List<PropertySource> sources = new ArrayList<>();

  public Env(final String name) {
    this.name = name;
  }

  @Override public Value get(@Nonnull String name) {
    Value result = super.get(name);
    if (result.isMissing()) {
      // fallback to full path access
      String value = fromSource(sources, name, null);
      if (value != null) {
        return Value.value(name, value);
      }
    }
    return result;
  }

  public @Nonnull String name() {
    return name;
  }

  @Override public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append(name).append("\n");
    String indent = "  ";
    for (int i = sources.size() - 1; i >= 0; i--) {
      String sname = sources.get(i).name;
      buff.append(indent).append("::").append(sname).append("\n");
      indent += "  ";
    }
    return buff.toString().trim();
  }

  public static PropertySource parse(String... args) {
    if (args == null || args.length == 0) {
      return new PropertySource("args", Collections.emptyMap());
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
    return new PropertySource("args", conf);
  }

  public static PropertySource load(ClassLoader loader, String filename) {
    return load(loader, filename, (name, stream) -> {
      try {
        Properties properties = new Properties();
        properties.load(stream);
        return new PropertySource(name, properties);
      } catch (IOException x) {
        throw Throwing.sneakyThrow(x);
      }
    });
  }

  public static PropertySource load(ClassLoader loader, String filename,
      BiFunction<String, InputStream, PropertySource> propertyLoader) {
    AtomicReference<String> fullpath = new AtomicReference<>();
    InputStream stream = findProperties(loader, filename, fullpath::set);
    if (stream != null) {
      try {
        return propertyLoader.apply(fullpath.get(), stream);
      } finally {
        try {
          stream.close();
        } catch (IOException x) {
          throw Throwing.sneakyThrow(x);
        }
      }
    }
    return new PropertySource(filename, Collections.emptyMap());
  }

  public static PropertySource systemProperties() {
    return new PropertySource("systemProperties", System.getProperties());
  }

  public static PropertySource systemEnv() {
    return new PropertySource("systemEnv", System.getenv());
  }

  public static Env defaultEnvironment(String... args) {
    ClassLoader classLoader = Env.class.getClassLoader();
    PropertySource argMap = parse(args);
    String env = argMap.properties.getOrDefault("application.env", "dev");
    List<PropertySource> sources = new LinkedList<>();
    Stream
        .of("application.properties", "application." + env + ".properties")
        .map(filename -> load(classLoader, filename))
        .filter(props -> props.properties().size() > 0)
        .forEach(sources::add);
    sources.add(systemProperties());
    sources.add(systemEnv());
    sources.add(argMap);
    return build(sources.toArray(new PropertySource[sources.size()]));
  }

  public static Env build(PropertySource... sources) {
    Env env = new Env(fromSource(Arrays.asList(sources), "application.env", "dev").toLowerCase());
    /** Merge to build values: */
    Map<String, String> merged = new LinkedHashMap<>();
    String tmpdir = System.getProperty("java.io.tmpdir");
    PropertySource defaults = new PropertySource("defaults", Map.of("application.tmpdir", tmpdir));
    merged.putAll(defaults.properties);
    for (PropertySource source : sources) {
      merged.putAll(source.properties);
    }
    List<String> skip = Arrays.asList("java.", "sun.");
    merged.forEach((k, v) -> {
      if (skip.stream().anyMatch(it -> !it.startsWith(k))) {
        // ignore java. and sun. properties (too many, not worth it)
        try {
          String[] values = v.split(",");
          for (String value : values) {
            env.put(k, value.trim());
          }
        } catch (ClassCastException x) {
          // Caused by system properties like:
          // java.vendor.url vs java.vendor.url.bug
        }
      }
    });
    // Only worth it property from java.*
    env.put("java.io.tmpdir", tmpdir);

    merged.clear();

    /** Add sources */
    env.sources.add(defaults);
    Stream.of(sources).forEach(env.sources::add);
    return env;
  }

  private static InputStream findProperties(ClassLoader loader, String filename,
      Consumer<String> fullpath) {
    try {
      String envdir = System.getProperty("env.dir", "conf");
      Path file = Paths.get(System.getProperty("user.dir"), envdir, filename).toAbsolutePath();
      if (Files.exists(file)) {
        fullpath.accept(file.toString());
        return new FileInputStream(file.toFile());
      }
      String resource = envdir + "/" + filename;
      fullpath.accept(resource);
      return loader.getResourceAsStream(resource);
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  private static String fromSource(List<PropertySource> sources, String name, String defaults) {
    for (int i = sources.size() - 1; i >= 0; i--) {
      PropertySource source = sources.get(i);
      String value = source.properties().get(name);
      if (value != null) {
        return value;
      }
    }
    return defaults;
  }
}
