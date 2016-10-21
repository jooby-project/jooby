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
import static javaslang.Predicates.instanceOf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.function.Consumer;
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
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import javaslang.control.Try;

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

  private final List<AssetAggregator> aggregators = new ArrayList<>();

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
    this.loader = AssetClassLoader.classLoader(loader);
    this.conf = requireNonNull(conf, "Assets conf is required.");
    String basedir = conf.hasPath("assets.basedir") ? spath(conf.getString("assets.basedir")) : "";
    this.charset = Charset.forName(this.conf.getString("assets.charset"));
    if (this.conf.hasPath("assets.fileset")) {
      this.fileset = fileset(loader, basedir, this.conf, aggregators::add);
    } else {
      this.fileset = new HashMap<>();
    }
    this.scripts = predicate(this.conf, ".js", ".coffee", ".ts");
    this.styles = predicate(this.conf, ".css", ".scss", ".sass", ".less");
    if (this.fileset.size() > 0) {
      this.pipeline = pipeline(loader, this.conf.getConfig("assets"));
    } else {
      this.pipeline = Collections.emptyMap();
    }
  }

  /**
   * Get all the assets for the provided file set. Example:
   *
   * <pre>
   * assets {
   *   fileset {
   *     home: [home.css, home.js]
   *   }
   * }
   * </pre>
   *
   * This method returns <code>home.css</code> and <code>home.js</code> for <code>home</code> file
   * set. If there is no fileset under that name then it returns an empty list.
   *
   * @param name Fileset name.
   * @return List of files or empty list.
   */
  public List<String> assets(final String name) {
    return fileset.getOrDefault(name, Collections.emptyList());
  }

  /**
   * @return Returns all the fileset.
   */
  public Set<String> fileset() {
    return fileset.keySet();
  }

  /**
   * Iterate over fileset and common path pattern for them. Example:
   *
   * <pre>
   * {
   *   assets {
   *     fileset {
   *       lib: [js/lib/jquery.js],
   *       home: [css/style.css, js/home.js]
   *     }
   *   }
   * }
   * </pre>
   *
   * This method returns a set with <code>/css/**</code> and <code>/js/**</code> pattern.
   *
   * @return Path pattern of the entire fileset.
   */
  public Set<String> patterns() {
    return patterns(file -> !aggregators.stream()
        .filter(it -> it.fileset().contains(file))
        .findFirst().isPresent())
            .map(v -> "/" + v + "/**")
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Test if the provided path is part of the fileset.
   *
   * @param path Test to test.
   * @return True, if the path belong to the filset.
   */
  public boolean contains(final String path) {
    Predicate<List<String>> filter = fs -> fs.stream()
        .filter(it -> path.endsWith(it))
        .findFirst()
        .isPresent();

    boolean generated = aggregators.stream()
        .map(AssetAggregator::fileset)
        .filter(filter)
        .findFirst()
        .isPresent();
    if (!generated) {
      return fileset.values().stream()
          .filter(filter)
          .findFirst()
          .isPresent();
    }
    return false;
  }

  /**
   * Get all the javascript (or derived) for the provided fileset. Example:
   *
   * <pre>
   * {
   *   assets {
   *     fileset {
   *       mypage: [mypage.js, mypage.css]
   *     }
   *   }
   * }
   * </pre>
   *
   * <p>
   * This method returns <code>mypage.js</code> for <code>mypage</code> fileset.
   * </p>
   *
   * @param fileset Fileset name.
   * @return All the scripts for a fileset.
   */
  public List<String> scripts(final String fileset) {
    return assets(fileset)
        .stream()
        .filter(scripts)
        .collect(Collectors.toList());
  }

  /**
   * Get all the css files (or derived) for the provided fileset. Example:
   *
   * <pre>
   * {
   *   assets {
   *     fileset {
   *       mypage: [mypage.js, mypage.css]
   *     }
   *   }
   * }
   * </pre>
   *
   * <p>
   * This method returns <code>mypage.js</code> for <code>mypage</code> fileset.
   * </p>
   *
   * @param fileset Fileset name.
   * @return All the scripts for a fileset.
   */
  public List<String> styles(final String fileset) {
    return assets(fileset)
        .stream()
        .filter(styles)
        .collect(Collectors.toList());
  }

  /**
   * List all the {@link AssetProcessor} for a distribution (a.k.a. environment).
   *
   * @param dist Distribution's name.
   * @return A readonly list of available {@link AssetProcessor}.
   */
  public List<AssetProcessor> pipeline(final String dist) {
    List<AssetProcessor> chain = this.pipeline.get(dist);
    if (chain == null) {
      log.debug("no pipeline for: {}", dist);
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(chain);
  }

  /**
   * @return Readonly list of available {@link AssetAggregator}.
   */
  public List<AssetAggregator> aggregators() {
    return Collections.unmodifiableList(aggregators);
  }

  /**
   * Build assets using the given distribution and write output to the provided directory.
   *
   * Build process is defined as follow:
   *
   * 1. First, it runs all the aggregators (if any)
   * 2. Then iterates each fileset and per each file in the fileset it apply the processor pipeline.
   * 3. Finally, it merge all the files into one file and compressed/optimized if need it.
   *
   * @param dist Distribution's name (usually dev or dist).
   * @param dir Output directory.
   * @return Map with fileset name as key and list of generated assets.
   * @throws Exception If something goes wrong.
   */
  public Map<String, List<File>> build(final String dist, final File dir) throws Exception {
    Map<String, List<File>> output = new LinkedHashMap<>();

    log.info("{} aggregators: {}", dist, aggregators);
    aggregators(aggregators, conf);

    List<AssetProcessor> pipeline = pipeline(dist);
    log.info("{} pipeline: {}", dist, pipeline);
    for (String fset : fileset()) {
      List<String> files = assets(fset);

      log.info("compiling {}:", fset);

      String css = compile(pipeline, files.stream().filter(styles).iterator(), MediaType.css, "");
      Path cssSha1 = Paths.get(fset + "." + sha1(css) + ".css");
      Path pcss = patterns(styles).findFirst()
          .map(p -> Paths.get(p).resolve(cssSha1))
          .orElse(cssSha1);
      File fcss = dir.toPath().resolve(pcss).toFile();
      fcss.getParentFile().mkdirs();
      ImmutableList.Builder<File> outputbuilder = ImmutableList.builder();
      if (css.length() > 0) {
        Files.write(css, fcss, charset);
        outputbuilder.add(fcss);
      }

      String js = compile(pipeline, files.stream().filter(scripts).iterator(), MediaType.js, ";");
      Path jsSha1 = Paths.get(fset + "." + sha1(js) + ".js");
      Path pjs = patterns(scripts).findFirst()
          .map(p -> Paths.get(p).resolve(jsSha1))
          .orElse(jsSha1);
      File fjs = dir.toPath().resolve(pjs).toFile();
      fjs.getParentFile().mkdirs();
      if (js.length() > 0) {
        Files.write(js, fjs, charset);
        outputbuilder.add(fjs);
      }

      List<File> fsoutput = outputbuilder.build();
      fsoutput.forEach(
          it -> log.info("{} {} ({})", it.getName(), humanReadableByteCount(it.length()), it));
      output.put(fset, fsoutput);
    }
    return output;
  }

  private void aggregators(final List<AssetAggregator> aggregators, final Config conf)
      throws Exception {
    for (AssetAggregator it : aggregators) {
      log.info("applying {}", it);
      it.run(conf);
    }

  }

  /**
   * Apply the processor pipeline to the given asset. Like {@link #build(String, File)} but for a
   * single file or asset.
   *
   * @param asset Asset to build.
   * @return Processed asset.
   * @throws Exception If something goes wrong.
   */
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

    List<AssetProcessor> pipeline = pipeline("dev");
    String output = compile(pipeline, filename, type, toString(asset.stream(), charset));

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

  private String compile(final List<AssetProcessor> pipeline, final Iterator<String> files,
      final MediaType type, final String sep) throws Exception {
    StringBuilder buff = new StringBuilder();
    while (files.hasNext()) {
      String file = files.next();
      log.info("  {}", file);
      buff.append(compile(pipeline, file, type, readFile(loader, file, charset))).append(sep);
    }
    return buff.toString();
  }

  private String compile(final List<AssetProcessor> pipeline, final String filename,
      final MediaType type, final String input) throws Exception {

    Iterator<AssetProcessor> it = pipeline.iterator();
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
    InputStream resource = loader.getResourceAsStream(spath);
    if (resource == null) {
      throw new FileNotFoundException(path);
    }
    return toString(resource, charset);
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

  private static Map<String, List<String>> fileset(final ClassLoader loader, final String basedir,
      final Config conf, final Consumer<AssetAggregator> aggregators) {
    Map<String, List<String>> raw = new HashMap<>();
    Map<String, List<String>> graph = new HashMap<>();
    Config assetconf = conf.getConfig("assets");
    Config fileset = assetconf.getConfig("fileset");
    // 1st pass, collect single resources (no merge)
    fileset.entrySet().forEach(e -> {
      List<String> key = Splitter.on('<')
          .trimResults()
          .omitEmptyStrings()
          .splitToList(unquote(e.getKey()));
      List<String> candidates = strlist(e.getValue().unwrapped(), v -> basedir + spath(v));
      List<String> values = new ArrayList<>();
      candidates.forEach(it -> {
        Try.run(() -> {
          processors(assetconf, loader, null, ImmutableList.of(it.substring(1)), ImmutableSet.of())
              .stream().filter(instanceOf(AssetAggregator.class))
              .forEach(p -> {
                AssetAggregator a = (AssetAggregator) p;
                aggregators.accept(a);
                a.fileset().forEach(f -> values.add(spath(f)));
              });
        }).onFailure(x -> values.add(it));
      });
      raw.put(key.get(0), values);
      graph.put(key.get(0), key);
    });

    Map<String, List<String>> resolved = new HashMap<>();
    graph.forEach((fs, deps) -> {
      resolve(fs, deps, raw, graph, resolved);
    });
    return resolved;
  }

  private static List<String> resolve(final String fs, final List<String> deps,
      final Map<String, List<String>> raw, final Map<String, List<String>> graph,
      final Map<String, List<String>> resolved) {
    List<String> result = resolved.get(fs);
    if (result == null) {
      result = new ArrayList<>();
      resolved.put(fs, result);
      for (int i = deps.size() - 1; i > 0; i--) {
        result.addAll(resolve(deps.get(i), graph.get(deps.get(i)), raw, graph, resolved));
      }
      result.addAll(raw.get(fs));
    }
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
  private static <T extends AssetOptions> List<T> processors(final Config conf,
      final ClassLoader loader, final String env, final List<String> names,
      final Set<String> filter) throws Exception {
    Map<String, Class<AssetOptions>> classes = new LinkedHashMap<>();
    for (Entry<String, String> entry : bind(conf, names).entrySet()) {
      classes.put(entry.getKey(), (Class<AssetOptions>) loader.loadClass(entry.getValue()));
    }
    return (List<T>) processors(conf, env, filter, classes);
  }

  @SuppressWarnings("unchecked")
  private static <T extends AssetOptions> List<T> processors(final Config conf, final String env,
      final Set<String> filter, final Map<String, Class<T>> classes) throws Exception {
    List<T> processors = new ArrayList<>();
    Function<Config, Config> without = options -> {
      for (String path : filter) {
        options = options.withoutPath(path);
      }
      return options;
    };
    for (Entry<String, Class<T>> entry : classes.entrySet()) {
      String name = entry.getKey();
      Class<T> clazz = entry.getValue();
      Config options = ConfigFactory.empty();
      if (conf.hasPath(name)) {
        options = conf.getConfig(name);
        if (env != null && options.hasPath(env)) {
          options = options.getConfig(env).withFallback(options);
        }
      }
      AssetOptions processor = clazz.newInstance();
      processor.set(without.apply(options));
      processors.add((T) processor);
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
    return path.charAt(0) == '/' ? path : "/" + path;
  }

  // http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
  static String humanReadableByteCount(final long bytes) {
    int unit = 1024;
    if (bytes < unit) {
      return bytes + "b";
    }
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    char pre = "kmgtpe".charAt(exp - 1);
    return String.format("%.1f%sb", bytes / Math.pow(unit, exp), pre);
  }

}
