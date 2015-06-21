package org.jooby.internal;

import java.lang.management.ManagementFactory;

public class JvmInfo {

  /**
   * @return Get JVM PID.
   */
  public static long pid() {
    try {
      return Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    } catch (Exception e) {
      return -1;
    }
  }
}
