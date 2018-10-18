package io.jooby.internal.netty;

final class NettyUtils {

  public static String pathOnly(String uri) {
    int len = uri.length();
    for (int i = 0; i < len; i++) {
      char c = uri.charAt(i);
      if (c == '?') {
        return uri.substring(0, i);
      }
    }
    return uri;
  }
}
