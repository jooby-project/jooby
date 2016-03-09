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
package org.jooby.internal.js;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.jooby.Jooby;

import com.google.common.io.Closeables;

import javaslang.control.Try;

public class JsJooby {

  private ScriptEngine engine;

  public JsJooby() throws Exception {
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

  void eval(final Reader reader) throws Exception {
    try {
      engine.eval(reader);
    } finally {
      Closeables.closeQuietly(reader);
    }
  }

}
