package io.jooby.internal.whoops;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Frame {
  private static final int SAMPLE_SIZE = 10;

  private String fileName;

  private String methodName;

  private int lineNumber;

  private int lineStart;

  private int lineNth;

  private String location;

  private String source;

  private String open;

  private String className;

  private List<FrameComment> comments;

  public String getFileName() {
    return fileName;
  }

  public String getMethodName() {
    return methodName;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public int getLineStart() {
    return lineStart;
  }

  public int getLineNth() {
    return lineNth;
  }

  public String getLocation() {
    return location;
  }

  public String getSource() {
    return source;
  }

  public String getOpen() {
    return open;
  }

  public String getClassName() {
    return className;
  }

  public List<FrameComment> getComments() {
    return comments;
  }

  public boolean hasSource() {
    return source != null && source.length() > 0;
  }

  public static List<Frame> toFrames(SourceLocator locator, Throwable cause) {
    List<Throwable> causal = getCausalChain(cause);
    Throwable head = causal.get(causal.size() - 1);
    List<Frame> frames = causal.stream()
        .filter(it -> it != head)
        .map(it -> toFrame(locator, it, it.getStackTrace()[0]))
        .collect(Collectors.toList());

    frames.addAll(frames(locator, head));

    // truncate frames
    return frames.stream()
        .filter(Frame::hasSource)
        .collect(Collectors.toList());
  }

  private static List<Frame> frames(final SourceLocator locator, final Throwable cause) {
    List<StackTraceElement> stacktrace = Arrays.asList(cause.getStackTrace());
    //    int limit = IntStream.range(0, stacktrace.size())
    //        .filter(i -> stacktrace.get(i).getClassName().equals(HANDLER)).findFirst()
    //        .orElse(stacktrace.size());
    return stacktrace.stream()
        // trunk stack at HttpHandlerImpl (skip servers stack)
        //        .limit(limit)
        .map(e -> toFrame(locator, cause, e))
        .collect(Collectors.toList());
  }

  static Frame toFrame(final SourceLocator locator,
      final Throwable cause, final StackTraceElement e) {
    int line = Math.max(e.getLineNumber(), 1);
    String className = e.getClassName();
    SourceLocator.Source source = locator.source(className);
    int[] range = source.range(line, SAMPLE_SIZE);
    int lineStart = range[0];
    int lineNth = line - lineStart;
    //    Path filePath = source.getPath();
    Optional<Class> clazz = locator.findClass(className);
    String filename = Optional.ofNullable(e.getFileName()).orElse("~unknown");
    Frame frame = new Frame();
    frame.fileName = filename;
    frame.methodName = Optional.ofNullable(e.getMethodName()).orElse("~unknown");
    frame.lineNumber = line;
    frame.lineStart = lineStart + 1;
    frame.lineNth = lineNth;
    frame.location = Files.exists(source.getPath())
        ? locator.getBasedir().relativize(source.getPath()).toString()
        : filename;
    frame.source = source.source(range[0], range[1]);
    frame.open = "";
    frame.className = clazz.map(Class::getName).orElse("~unknown");
    frame.comments = Arrays.asList(new FrameComment(cause.getClass().getName(),
        Optional.ofNullable(cause.getMessage()).orElse("")));
    return frame;
  }

  private static List<Throwable> getCausalChain(Throwable throwable) {
    List<Throwable> causes = new ArrayList<>(4);
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
