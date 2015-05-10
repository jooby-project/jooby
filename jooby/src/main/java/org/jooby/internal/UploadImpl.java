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
import java.io.IOException;

import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Upload;
import org.jooby.internal.reqparam.ParserExecutor;
import org.jooby.spi.NativeUpload;

import com.google.inject.Injector;

public class UploadImpl implements Upload {

  private Injector injector;

  private NativeUpload upload;

  public UploadImpl(final Injector injector, final NativeUpload upload) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.upload = requireNonNull(upload, "An upload is required.");
  }

  @Override
  public void close() throws IOException {
    upload.close();
  }

  @Override
  public String name() {
    return upload.name();
  }

  @Override
  public MediaType type() {
    return header("Content-Type").toOptional(MediaType.class)
        .orElseGet(() -> MediaType.byPath(name()).orElse(MediaType.octetstream));
  }

  @Override
  public Mutant header(final String name) {
    return new MutantImpl(injector.getInstance(ParserExecutor.class), upload.headers(name));
  }

  @Override
  public File file() throws IOException {
    return upload.file();
  }

  @Override
  public String toString() {
    return name();
  }

}
