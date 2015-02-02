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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal.undertow;

import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.util.Headers;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Upload;
import org.jooby.internal.MutantImpl;
import org.jooby.internal.reqparam.RootParamConverter;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Injector;

public class UndertowUpload implements Upload {

  private Injector injector;

  private FormValue value;

  private Supplier<MediaType> type;

  public UndertowUpload(final Injector injector, final FormValue value) {
    this.injector = injector;
    this.value = value;
    this.type = Suppliers.memoize(
        () -> Optional
            .ofNullable(value.getHeaders())
            .map(headers ->
                Optional.ofNullable(headers.get(Headers.CONTENT_TYPE))
                    .map(h -> MediaType.valueOf(h.getFirst()))
                    .orElse(MediaType.byPath(value.getFileName())
                        .orElse(MediaType.octetstream)
                    )
            ).orElse(MediaType.octetstream)
        );
  }

  @Override
  public void close() throws IOException {
    // undertow will delete the file, we don't have to worry about.
  }

  @Override
  public String name() {
    return value.getFileName();
  }

  @Override
  public MediaType type() {
    return type.get();
  }

  @Override
  public Mutant header(final String name) {
    List<String> headers = Optional
        .ofNullable(value.getHeaders())
        .<List<String>> map(
            h -> Optional.<List<String>> ofNullable(h.get(name))
                .orElse(Collections.<String> emptyList()))
        .orElse(Collections.<String> emptyList());
    return new MutantImpl(injector.getInstance(RootParamConverter.class), headers);
  }

  @Override
  public File file() throws IOException {
    return value.getFile();
  }

}
