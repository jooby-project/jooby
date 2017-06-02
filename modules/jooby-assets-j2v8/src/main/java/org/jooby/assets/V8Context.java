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
package org.jooby.assets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;

import javaslang.control.Try;

public class V8Context {

  public interface Callback {

    String call(V8Context ctx) throws Exception;

  }

  public final V8 v8;

  private String id;

  private V8Context(final String global, final String id) {
    this(V8.createV8Runtime(global), id);
  }

  private V8Context(final V8 v8, final String id) {
    this.v8 = v8;
    this.id = id;

    console(id);

    assets(v8);

    b64(v8);
  }

  public V8Object hash() {
    return register(new V8Object(v8));
  }

  public V8Function function(final JavaCallback callback) {
    return register(new V8Function(v8, callback));
  }

  public V8Object hash(final Map<String, Object> hash) {
    return register(V8ObjectUtils.toV8Object(v8, hash));
  }

  public V8Array array() {
    return register(new V8Array(v8));
  }

  public V8Array array(final List<? extends Object> value) {
    return register(V8ObjectUtils.toV8Array(v8, value));
  }

  public Object load(final String path) throws Exception {
    return register(v8.executeScript(readFile(path), path, 0));
  }

  public String invoke(final String path, final Object... args) throws Exception {
    V8Function fn = register((V8Function) load(path));
    Object value = register(fn.call(v8, array(Arrays.asList(args))));
    if (value instanceof String) {
      return value.toString();
    }

    List<AssetProblem> problems = problems(value);
    if (problems.size() > 0) {
      throw new AssetException(id, problems);
    }
    return ((V8Object) value).getString("output");
  }

  private List<AssetProblem> problems(final Object value) {
    if (value instanceof V8Array) {
      return problems((V8Array) value);
    }

    V8Object hash = (V8Object) value;
    if (hash.contains("errors")) {
      return problems(register(hash.getArray("errors")));
    }
    if (hash.contains("message")) {
      return ImmutableList.of(problem(hash));
    }
    return Collections.emptyList();
  }

  private List<AssetProblem> problems(final V8Array array) {
    ImmutableList.Builder<AssetProblem> result = ImmutableList.builder();
    for (int i = 0; i < array.length(); i++) {
      result.add(problem(register(array.getObject(i))));
    }
    return result.build();
  }

  private <T> Optional<T> get(final String name, final Function<String, T> provider) {
    return Try.of(() -> Optional.of(register(provider.apply(name)))).getOrElse(Optional.empty());
  }

  private AssetProblem problem(final V8Object js) {
    Optional<Integer> line = get("line", name -> ((Number) js.get(name)).intValue());
    Optional<Integer> column = get("column", name -> ((Number) js.get(name)).intValue());
    Optional<String> filename = get("filename", js::getString);
    Optional<String> evidence = get("evidence", js::getString);
    Optional<String> message = get("message", js::getString);

    return new AssetProblem(filename.orElse("file.js"), line.orElse(-1), column.orElse(-1),
        message.orElse(""), evidence.orElse(null));
  }

  private URL resolve(final String path) {
    URL resource = getClass().getResource(path.startsWith("/") ? path : "/" + path);
    return resource;
  }

  private boolean exists(final String path) {
    return resolve(path) != null;
  }

  private String readFile(final String path) throws IOException {
    URL resource = resolve(path);
    if (resource == null) {
      throw new FileNotFoundException(path);
    }
    try (InputStream stream = resource.openStream()) {
      return new String(ByteStreams.toByteArray(stream), "UTF-8");
    }
  }

  public static String run(final Callback callback) throws Exception {
    return run(null, callback);
  }

  public static String run(final String global, final Callback callback) throws Exception {
    V8Context ctx = new V8Context(global, classname(callback));
    try {
      return callback.call(ctx);
    } finally {
      ctx.v8.release();
    }
  }

  private static String classname(final Callback callback) {
    String logname = callback.getClass().getSimpleName();
    logname = logname.substring(0, logname.indexOf("$"));
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, logname);
  }

  private <T> T register(final T value) {
    if (value instanceof Releasable) {
      v8.registerResource((Releasable) value);
    }
    return value;
  }

  private JavaVoidCallback console(final Consumer<String> log) {
    return (self, args) -> {
      StringBuilder buff = new StringBuilder();
      for (int i = 0; i < args.length(); i++) {
        buff.append(register(args.get(i)));
      }
      log.accept(buff.toString());
    };
  }

  private void console(final String logname) {
    V8Object console = hash();
    Logger log = LoggerFactory.getLogger(logname);
    v8.add("console", console);
    console.registerJavaMethod(console(log::info), "log");
    console.registerJavaMethod(console(log::info), "info");
    console.registerJavaMethod(console(log::error), "error");
    console.registerJavaMethod(console(log::debug), "debug");
    console.registerJavaMethod(console(log::warn), "warn");
  }

  private void b64(final V8 v8) {
    v8.registerJavaMethod((JavaCallback) (receiver, args) -> {
      byte[] bytes = args.get(0).toString().getBytes(StandardCharsets.UTF_8);
      return BaseEncoding.base64().encode(bytes);
    }, "btoa");
    v8.registerJavaMethod((JavaCallback) (receiver, args) -> {
      byte[] atob = BaseEncoding.base64().decode(args.get(0).toString());
      return new String(atob, StandardCharsets.UTF_8);
    }, "atob");
  }

  private void assets(final V8 v8) {
    V8Object assets = hash();
    v8.add("assets", assets);

    assets.registerJavaMethod((JavaCallback) (receiver, args) -> {
      try {
        return readFile(args.get(0).toString());
      } catch (IOException ex) {
        // we can't fire exceptions from Java :S
        return V8.getUndefined();
      }
    }, "readFile");

    assets.registerJavaMethod((JavaCallback) (receiver, args) -> exists(args.get(0).toString()),
        "exists");

    assets.registerJavaMethod((JavaCallback) (receiver, args) -> {
      try {
        return load(args.get(0).toString());
      } catch (Exception ex) {
        // we can't fire exceptions from Java :S
        return V8.getUndefined();
      }
    }, "load");
  }

}
