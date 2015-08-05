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
package org.jooby.internal.sass;

import org.jooby.Err;
import org.jooby.Status;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;

import com.vaadin.sass.internal.handler.SCSSErrorHandler;

public class SassErrHandler extends SCSSErrorHandler {

  static {
    if (!SLF4JBridgeHandler.isInstalled()) {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
    }
  }

  private CSSParseException err;

  private Logger log;

  public SassErrHandler(final Logger log) {
    this.log = log;
  }

  @Override
  public void warning(final CSSParseException err) throws CSSException {
    log.warn("{}:{}:{}\n\t{}", err.getURI(), err.getLineNumber(), err.getColumnNumber(),
        err.getMessage());
  }

  @Override
  public void error(final CSSParseException err) throws CSSException {
    this.err = err;
  }

  @Override
  public void fatalError(final CSSParseException err) throws CSSException {
    this.err = err;
  }

  public void validate() {
    if (err != null) {
      String header = err.getURI() + ":" + err.getLineNumber() + ":" + err.getColumnNumber();
      throw new Err(Status.SERVER_ERROR, header + ": " + err.getMessage());
    }
  }

}
