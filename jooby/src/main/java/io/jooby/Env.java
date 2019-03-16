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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

import javax.annotation.Nonnull;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Env {

  public static final String KEY = "application.env";

  public static class Builder {

    private String basedir;

    private String filename = "application";

    private String extension = "conf";

    public Builder basedir(@Nonnull String basedir) {
      this.basedir = basedir;
      return this;
    }

    public Builder basedir(@Nonnull Path basedir) {
      this.basedir = basedir.toAbsolutePath().toString();
      return this;
    }

    public Builder filename(@Nonnull String filename) {
      this.filename = filename;
      return this;
    }

    public Builder extension(@Nonnull String extension) {
      this.extension = extension;
      return this;
    }

    public Env build(@Nonnull ClassLoader loader, @Nonnull String env) {
      Config sysprops = systemProperties();

      /** Application file: */
      Config application = ConfigFactory.empty();
      String[][] names = {
          {filename + "." + env.toLowerCase() + "." + extension},
          {filename + "." + extension}
      };
      Path userdir = Paths.get(System.getProperty("user.dir"));
      Path confdir = userdir.resolve("conf");
      Path defdir = Files.exists(confdir) ? confdir : userdir;
      Path fsroot = this.basedir == null ? defdir : Paths.get(this.basedir).toAbsolutePath();
      String[] cproot = this.basedir == null ? new String[0] : this.basedir.split("/");
      for (String[] name : names) {
        Path fsfile = Stream.of(name).reduce(fsroot, Path::resolve, Path::resolve);
        Config it;
        if (Files.exists(fsfile)) {
          String origin = fsfile.startsWith(userdir)
              ? userdir.relativize(fsfile).toString()
              : fsfile.toString();
          it = ConfigFactory.parseFile(fsfile.toFile(),
              ConfigParseOptions.defaults().setOriginDescription(origin));
        } else {
          String cpfile = Stream.concat(Stream.of(cproot), Stream.of(name))
              .collect(Collectors.joining("/"));
          it = ConfigFactory.parseResources(loader, cpfile,
              ConfigParseOptions.defaults().setOriginDescription("classpath://" + cpfile));
        }
        application = application.withFallback(it);
      }

      Config result = sysprops
          .withFallback(systemEnv())
          .withFallback(application)
          .withFallback(defaults())
          .resolve();

      return new Env(result.getString(KEY), result);
    }
  }

  private final String name;

  private final Config conf;

  public Env(@Nonnull final String name, @Nonnull final Config conf) {
    this.name = name.toLowerCase();
    this.conf = conf;
  }

  public Config conf() {
    return conf;
  }

  public @Nonnull String name() {
    return name;
  }

  public boolean matches(String... names) {
    return Stream.of(names)
        .anyMatch(it -> it.equalsIgnoreCase(this.name));
  }

  public boolean matches(@Nonnull String name) {
    return this.name.equalsIgnoreCase(name);
  }

  public static Env empty(String name) {
    return new Env(name, ConfigFactory.empty());
  }

  @Override public String toString() {
    return name + "\n" + toString(conf).trim();
  }

  private String toString(final Config conf) {
    return configTree(conf.origin().description());
  }

  private String configTree(final String description) {
    return configTree(description.split(":\\s+\\d+,|,"), 0);
  }

  private String configTree(final String[] sources, final int i) {
    char[] pad = new char[i];
    Arrays.fill(pad, ' ');
    if (i < sources.length) {
      return new StringBuilder()
          .append(pad)
          .append("└── ")
          .append(sources[i].replace("merge of", "").trim())
          .append("\n")
          .append(configTree(sources, i + 1))
          .toString();
    }
    return "";
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

  public static Config systemProperties() {
    return ConfigFactory.parseProperties(System.getProperties(),
        ConfigParseOptions.defaults().setOriginDescription("system.properties"));
  }

  public static Config systemEnv() {
    return ConfigFactory.systemEnvironment();
  }

  public static Env defaultEnvironment(ClassLoader loader) {
    return new Builder().build(loader, System.getProperty(KEY, "dev"));
  }

  public static Builder create() {
    return new Builder();
  }

  public static Config defaults() {
    Path tmpdir = Paths.get(System.getProperty("user.dir"), "tmp");
    Map<String, String> defaultMap = new HashMap<>();
    defaultMap.put("application.tmpdir", tmpdir.toString());
    defaultMap.put("application.env", "dev");
    defaultMap.put("application.charset", "UTF-8");
    defaultMap.put("server.maxRequestSize", Integer.toString(Server._10MB));
    String pid = pid();
    if (pid != null) {
      System.setProperty("PID", pid);
      defaultMap.put("pid", pid);
    }

    return ConfigFactory.parseMap(defaultMap, "defaults");
  }

  private static String pid() {
    String pid = System.getenv().getOrDefault("PID", System.getProperty("PID"));
    if (pid == null) {
      pid = ManagementFactory.getRuntimeMXBean().getName();
      int i = pid.indexOf("@");
      if (i > 0) {
        pid = pid.substring(0, i);
      }
    }
    return pid;
  }
}
