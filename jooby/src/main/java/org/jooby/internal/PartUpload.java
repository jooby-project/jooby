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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;

import javax.servlet.http.Part;

import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Upload;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;

class PartUpload implements Upload {

  private Part part;

  private String workDir;

  private MediaType type;

  private Injector injector;

  private Charset charset;

  public PartUpload(final Injector injector, final Part part, final Charset charset,
      final String workDir) {
    this.injector = injector;
    this.part = part;
    this.charset = charset;
    this.workDir = workDir;
    this.type = MediaType.byPath(part.getSubmittedFileName()).orElse(MediaType.octetstream);
  }

  @Override
  public String name() {
    return part.getSubmittedFileName();
  }

  @Override
  public MediaType type() {
    return type;
  }

  @Override
  public Mutant header(final String name) {
    Collection<String> headers = part.getHeaders(name);
    return new MutantImpl(injector, name, ImmutableList.copyOf(headers), MediaType.all, charset);
  }

  @Override
  public File file() throws IOException {
    String name = System.currentTimeMillis() + "." + name();
    File fout = new File(workDir, name);
    part.write(fout.getAbsolutePath());
    return fout;
  }

  @Override
  public void close() throws IOException {
    part.delete();
  }

}
