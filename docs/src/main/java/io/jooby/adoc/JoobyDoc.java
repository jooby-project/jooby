package io.jooby.adoc;

import org.apache.commons.io.FileUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JoobyDoc {
  public static final Object VERSION = "2.0.0-Alpha1";

  public static void main(String[] args) throws IOException {
    Path basedir = Paths.get(System.getProperty("user.dir"), "docs");
    Path outdir = basedir.resolve("out");
    Asciidoctor asciidoctor = Asciidoctor.Factory.create();
    Options options = new Options();
    options.setBackend("html");
    Attributes attributes = new Attributes();
    attributes.setAttribute("docinfo", "shared");
    attributes.setAttribute("joobyVersion", VERSION);

    options.setAttributes(attributes);
    options.setBaseDir(basedir.toAbsolutePath().toString());
    options.setDocType("book");
    options.setToDir(outdir.getFileName().toString());
    options.setMkDirs(true);
    options.setDestinationDir("out");
    options.setSafe(SafeMode.SAFE);

    // Wipe out directory
    FileUtils.cleanDirectory(outdir.toFile());

    copyFile(outdir,
        // images
        basedir.resolve("images"),
        // js
        basedir.resolve("js")
    );

    asciidoctor.convertFile(basedir.resolve("index.adoc").toFile(), options);

  }

  private static void copyFile(Path out, Path... dirs) throws IOException {
    for (Path dir : dirs) {
      FileUtils.copyDirectory(dir.toFile(), out.resolve(dir.getFileName().toString()).toFile());
    }
  }
}
