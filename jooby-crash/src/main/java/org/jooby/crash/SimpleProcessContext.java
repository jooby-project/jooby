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
package org.jooby.crash;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.io.IOException;
import java.util.function.Consumer;

import org.crsh.shell.ShellProcessContext;
import org.crsh.shell.ShellResponse;
import org.crsh.text.Screenable;
import org.crsh.text.Style;
import org.jooby.Result;
import org.jooby.Results;

class SimpleProcessContext implements ShellProcessContext {

  private StringBuilder buff = new StringBuilder();

  private Consumer<Result> deferred;

  private int width;

  private int height;

  public SimpleProcessContext(final Consumer<Result> deferred) {
    this(deferred, 204, 48);
  }

  public SimpleProcessContext(final Consumer<Result> deferred, final int width,
      final int height) {
    this.deferred = deferred;
    this.width = width;
    this.height = height;
  }

  @Override
  public boolean takeAlternateBuffer() throws IOException {
    return false;
  }

  @Override
  public boolean releaseAlternateBuffer() throws IOException {
    return false;
  }

  @Override
  public String getProperty(final String propertyName) {
    return null;
  }

  @Override
  public String readLine(final String msg, final boolean echo)
      throws IOException, InterruptedException, IllegalStateException {
    return null;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public void flush() throws IOException {
  }

  @Override
  public Screenable append(final Style style) throws IOException {
    return this;
  }

  @Override
  public Screenable cls() throws IOException {
    return this;
  }

  @Override
  public Appendable append(final CharSequence csq) throws IOException {
    buff.append(csq);
    return this;
  }

  @Override
  public Appendable append(final CharSequence csq, final int start, final int end)
      throws IOException {
    buff.append(csq, start, end);
    return this;
  }

  @Override
  public Appendable append(final char c) throws IOException {
    buff.append(c);
    return this;
  }

  @Override
  public void end(final ShellResponse response) {
    org.jooby.Status status = Match(response).of(
        Case(instanceOf(ShellResponse.Ok.class), org.jooby.Status.OK),
        Case(instanceOf(ShellResponse.UnknownCommand.class), org.jooby.Status.BAD_REQUEST),
        Case($(), org.jooby.Status.SERVER_ERROR));

    deferred.accept(Results.with(buff.length() == 0 ? response.getMessage() : buff, status));
  }

  @Override
  public String toString() {
    return buff.toString();
  }
}
