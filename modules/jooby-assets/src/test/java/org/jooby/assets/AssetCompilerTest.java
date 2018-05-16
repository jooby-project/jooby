package org.jooby.assets;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.funzy.Try;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AssetCompilerTest {

  @Test
  public void newCompiler() throws Exception {
    assertEquals("{home=[/assets/index.js, /assets/index.css]}",
        new AssetCompiler(conf("assets-test.conf", "dev")).toString());
  }

  @Test
  public void fileSet() throws Exception {
    assertEquals(Sets.newHashSet("home"),
        new AssetCompiler(conf("assets-test.conf", "dev")).fileset());

    assertEquals(Sets.newHashSet(), new AssetCompiler(conf("empty.conf", "dev")).fileset());

    assertEquals(Sets.newHashSet("home", "home1", "home2", "home3", "base", "lib", "lib2", "tjs"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).fileset());
  }

  @Test
  public void fileSetAggregator() throws Exception {
    assertEquals(Lists.newArrayList("/assets/dyn.css", "/assets/index.js", "/assets/index.css"),
        new AssetCompiler(conf("assets-aggregator.conf", "dev")).assets("mypage"));

    assertEquals(Lists.newArrayList("/aggregator-missing", "/assets/index.js", "/assets/index.css"),
        new AssetCompiler(conf("assets-missing-aggregator.conf", "dev")).assets("mypage"));
  }

  @Test
  public void listAggregators() throws Exception {
    assertEquals("[aggregator-test]",
        new AssetCompiler(conf("assets-aggregator.conf", "dev")).aggregators().toString());
  }

  @Test
  public void basedir() throws Exception {
    assertEquals(
        Lists.newArrayList("/assets/base/base.js", "/assets/home.css", "/assets/js/home.js"),
        new AssetCompiler(conf("assets-basedir.conf", "dev")).assets("home"));
  }

  @Test
  public void patterns() throws Exception {
    assertEquals(Sets.newHashSet("/assets/**"),
        new AssetCompiler(conf("assets-pattern-1.conf", "dev")).patterns());

    assertEquals(Sets.newHashSet("/js/**", "/css/**"),
        new AssetCompiler(conf("assets-pattern-2.conf", "dev")).patterns());

  }

  @Test
  public void summary() throws Exception {
    AssetCompiler compiler = new AssetCompiler(conf("assets-compile-all.conf", "dev"));

    File dir = Paths.get("target", "summary").toFile();
    Map<String, List<File>> files = compiler.build("dev", dir);

    assertEquals("Summary:\n"
            + "Pipeline: []\n"
            + "Time: 1s\n"
            + "Output: target/summary\n"
            + "Foo: bar\n"
            + "Fileset          Output       Size\n"
            + "  all                               \n"
            + "          assets/home.css         8b\n"
            + "           assets/base.js        19b\n"
            + "           assets/home.js        19b\n",
        compiler.summary(files, dir.toPath(), "dev", 1000, "Foo: bar"));
  }

  @Test
  public void stop() throws Exception {
    try {
      TestEngineFactory.count.set(0);
      System.setProperty("assets.engine", TestEngineFactory.class.getName());
      AssetCompiler compiler = new AssetCompiler(conf("assets-compile-all.conf", "dev"));
      compiler.stop();
      assertEquals(1, TestEngineFactory.count.get());
    } finally {
      TestEngineFactory.count.set(0);
      System.clearProperty("assets.engine");
    }
  }

  @Test
  public void progressbar() throws Exception {
    File dir = Paths.get("target", "progressbar").toFile();

    AssetCompiler compiler = new AssetCompiler(conf("assets-compile-all.conf", "dev"));
    AtomicInteger counter = new AtomicInteger();
    AtomicInteger total = new AtomicInteger();
    AtomicInteger lastProgress = new AtomicInteger();
    compiler.setProgressBar((c, t) -> {
      counter.incrementAndGet();
      lastProgress.set(c);
      total.set(t);
    });
    compiler.build("dev", dir);
    assertEquals(3, counter.get());
    assertEquals(3, lastProgress.get());
    assertEquals(3, total.get());
  }

  @Test
  public void assets() throws Exception {
    assertEquals(Lists.newArrayList("/assets/base.js", "/assets/normalize.css"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).assets("base"));

    assertEquals(Lists.newArrayList("/assets/jquery.js"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).assets("lib"));

    assertEquals(Lists.newArrayList("/assets/jquery.js", "/assets/jquery-ui.js"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).assets("lib2"));

    assertEquals(Lists.newArrayList("/assets/index.js", "/assets/index.css"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).assets("home"));

    assertEquals(Lists.newArrayList("/assets/base.js", "/assets/normalize.css", "/assets/index.js",
        "/assets/index.css"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).assets("home1"));

    assertEquals(Lists.newArrayList("/assets/base.js", "/assets/normalize.css", "/assets/jquery.js",
        "/assets/index.js",
        "/assets/index.css"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).assets("home2"));

    assertEquals(Lists.newArrayList("/assets/jquery.js", "/assets/jquery-ui.js", "/assets/index.js",
        "/assets/index.css"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).assets("home3"));

    assertEquals(Lists.newArrayList("/assets/tjs.tjs"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).assets("tjs"));
  }

  @Test
  public void scripts() throws Exception {
    assertEquals(Arrays.asList("/assets/base.js"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).scripts("base"));

    assertEquals(Arrays.asList("/assets/normalize.css"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).styles("base"));
  }

  @Test
  public void build() throws Exception {
    assertEquals("processor-test:",
        compile(new AssetCompiler(conf("assets-test.conf", "dev")), "/assets/index.js"));

    assertEquals("css-processor-test:",
        compile(new AssetCompiler(conf("assets-test.conf", "dev")), "/assets/index.css"));
  }

  @Test
  public void bundle() throws Exception {
    File dir = Paths.get("target", "public").toFile();
    Map<String, List<File>> files = new AssetCompiler(conf("assets-compile-all.conf", "prod"))
        .build("prod", dir);
    Set<File> expected = Sets.newHashSet(
        Paths.get("target", "public", "assets", "all.0a36d8bd.css").toFile(),
        Paths.get("target", "public", "assets", "all.9a92930a.js").toFile());
    files.values()
        .forEach(file -> file
            .forEach(it -> {
              assertTrue(it.exists());
              expected.remove(it);
            }));
    assertEquals(0, expected.size());
  }

  @Test
  public void devBuild() throws Exception {
    File dir = Paths.get("target", "public").toFile();
    Map<String, List<File>> files = new AssetCompiler(conf("assets-compile-all.conf", "dev"))
        .build("dev", dir);
    Set<File> expected = Sets.newHashSet(
        Paths.get("target", "public", "assets", "home.css").toFile(),
        Paths.get("target", "public", "assets", "home.js").toFile(),
        Paths.get("target", "public", "assets", "base.js").toFile());
    files.values()
        .forEach(file -> file
            .forEach(it -> {
              assertTrue(it.exists());
              expected.remove(it);
            }));
    assertEquals(0, expected.size());
  }

  @Test
  public void devBuildSkipDuplicates() throws Exception {
    File dir = Paths.get("target", "public").toFile();
    Map<String, List<File>> files = new AssetCompiler(conf("assets-dev.conf", "dev"))
        .build("dev", dir);
    Set<File> expected = Sets.newHashSet(
        Paths.get("target", "public", "assets", "home.css").toFile(),
        Paths.get("target", "public", "assets", "home.js").toFile(),
        Paths.get("target", "public", "assets", "base.js").toFile());
    files.values()
        .forEach(file -> file
            .forEach(it -> {
              assertTrue(it.exists());
              expected.remove(it);
            }));
    assertEquals(0, expected.size());
  }

  @Test
  public void bundlenojs() throws Exception {
    File dir = Paths.get("target", "public", "nojs").toFile();
    Map<String, List<File>> files = new AssetCompiler(conf("assets-nojs.conf", "dev"))
        .build("prod", dir);
    assertEquals(0, files.get("home").size());
  }

  @Test
  public void bundlenocss() throws Exception {
    File dir = Paths.get("target", "public", "nojs").toFile();
    Map<String, List<File>> files = new AssetCompiler(conf("assets-nocss.conf", "prod"))
        .build("prod", dir);
    assertEquals(0, files.get("home").size());
  }

  @Test
  public void bundleWithAggregator() throws Exception {
    File dir = Paths.get("target", "public").toFile();
    Map<String, List<File>> files = new AssetCompiler(conf("assets-aggregator.conf", "prod"))
        .build("prod", dir);
    files.values()
        .forEach(file -> file
            .forEach(it -> {
              assertTrue(it.exists());
              if (it.getName().endsWith(".css")) {
                String css = Try.apply(() -> Files.readAllLines(it.toPath()).stream()
                    .collect(Collectors.joining("\n"))).get();
                assertTrue(css.contains(".dyn"));
              }
            }));
  }

  @Test(expected = FileNotFoundException.class)
  public void fnf() throws Exception {
    File dir = Paths.get("target", "public").toFile();
    Map<String, List<File>> files = new AssetCompiler(conf("assets-fnf.conf", "dev"))
        .build("dev", dir);
    files.values()
        .forEach(file -> file
            .forEach(it -> assertTrue(it.exists())));
  }

  @Test
  public void shouldNotProcessWithoutFileSet() throws Exception {
    File dir = Paths.get("target", "public").toFile();
    assertEquals(dir.toPath().resolve("assets").resolve("index.js").toFile(),
        new AssetCompiler(conf("missing.conf", "dev")).buildOne("/assets/index.js", dir));
  }

  @Test
  public void shouldNotProcessUnknownTypes() throws Exception {
    File dir = Paths.get("target", "public").toFile();
    assertEquals(null,
        new AssetCompiler(conf("assets-test.conf", "dev")).buildOne("/assets/index.none", dir));
  }

  @Test
  public void shouldExcludeFiles() throws Exception {
    assertEquals("processor-test:",
        compile(new AssetCompiler(conf("assets-excludes.conf", "dev")), "/assets/index.js"));

    assertEquals("(function () {});",
        compile(new AssetCompiler(conf("assets-excludes.conf", "dev")), "/assets/lib/lib.js"));

    assertEquals("(function () {});",
        compile(new AssetCompiler(conf("assets-exclude-one.conf", "dev")), "/assets/lib/lib.js"));
  }

  @Test(expected = ClassNotFoundException.class)
  public void shouldFaileOnNotFoundProcessor() throws Exception {
    new AssetCompiler(conf("assets-processor-not-found.conf", "dev"));
  }

  @Test
  public void pipeline() throws Exception {
    AssetCompiler compiler = new AssetCompiler(conf("assets-pipeline.conf", "dev"));
    List<AssetProcessor> dev = compiler.pipeline("dev");
    assertNotNull(dev);
    assertEquals(1, dev.size());
    assertTrue(dev.iterator().next() instanceof ProcessorTest);
    assertEquals("bar", dev.iterator().next().get("foo"));
    assertEquals("file", dev.iterator().next().get("sourceMap.type"));
    assertEquals(true, dev.iterator().next().get("sourceMap.sources"));

    List<AssetProcessor> prod = compiler.pipeline("dist");
    assertNotNull(prod);
    assertEquals(2, prod.size());
    assertTrue(prod.iterator().next() instanceof ProcessorTest);
    assertEquals("foo", prod.iterator().next().get("foo"));
    assertEquals("bar", prod.iterator().next().get("bar"));

    List<AssetProcessor> pipeline = compiler.pipeline("prod");
    assertEquals(Collections.emptyList(), pipeline);
  }

  @Test
  public void humandReadableBytes() throws Exception {
    withLocale(Locale.US, () -> {
      assertEquals("1.0kb", AssetCompiler.humanReadableByteCount(1024));
      assertEquals("1.0mb", AssetCompiler.humanReadableByteCount(1024 * 1024));
      assertEquals("100.0mb", AssetCompiler.humanReadableByteCount(1024 * 1024 * 100));
    });
    withLocale(Locale.GERMAN, () -> {
      assertEquals("1,0kb", AssetCompiler.humanReadableByteCount(1024));
      assertEquals("1,0mb", AssetCompiler.humanReadableByteCount(1024 * 1024));
      assertEquals("100,0mb", AssetCompiler.humanReadableByteCount(1024 * 1024 * 100));
    });
  }

  private String compile(final AssetCompiler compiler, final String path) throws Exception {
    File dir = Paths.get("target", "public").toFile();
    return Files.readAllLines(compiler.buildOne(path, dir).toPath()).stream()
        .collect(Collectors.joining("\n"));
  }

  private Config conf(final String path, final String env) {
    return ConfigFactory.parseResources(path)
        .withValue("assets.env", ConfigValueFactory.fromAnyRef(env))
        .withValue("assets.charset", ConfigValueFactory.fromAnyRef("UTF-8"));
  }

  private void withLocale(final Locale locale, final Runnable block) {
    Locale systemDefault = Locale.getDefault();
    try {
      Locale.setDefault(locale);
      block.run();
    } finally {
      Locale.setDefault(systemDefault);
    }
  }
}
