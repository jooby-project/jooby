/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.whoops;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Frame {
  private static final int SAMPLE_SIZE = 10;

  private static final Set<String> VENDOR_PACKAGES =
      new HashSet<>(
          Arrays.asList(
              "java.",
              "javax.",
              "jakarta.",
              "sun.",
              "com.sun.",
              "org.eclipse.",
              "io.netty.",
              "org.hibernate.",
              "com.fasterxml.",
              "tools.jackson.",
              "org.slf4j",
              "io.undertow.",
              "io.jooby."));

  private String fileName;

  private String methodName;

  private int lineStart;

  private int line;

  private String location;

  private String source;

  private boolean open;

  private String className;

  private List<Throwable> comments;

  private Frame() {}

  public String getFileName() {
    return fileName;
  }

  public String getMethodName() {
    return methodName;
  }

  public int getLineStart() {
    return lineStart;
  }

  public int getLine() {
    return line;
  }

  public String getLocation() {
    return location;
  }

  public String getSource() {
    return source;
  }

  public boolean isOpen() {
    return open;
  }

  public String getClassName() {
    return className;
  }

  public List<Throwable> getComments() {
    return comments;
  }

  public boolean hasSource() {
    return source != null && !source.isEmpty();
  }

  public boolean shouldScan() {
    return VENDOR_PACKAGES.stream().noneMatch(it -> getClassName().startsWith(it));
  }

  public boolean isApplication() {
    return hasSource();
  }

  public static List<Frame> toFrames(SourceLocator locator, Throwable cause) {
    LinkedList<Throwable> causalChain = getCausalChain(cause);
    Throwable head = causalChain.getLast();
    List<Frame> frames =
        causalChain.stream()
            .filter(it -> it != head)
            .map(it -> toFrame(locator, it, it.getStackTrace()[0]))
            .collect(Collectors.toList());

    Stream.of(head.getStackTrace()).map(e -> toFrame(locator, head, e)).forEach(frames::add);

    return frames.stream().toList();
  }

  static Frame toFrame(
      final SourceLocator locator, final Throwable cause, final StackTraceElement e) {
    int line = Math.max(e.getLineNumber(), 1);
    String className = ofNullable(e.getClassName()).orElse("~unknown");
    String[] names = className.split("\\.");
    String filename = ofNullable(e.getFileName()).orElse(names[names.length - 1]);

    Frame frame = new Frame();
    frame.fileName = filename;
    frame.methodName = ofNullable(e.getMethodName()).orElse("~unknown");
    frame.line = line;
    frame.className =
        className
            // clean up kotlin generated class name: App$1$1 => App
            .replaceAll("\\$\\d+", "");
    frame.comments = Collections.singletonList(cause);

    if (frame.shouldScan()) {
      StringBuilder path = new StringBuilder();
      Stream.of(names)
          .limit(names.length - 1)
          .forEach(it -> path.append(it).append(File.separator));
      path.append(names[names.length - 1]);
      SourceLocator.Source source = locator.source(path.toString());
      SourceLocator.Preview preview = source.preview(line, SAMPLE_SIZE);

      frame.lineStart = preview.getLineStart();
      Path sourcePath = source.getPath();
      frame.location =
          sourcePath != null && sourcePath.isAbsolute()
              ? locator.getBasedir().relativize(sourcePath).toString()
              : filename;
      frame.source = preview.getCode();
    } else {
      frame.location = filename;
    }

    frame.open = false;
    return frame;
  }

  private static LinkedList<Throwable> getCausalChain(Throwable throwable) {
    LinkedList<Throwable> causes = new LinkedList<>();
    causes.add(throwable);

    // Keep a second pointer that slowly walks the causal chain. If the fast pointer ever catches
    // the slower pointer, then there's a loop.
    Throwable slowPointer = throwable;
    boolean advanceSlowPointer = false;

    Throwable cause;
    while ((cause = throwable.getCause()) != null) {
      throwable = cause;
      causes.add(throwable);

      if (throwable == slowPointer) {
        throw new IllegalArgumentException("Loop in causal chain detected.", throwable);
      }
      if (advanceSlowPointer) {
        slowPointer = slowPointer.getCause();
      }
      advanceSlowPointer = !advanceSlowPointer; // only advance every other iteration
    }
    return causes;
  }
}
