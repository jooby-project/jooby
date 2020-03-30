/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.whoops;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public class Frame {
  private static final int SAMPLE_SIZE = 10;

  private String fileName;

  private String methodName;

  private int lineStart;

  private int line;

  private String location;

  private String source;

  private boolean open;

  private String className;

  private List<Throwable> comments;

  private Frame() {
  }

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
    return source != null && source.length() > 0;
  }

  public static List<Frame> toFrames(SourceLocator locator, Throwable cause) {
    LinkedList<Throwable> causalChain = getCausalChain(cause);
    Throwable head = causalChain.getLast();
    List<Frame> frames = causalChain.stream()
        .filter(it -> it != head)
        .map(it -> toFrame(locator, it, it.getStackTrace()[0]))
        .collect(Collectors.toList());

    Stream.of(head.getStackTrace())
        .map(e -> toFrame(locator, head, e))
        .forEach(frames::add);

    // Keep application frames (ignore all others)
    return frames.stream()
        .filter(Frame::hasSource)
        .collect(Collectors.toList());
  }

  static Frame toFrame(final SourceLocator locator, final Throwable cause,
      final StackTraceElement e) {
    int line = Math.max(e.getLineNumber(), 1);
    String className = ofNullable(e.getClassName()).orElse("~unknown");
    String filename = ofNullable(e.getFileName()).orElse(className.replace(".", File.separator));

    SourceLocator.Source source = locator.source(filename);
    SourceLocator.Preview preview = source.preview(line, SAMPLE_SIZE);

    Frame frame = new Frame();
    frame.fileName = filename;
    frame.methodName = ofNullable(e.getMethodName()).orElse("~unknown");
    frame.lineStart = preview.getLineStart();
    frame.line = line;
    frame.location = Files.exists(source.getPath())
        ? locator.getBasedir().relativize(source.getPath()).toString()
        : filename;
    frame.source = preview.getCode();
    frame.open = false;
    frame.className = className
        // clean up kotlin generated class name: App$1$1 => App
        .replaceAll("\\$\\d+", "");
    frame.comments = Collections.singletonList(cause);
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
