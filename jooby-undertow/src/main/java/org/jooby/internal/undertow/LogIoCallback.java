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
package org.jooby.internal.undertow;

import java.io.IOException;

import org.jooby.spi.NativeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;

public class LogIoCallback implements IoCallback {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(NativeResponse.class);

  private IoCallback callback;

  public LogIoCallback(final IoCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onComplete(final HttpServerExchange exchange, final Sender sender) {
    callback.onComplete(exchange, sender);
  }

  @Override
  public void onException(final HttpServerExchange exchange, final Sender sender,
      final IOException x) {
    log.error("execution of {} resulted in exception", exchange.getRequestPath(), x);
    callback.onException(exchange, sender, x);
  }

}
