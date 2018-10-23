package io.jooby;

import javax.annotation.Nonnull;
import java.util.LinkedList;

/**
 * Collection of utility functions around IO, exceptions, etc..
 *
 * @author edgar
 */
public final class Functions {

  public static class Closer implements AutoCloseable {

    private LinkedList<AutoCloseable> stack = new LinkedList<>();

    public Closer register(@Nonnull AutoCloseable closeable) {
      stack.addLast(closeable);
      return this;
    }

    @Override public void close() {
      Throwable root = null;
      while (!stack.isEmpty()) {
        AutoCloseable closeable = stack.removeFirst();
        try {
          closeable.close();
        } catch (Throwable x) {
          if (root == null) {
            root = x;
          } else {
            root.addSuppressed(x);
          }
        }
      }
      stack = null;
      if (root != null) {
        throw sneakyThrow(root);
      }
    }
  }

  public static Closer closer() {
    return new Closer();
  }

  /**
   * Throws any throwable 'sneakily' - you don't need to catch it, nor declare that you throw it
   * onwards.
   * The exception is still thrown - javac will just stop whining about it.
   * <p>
   * Example usage:
   * <pre>public void run() {
   *     throw sneakyThrow(new IOException("You don't need to catch me!"));
   * }</pre>
   * <p>
   * NB: The exception is not wrapped, ignored, swallowed, or redefined. The JVM actually does not
   * know or care
   * about the concept of a 'checked exception'. All this method does is hide the act of throwing a
   * checked exception from the java compiler.
   * <p>
   * Note that this method has a return type of {@code RuntimeException}; it is advised you always
   * call this
   * method as argument to the {@code throw} statement to avoid compiler errors regarding no return
   * statement and similar problems. This method won't of course return an actual
   * {@code RuntimeException} -
   * it never returns, it always throws the provided exception.
   *
   * @param x The throwable to throw without requiring you to catch its type.
   * @return A dummy RuntimeException; this method never returns normally, it <em>always</em> throws
   *         an exception!
   */
  public static RuntimeException sneakyThrow(final Throwable x) {
    if (x == null) {
      throw new NullPointerException("x");
    }

    sneakyThrow0(x);
    return null;
  }

  /**
   * True if the given exception is one of {@link InterruptedException}, {@link LinkageError},
   * {@link ThreadDeath}, {@link VirtualMachineError}.
   *
   * @param x Exception to test.
   * @return True if the given exception is one of {@link InterruptedException}, {@link LinkageError},
   *     {@link ThreadDeath}, {@link VirtualMachineError}.
   */
  public static boolean isFatal(Throwable x) {
    return x instanceof InterruptedException ||
        x instanceof LinkageError ||
        x instanceof ThreadDeath ||
        x instanceof VirtualMachineError;
  }

  /**
   * Make a checked exception un-checked and rethrow it.
   *
   * @param x Exception to throw.
   * @param <E> Exception type.
   * @throws E Exception to throw.
   */
  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow0(final Throwable x) throws E {
    throw (E) x;
  }
}
