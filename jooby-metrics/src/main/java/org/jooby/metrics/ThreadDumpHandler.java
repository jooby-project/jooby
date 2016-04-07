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
package org.jooby.metrics;

import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route.Handler;
import org.jooby.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.jvm.ThreadDump;

/**
 * Prints thread states (a.k.a thread dump).
 *
 * @author edgar
 * @since 0.13.0
 */
public class ThreadDumpHandler implements Handler {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private ThreadDump threadDump;

  {
    try {
      // Some PaaS like Google App Engine blacklist java.lang.managament
      this.threadDump = new ThreadDump(ManagementFactory.getThreadMXBean());
    } catch (Exception ex) {
      log.warn("Thread dump isn't available", ex);
    }
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Throwable {
    Object data;
    Status status;
    if (threadDump == null) {
      data = "Sorry your runtime environment does not allow to dump threads.";
      status = Status.NOT_IMPLEMENTED;
    } else {
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      threadDump.dump(output);
      data = output.toByteArray();
      status = Status.OK;
    }
    rsp.type(MediaType.plain)
        .status(status)
        .header("Cache-Control", "must-revalidate,no-cache,no-store")
        .send(data);
  }

}
