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
package io.jooby.gradle;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class RunTaskConfig {

  private List<String> restartExtensions = Arrays.asList("conf", "properties", "class");

  private List<String> compileExtensions = Arrays.asList("java", "kt");

  private int port = 8080;

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public List<String> getRestartExtensions() {
    return restartExtensions;
  }

  public void setRestartExtensions(List<String> restartExtensions) {
    this.restartExtensions = restartExtensions;
  }

  public List<String> getCompileExtensions() {
    return compileExtensions;
  }

  public void setCompileExtensions(List<String> compileExtensions) {
    this.compileExtensions = compileExtensions;
  }

  public boolean isCompileExtension(Path path) {
    return containsExtension(compileExtensions, path);
  }

  public boolean isRestartExtension(Path path) {
    return containsExtension(restartExtensions, path);
  }

  private boolean containsExtension(List<String> extensions, Path path) {
    String filename = path.getFileName().toString();
    return extensions.stream().anyMatch(ext -> filename.endsWith("." + ext));
  }
}
