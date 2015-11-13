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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooby.Asset;
import org.jooby.MediaType;
import org.jooby.internal.RoutePattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

/**
 * <h1>Asset compiler</h1>
 * <p>
 * Process static files by validate or modify them in some way.
 * </p>
 *
 * @author edgar
 * @see AssetProcessor
 * @see Assets
 * @since 0.11.0
 */
public class AssetCompiler {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<String, List<AssetProcessor>> pipeline;

  private final Map<String, List<String>> fileset;

  private final Predicate<String> scripts;

  private final Predicate<String> styles;

  private final Config conf;

  private final Charset charset;

  private ClassLoader loader;

  public AssetCompiler(final Config conf) throws Exception {
    this(conf.getClass().getClassLoader(), conf);
  }

  public AssetCompiler(final ClassLoader loader, final Config conf) throws Exception {
    this.loader = requireNonNull(loader, "Class loader is required.");
    this.conf = requireNonNull(conf, "Assets conf is required.");
    String basedir = conf.hasPath("assets.basedir") ? spath(conf.getString("assets.basedir")) : "";
    this.charset = Charset.forName(this.conf.getString("assets.charset"));
    if (this.conf.hasPath("assets.fileset")) {
      this.fileset = fileset(basedir, this.conf.getConfig("assets.fileset"));
    } else {
      this.fileset = Collections.emptyMap();
    }
    this.scripts = predicate(this.conf, ".js", ".coffee", ".ts");
    this.styles = predicate(this.conf, ".css", ".scss", ".less");
    if (this.fileset.size() > 0) {
      this.pipeline = pipeline(loader, this.conf.getConfig("assets"));
    } else {
      this.pipeline = Collections.emptyMap();
    }
  }

  public List<String> assets(final String name) {
    return fileset.getOrDefault(name, Collections.emptyList());
  }

  public Set<String> keySet() {
    return fileset.keySet();
  }

