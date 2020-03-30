package io.jooby.internal.whoops;

public class FrameComment {
  private String context;

  private String text;

  public FrameComment(String context, String text) {
    this.context = context;
    this.text = text;
  }

  public String getContext() {
    return context;
  }

  public String getText() {
    return text;
  }
}
