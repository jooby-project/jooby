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
package io.jooby.run;

import java.nio.file.Files;
import java.nio.file.Path;

public class Dependency implements Comparable<Dependency> {
  private String name;

  private Path location;

  public Dependency(Path location) {
    if (Files.exists(location)) {
      this.location = location.toAbsolutePath();
      this.name = name(location);
    } else {
      throw new IllegalArgumentException("File not found: " + location.toAbsolutePath().toString());
    }
  }

  public String getName() {
    return name;
  }

  public Path getLocation() {
    return location;
  }

  @Override public int compareTo(Dependency o) {
    return name.compareTo(o.name);
  }

  @Override public String toString() {
    return location.toString();
  }

  @Override public boolean equals(Object obj) {
    if (obj instanceof Dependency) {
      Dependency that = (Dependency) obj;
      return name.equals(that.name);
    }
    return false;
  }

  @Override public int hashCode() {
    return name.hashCode();
  }

  private static String name(Path path) {
    String filename = path.getFileName().toString();
    if (Files.isRegularFile(path) && !Files.isDirectory(path)) {
      int ext = filename.lastIndexOf('.');
      if (ext > 0) {
        filename = filename.substring(0, ext);
      }
    }
    return filename;
  }
}