  public Set<String> patterns() {
    return patterns(file -> true)
        .map(v -> "/" + v + "/**")
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public List<String> scripts(final String name) {
    return assets(name)
        .stream()
        .filter(scripts)
        .collect(Collectors.toList());
  }

  public List<String> styles(final String name) {
    return assets(name)
        .stream()
        .filter(styles)
        .collect(Collectors.toList());
  }

  public List<AssetProcessor> pipeline(final String dist) {
    List<AssetProcessor> chain = this.pipeline.get(dist);
    if (chain == null) {
      throw new IllegalArgumentException("No pipeline for: " + dist);
    }
    return chain;
  }

  public Map<String, List<File>> build(final String dist, final File dir) throws Exception {
    Map<String, List<File>> output = new LinkedHashMap<>();
    for (String fset : keySet()) {
      List<String> files = assets(fset);

      log.info("compiling {}: ", fset);

      String css = compile(dist, files.stream().filter(styles).iterator(), MediaType.css, "");
      Path pcss = Paths.get(patterns(styles).findFirst().get(), fset + "." + sha1(css) + ".css");
      File fcss = dir.toPath().resolve(pcss).toFile();
      fcss.getParentFile().mkdirs();
      Files.write(css, fcss, charset);

      String js = compile(dist, files.stream().filter(scripts).iterator(), MediaType.js, ";");
      Path pjs = Paths.get(patterns(scripts).findFirst().get(), fset + "." + sha1(js) + ".js");
      File fjs = dir.toPath().resolve(pjs).toFile();
      fjs.getParentFile().mkdirs();
      Files.write(js, fjs, charset);

      log.info("{}", fcss);
      log.info("{}", fjs);

      output.put(fset, Arrays.asList(fcss, fjs));
    }
    return output;
  }

  public Asset build(final Asset asset) throws Exception {

    if (pipeline.size() == 0) {
      return asset;
    }

    String filename = asset.path();

    final MediaType type;
    if (scripts.test(filename)) {
      type = MediaType.js;
    } else if (styles.test(filename)) {
      type = MediaType.css;
    } else {
      return asset;
    }

    String output = compile("dev", filename, type, toString(asset.stream(), charset));

    return new InMemoryAsset(asset, output.getBytes(charset));
  }

  @Override
  public String toString() {
    return fileset.toString();
  }

  private Stream<String> patterns(final Predicate<String> filter) {
    return fileset.values().stream()
        .flatMap(List::stream)
        .filter(filter)
        .map(path -> path.split("/")[1]);

  }

  private String compile(final String env, final Iterator<String> files, final MediaType type,
      final String sep)
          throws Exception {
    StringBuilder buff = new StringBuilder();
    while (files.hasNext()) {
      String file = files.next();
      log.info("  {}", file);
      buff.append(compile(env, file, type, readFile(loader, file, charset))).append(sep);
    }
    return buff.toString();
  }

  private String compile(final String env, final String filename, final MediaType type,
      final String input) throws Exception {

    Iterator<AssetProcessor> it = pipeline(env).iterator();
    String contents = input;
    while (it.hasNext()) {
      AssetProcessor processor = it.next();
      if (processor.matches(type) && !excludes(processor, filename)) {
        String pname = processor.name();
        long start = System.currentTimeMillis();
        try {
          log.debug("executing: {}", pname);
          contents = processor.process(filename, contents, conf);
        } finally {
          long end = System.currentTimeMillis();
          log.debug("{} took {}ms", pname, end - start);
        }
      }
    }
    return contents;
  }

  private String sha1(final String source) {
    return BaseEncoding.base16()
        .encode(Hashing
            .sha1()
            .hashString(source, charset)
            .asBytes())
        .substring(0, 8).toLowerCase();
  }

  @SuppressWarnings("unchecked")
  private boolean excludes(final AssetProcessor processor, final String path) {
    Object value = processor.get("excludes");
    if (value == null) {
      return false;
    }
    List<String> excludes;
    if (value instanceof List) {
      excludes = (List<String>) value;
    } else {
      excludes = ImmutableList.of(value.toString());
    }
    String spath = spath(path);
    return excludes.stream()
        .map(it -> new RoutePattern("GET", it))
        .filter(pattern -> pattern.matcher("GET" + spath).matches())
        .findFirst()
        .isPresent();
  }

  private static String readFile(final ClassLoader loader, final String path, final Charset charset)
      throws IOException {
    String spath = path.startsWith("/") ? path.substring(1) : path;
    URL res = loader.getResource(spath);
    if (res == null) {
      throw new FileNotFoundException(spath);
    }
    return toString(res.openStream(), charset);
  }

  private static String toString(final InputStream in, final Charset charset) throws IOException {
    try {
      return new String(ByteStreams.toByteArray(in), charset);
    } finally {
      Closeables.closeQuietly(in);
    }
  }

  private static Predicate<String> predicate(final Config fileset, final String... extension) {
    String path = "assets" + extension[0];
    Set<String> extensions = new HashSet<>();
    extensions.addAll(Arrays.asList(extension));
    if (fileset.hasPath(path)) {
      extensions.addAll(strlist(fileset.getAnyRef(path)));
    }
    return file -> {
      for (String ext : extensions) {
        if (file.endsWith(ext)) {
          return true;
        }
      }
      return false;
    };
  }

  private static Map<String, List<String>> fileset(final String basedir, final Config fileset) {
    Map<String, List<String>> result = new HashMap<>();
    // 1st pass, collect single resources (no merge)
    fileset.entrySet().forEach(e -> {
      String[] key = unquote(e.getKey()).split("\\s*<\\s*");
      result.put(key[0], strlist(e.getValue().unwrapped(), v -> basedir + spath(v)));
    });
    // 2nd pass, merge resources
    fileset.entrySet().forEach(e -> {
      String[] key = unquote(e.getKey()).split("\\s*<\\s*");
      if (key.length > 1) {
        ImmutableList.Builder<String> resources = ImmutableList.builder();
        for (int i = key.length - 1; i >= 0; i--) {
          resources.addAll(result.get(key[i]));
        }
        // overwrite
        result.put(key[0], resources.build());
      }
    });
    return result;
  }

  private static String unquote(final String key) {
    return key.replace("\"", "");
  }

  private static Map<String, List<AssetProcessor>> pipeline(final ClassLoader loader,
      final Config conf) throws Exception {
    Map<String, List<AssetProcessor>> processors = new HashMap<>();
    processors.put("dev", Collections.emptyList());
    if (conf.hasPath("pipeline")) {
      Set<String> filter = conf.getConfig("pipeline").entrySet().stream()
          .map(e -> e.getKey())
          .collect(Collectors.toSet());
      filter.add("class");
      Set<Entry<String, ConfigValue>> entrySet = conf.getConfig("pipeline").entrySet();
      for (Entry<String, ConfigValue> entry : entrySet) {
        String env = unquote(entry.getKey());
        processors.put(env,
            processors(conf, loader, env, strlist(entry.getValue().unwrapped()), filter));
      }
    }
    return processors;
  }

  @SuppressWarnings("unchecked")
  private static List<AssetProcessor> processors(final Config conf, final ClassLoader loader,
      final String env, final List<String> names, final Set<String> filter) throws Exception {
    try {
      Map<String, Class<AssetProcessor>> classes = new LinkedHashMap<>();
      for (Entry<String, String> entry : bind(conf, names).entrySet()) {
        classes.put(entry.getKey(), (Class<AssetProcessor>) loader.loadClass(entry.getValue()));
      }
      return processors(conf, env, filter, classes);
    } catch (Exception ex) {
      throw ex;
    }
  }

  private static List<AssetProcessor> processors(final Config conf, final String env,
      final Set<String> filter, final Map<String, Class<AssetProcessor>> classes) throws Exception {
    List<AssetProcessor> processors = new ArrayList<>();
    Function<Config, Config> without = options -> {
      for (String path : filter) {
        options = options.withoutPath(path);
      }
      return options;
    };
    for (Entry<String, Class<AssetProcessor>> entry : classes.entrySet()) {
      String name = entry.getKey();
      Class<AssetProcessor> clazz = entry.getValue();
      Config options = ConfigFactory.empty();
      if (conf.hasPath(name)) {
        options = conf.getConfig(name);
        if (options.hasPath(env)) {
          options = options.getConfig(env).withFallback(options);
        }
      }
      AssetProcessor processor = clazz.newInstance();
      processor.set(without.apply(options));
      processors.add(processor);
    }
    return processors;
  }

  private static Map<String, String> bind(final Config conf, final List<String> names) {
    Map<String, String> map = new LinkedHashMap<>();
    names.forEach(name -> {
      String clazz = AssetCompiler.class.getPackage().getName() + "."
          + CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, name);
      if (conf.hasPath(name + ".class")) {
        clazz = conf.getString(name + ".class");
      }
      map.put(name, clazz);
    });

    return map;
  }

  private static List<String> strlist(final Object value) {
    return strlist(value, v -> v);
  }

  @SuppressWarnings("unchecked")
  private static List<String> strlist(final Object value, final Function<String, String> mapper) {
    ImmutableList.Builder<String> list = ImmutableList.builder();
    if (value instanceof Collection) {
      ((Collection<? extends String>) value).forEach(v -> list.add(mapper.apply(v)));
    } else {
      list.add(mapper.apply(value.toString()));
    }
    return list.build();
  }

  private static String spath(final String path) {
    return path.startsWith("/") ? path : "/" + path;
  }

}
