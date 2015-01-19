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
package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jooby.Asset;
import org.jooby.MediaType;

class FileAsset implements Asset {

  private File file;

  private MediaType contentType;

  public FileAsset(final File file, final MediaType contentType) {
    this.file = requireNonNull(file, "A file is required.");
    this.contentType = requireNonNull(contentType, "The contentType is required.");
  }

  @Override
  public String name() {
    return file.getName();
  }

  @Override
  public InputStream stream() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public long length() {
    return file.length();
  }

  @Override
  public long lastModified() {
    return file.lastModified();
  }

  @Override
  public MediaType type() {
    return contentType;
  }

  @Override
  public String toString() {
    return name() + "(" + type() + ")";
  }
}
