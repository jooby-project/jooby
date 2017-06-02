/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.spec;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.google.common.base.Throwables;

public class SourceResolverImpl implements SourceResolver {

  private Path basedir;

  public SourceResolverImpl(final Path basedir) {
    this.basedir = basedir;
  }

  @Override
  public Optional<Reader> resolveSource(final Type type) {
    Path src = typeToPath(type.getTypeName());
    try {
      return Files.walk(basedir)
          .filter(p -> p.toString().endsWith(src.toString()))
          .map(this::reader)
          .findFirst();
    } catch (IOException ex) {
    }
    return Optional.empty();
  }

  private Reader reader(final Path path) {
    try {
      return new FileReader(path.toFile());
    } catch (FileNotFoundException ex) {
      throw Throwables.propagate(ex);
    }
  }

  private Path typeToPath(final String qn) {
    String cname = qn;
    int idx = cname.indexOf('$');
    if (idx > 0) {
      cname = cname.substring(0, idx);
    }
    String[] name = cname.split("\\.");
    name[name.length - 1] = name[name.length - 1] + ".java";
    Path src = Paths.get(name[0]);
    for (int i = 1; i < name.length; i++) {
      src = src.resolve(name[i]);
    }
    return src;
  }

}
