package io.netty.channel.epoll;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hack for Epoll test.
 *
 * @author edgar
 *
 */
public class Epoll {

  public static final AtomicBoolean available = new AtomicBoolean(false);

  public static boolean isAvailable() {
    return available.get();
  }

  public static void ensureAvailability() {
  }

  public static Throwable unavailabilityCause() {
    return null;
  }
}
