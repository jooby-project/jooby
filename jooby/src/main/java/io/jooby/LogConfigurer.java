/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;

public final class LogConfigurer {
  private LogConfigurer() {
  }

  public static void configure(@Nonnull String... names) {
    String[] keys = {"logback.configurationFile", "log4j.configurationFile"};
    for (String key : keys) {
      String file = property(key);
      if (file != null) {
        // found, reset and exit
        System.setProperty(key, file);
        return;
      }
    }
    Path userdir = Paths.get(System.getProperty("user.dir"));
    Map<String, List<Path>> options = new LinkedHashMap<>();
    options.put("logback.configurationFile", logbackFiles(userdir, names));
    options.put("log4j.configurationFile", log4jFiles(userdir, names));
    for (Map.Entry<String, List<Path>> entry : options.entrySet()) {
      Optional<Path> logfile = entry.getValue().stream().filter(Files::exists)
          .findFirst()
          .map(Path::toAbsolutePath);
      if (logfile.isPresent()) {
        System.setProperty(entry.getKey(), logfile.get().toString());
        break;
      }
    }
  }

  private static List<Path> logbackFiles(Path basedir, String[] env) {
    return logFile(basedir, env, "logback", ".xml");
  }

  private static List<Path> logFile(Path basedir, String[] names, String name, String ext) {
    Path confdir = basedir.resolve("conf");
    List<Path> result = new ArrayList<>();
    for (String env : names) {
      String envlogfile = name + "." + env + ext;
      result.add(confdir.resolve(envlogfile));
      result.add(basedir.resolve(envlogfile));
    }
    String logfile = name + ext;
    result.add(confdir.resolve(logfile));
    result.add(basedir.resolve(logfile));
    return result;
  }

  private static List<Path> log4jFiles(Path basedir, String[] names) {
    List<Path> result = new ArrayList<>();
    String[] extensions = {".properties", ".xml", ".yaml", ".yml", ".json"};
    for (String extension : extensions) {
      result.addAll(logFile(basedir, names, "log4j", extension));
      result.addAll(logFile(basedir, names, "log4j2", extension));
    }
    return result;
  }

  private static String property(String name) {
    return System.getProperty(name, System.getenv(name));
  }
}
