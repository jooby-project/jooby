package io.jooby.internal.openapi;

public enum HttpType {
  CONTEXT,
  HEADER {
    @Override public String in() {
      return "header";
    }
  },
  COOKIE {
    @Override public String in() {
      return "cookie";
    }
  },
  PATH {
    @Override public String in() {
      return "path";
    }
  },
  QUERY {
    @Override public String in() {
      return "query";
    }
  },
  FORM,

  BODY;

  public String in() {
    return null;
  }
}
