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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Environment {

  private final List<String> actives;

  private final Config conf;

  public Environment(@Nonnull final Config conf, @Nonnull final String... actives) {
    this.actives = Stream.of(actives)
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toList());
    this.conf = conf;
  }

  public @Nonnull Config getConfig() {
    return conf;
  }

  public @Nonnull List<String> getActiveNames() {
    return Collections.unmodifiableList(actives);
  }

  public boolean isActive(String name, String... names) {
    return this.actives.contains(name.toLowerCase())
        || Stream.of(names).map(String::toLowerCase).anyMatch(this.actives::contains);
  }

  @Override public String toString() {
    return actives + "\n" + toString(conf).trim();
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

  public static @Nonnull Config systemProperties() {
    return ConfigFactory.parseProperties(System.getProperties(),
        ConfigParseOptions.defaults().setOriginDescription("system properties"));
  }

  public static @Nonnull Config systemEnv() {
    return ConfigFactory.systemEnvironment();
  }

  public static @Nonnull Environment loadEnvironment(@Nonnull EnvironmentOptions options) {
    Config sys = systemProperties()
        .withFallback(systemEnv());

    String[] actives = options.getActiveNames();
    String filename = options.getFilename();
    String extension;
    int ext = filename.lastIndexOf('.');
    if (ext <= 0) {
      extension = ".conf";
    } else {
      extension = filename.substring(ext);
      filename = filename.substring(0, ext);
    }
    String basedir = options.getBasedir();
    Path userdir = Paths.get(System.getProperty("user.dir"));
    /** Application file: */
    Config application = ConfigFactory.empty();
    String[] names = new String[actives.length + 1];
    for (int i = 0; i < actives.length; i++) {
      names[i] = filename + "." + actives[i].trim().toLowerCase() + extension;
    }
    names[actives.length] = filename + extension;
    Path fsroot = Paths.get(basedir).toAbsolutePath();
    String[] cproot = basedir.split("/");
    for (String name : names) {
      Path fsfile = fsroot.resolve(name);
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
        it = ConfigFactory.parseResources(options.getClassLoader(), cpfile,
            ConfigParseOptions.defaults().setOriginDescription("classpath://" + cpfile));
      }
      application = application.withFallback(it);
    }

    Config result = sys
        .withFallback(application)
        .withFallback(defaults())
        .resolve();

    return new Environment(result, actives);
  }

  public static @Nonnull Config defaults() {
    Path tmpdir = Paths.get(System.getProperty("user.dir"), "tmp");
    Map<String, String> defaultMap = new HashMap<>();
    defaultMap.put("application.tmpdir", tmpdir.toString());
    defaultMap.put("application.charset", "UTF-8");
    defaultMap.put("server.maxRequestSize", Integer.toString(ServerOptions._10MB));
    String pid = pid();
    if (pid != null) {
      System.setProperty("PID", pid);
      defaultMap.put("pid", pid);
    }

    return ConfigFactory.parseMap(defaultMap, "defaults");
  }

  public static String pid() {
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
