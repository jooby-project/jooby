package app.ns;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.Service;

public class GuavaService implements Service {

  @Override
  public Service startAsync() {
    return null;
  }

  @Override
  public boolean isRunning() {
    return false;
  }

  @Override
  public State state() {
    return null;
  }

  @Override
  public Service stopAsync() {
    return null;
  }

  @Override
  public void awaitRunning() {
  }

  @Override
  public void awaitRunning(final long timeout, final TimeUnit unit) throws TimeoutException {
  }

  @Override
  public void awaitTerminated() {
  }

  @Override
  public void awaitTerminated(final long timeout, final TimeUnit unit) throws TimeoutException {
  }

  @Override
  public Throwable failureCause() {
    return null;
  }

  @Override
  public void addListener(final Listener listener, final Executor executor) {
  }

}
