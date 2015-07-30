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

import java.io.InputStream;

import org.jooby.Asset;
import org.jooby.MediaType;

public class InputStreamAsset implements Asset {

  private InputStream stream;

  private String name;

  private MediaType type;

  public InputStreamAsset(final InputStream stream, final String name, final MediaType type) {
    this.stream = requireNonNull(stream, "InputStream is required.");
    this.name = requireNonNull(name, "Name is required.");
    this.type = requireNonNull(type, "Type is required.");
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String path() {
    return name;
  }

  @Override
  public long length() {
    return -1;
  }

  @Override
  public long lastModified() {
    return -1;
  }

  @Override
  public InputStream stream() throws Exception {
    return stream;
  }

  @Override
  public MediaType type() {
    return type;
  }

}
