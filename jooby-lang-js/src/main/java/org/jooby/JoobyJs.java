/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import com.google.common.io.Closeables;
import javaslang.control.Try;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JoobyJs {
  private ScriptEngine engine;

  public JoobyJs() throws Exception {
    ScriptEngineManager sem = new ScriptEngineManager();
    engine = sem.getEngineByName("nashorn");
    eval(Jooby.class.getResourceAsStream("/org/jooby/jooby.js"));
  }

  public Supplier<Jooby> run(final File file) throws Exception {
    return run(new FileReader(file));
  }

  public Supplier<Jooby> run(final Reader reader) throws Exception {
    eval(reader);
    return () -> Try.of(() -> (Jooby) engine.eval("this.__jooby_ && this.__jooby_()")).get();
  }

  void eval(final InputStream stream) throws Exception {
    eval(new InputStreamReader(stream, StandardCharsets.UTF_8));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  void eval(final Reader reader) throws Exception {
    Consumer closer = x -> Closeables.closeQuietly(reader);
    Try.run(() -> engine.eval(reader))
        .onFailure(closer)
        .onSuccess(closer);
  }

  public static void main(String[] mainargs) throws Throwable {
    String[] args = mainargs;
    String filename = "app.js";
    if (args.length > 0 && args[0].endsWith(".js")) {
      filename = args[0];
      args = new String[Math.max(0, mainargs.length - 1)];
      System.arraycopy(mainargs, 1, args, 0, args.length);
    }
    Jooby.run(new JoobyJs().run(new File(filename)), args);
  }
}
