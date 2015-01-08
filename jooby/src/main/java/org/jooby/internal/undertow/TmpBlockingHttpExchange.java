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

import io.undertow.io.BlockingSenderImpl;
import io.undertow.io.Sender;
import io.undertow.io.UndertowInputStream;
import io.undertow.server.BlockingHttpExchange;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TmpBlockingHttpExchange implements BlockingHttpExchange {

  private InputStream inputStream;
  private OutputStream outputStream;
  private Sender sender;
  private final HttpServerExchange exchange;

  TmpBlockingHttpExchange(final HttpServerExchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public InputStream getInputStream() {
    if (inputStream == null) {
      inputStream = new UndertowInputStream(exchange);
    }
    return inputStream;
  }

  @Override
  public OutputStream getOutputStream() {
    if (outputStream == null) {
      outputStream = new TmpOutputStream(exchange);
    }
    return outputStream;
  }

  @Override
  public Sender getSender() {
    if (sender == null) {
      sender = new BlockingSenderImpl(exchange, getOutputStream());
    }
    return sender;
  }

  @Override
  public void close() throws IOException {
    try {
      getInputStream().close();
    } finally {
      getOutputStream().close();
    }
  }

}
