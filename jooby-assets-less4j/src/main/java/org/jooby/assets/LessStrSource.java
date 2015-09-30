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
package org.jooby.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sommeri.less4j.LessSource;
import com.github.sommeri.less4j.LessSource.StringSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

public class LessStrSource extends StringSource {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private String path;

  public LessStrSource(final String content, final String name) {
    super(content, name);
    this.path = spath(FilenameUtils.getPathNoEndSeparator(name));
  }

  @Override
  public LessSource relativeSource(final String filename) throws StringSourceException {
    return source(spath(filename));
  }

  private LessSource source(final String filename) throws StringSourceException {
    String[] files = {path + filename, filename };
    for (String file : files) {
      InputStream stream = getClass().getResourceAsStream(file);
      if (stream != null) {
        try {
          return new LessStrSource(
              new String(ByteStreams.toByteArray(stream), StandardCharsets.UTF_8), file);
        } catch (IOException ex) {
          log.debug("Can't read file: " + path, ex);
          throw new StringSourceException();
        } finally {
          Closeables.closeQuietly(stream);
        }
      }
    }
    throw new StringSourceException();
  }

  private String spath(final String filename) {
    return filename.startsWith("/") ? filename : "/" + filename;
  }
}
