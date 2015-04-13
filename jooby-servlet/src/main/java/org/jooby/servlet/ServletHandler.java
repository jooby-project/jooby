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
package org.jooby.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jooby.Jooby;
import org.jooby.spi.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

@SuppressWarnings("serial")
public class ServletHandler extends HttpServlet {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private HttpHandler dispatcher;

  private String tmpdir;

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);

    ServletContext ctx = config.getServletContext();

    Jooby app = (Jooby) ctx.getAttribute(Jooby.class.getName());

    dispatcher = app.require(HttpHandler.class);
    tmpdir = app.require(Config.class).getString("application.tmpdir");
  }

  @Override
  protected void service(final HttpServletRequest req, final HttpServletResponse rsp)
      throws ServletException, IOException {
    try {
      dispatcher.handle(
          new ServletServletRequest(req, tmpdir),
          new ServletServletResponse(rsp));
    } catch (IOException | ServletException | RuntimeException ex) {
      log.error("execution of: " + req.getRequestURI() + " resulted in error", ex);
      throw ex;
    } catch (Throwable ex) {
      log.error("execution of: " + req.getRequestURI() + " resulted in error", ex);
      throw new IllegalStateException(ex);
    }
  }

}
