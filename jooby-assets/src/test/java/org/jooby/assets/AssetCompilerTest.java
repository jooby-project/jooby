package org.jooby.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.*;

import org.jooby.Asset;
import org.jooby.MediaType;
import org.jooby.internal.URLAsset;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class AssetCompilerTest {

  @Test
  public void newCompiler() throws Exception {
    assertEquals("{home=[/assets/index.js, /assets/index.css]}",
        new AssetCompiler(conf("assets-test.conf", "dev")).toString());
  }

  @Test
  public void fileSet() throws Exception {
    assertEquals(Sets.newHashSet("home"),
        new AssetCompiler(conf("assets-test.conf", "dev")).keySet());

    assertEquals(Sets.newHashSet(), new AssetCompiler(conf("empty.conf", "dev")).keySet());

    assertEquals(Sets.newHashSet("home", "home1", "home2", "home3", "base", "lib", "lib2", "tjs"),
        new AssetCompiler(conf("assets-fileset.conf", "dev")).keySet());
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
    Map<String, List<File>> files = new AssetCompiler(conf("assets-compile-all.conf", "dev"))
        .build("dev", dir);
    files.values()
        .forEach(file -> file
            .forEach(it -> assertTrue(it.exists())));
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
    Asset asset = asset("/assets/index.js");
    assertEquals(asset, new AssetCompiler(conf("missing.conf", "dev")).build(asset));
  }

  @Test
  public void shouldNotProcessUnknownTypes() throws Exception {
    Asset asset = asset("/assets/index.none");
    assertEquals(asset, new AssetCompiler(conf("assets-test.conf", "dev")).build(asset));
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
    return new String(
        ByteStreams.toByteArray(compiler.build(asset(path)).stream()), "UTF-8");
  }

  private Asset asset(final String path) throws Exception {
    return new URLAsset(getClass().getResource(path), path,
        MediaType.byPath(path).orElse(MediaType.octetstream));
  }

  private Config conf(final String path, final String env) {
    return ConfigFactory.parseResources(path)
        .withValue("assets.env", ConfigValueFactory.fromAnyRef(env))
        .withValue("assets.charset", ConfigValueFactory.fromAnyRef("UTF-8"));
  }

  private void withLocale(Locale locale, Runnable block) {
    Locale systemDefault = Locale.getDefault();
    try {
      Locale.setDefault(locale);
      block.run();
    } finally {
      Locale.setDefault(systemDefault);
    }
  }
}
