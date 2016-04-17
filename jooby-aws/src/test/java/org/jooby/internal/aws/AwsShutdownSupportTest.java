package org.jooby.internal.aws;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.amazonaws.AmazonWebServiceClient;

public class AwsShutdownSupportTest {

  public static class NoShutdown {
  }

  public static class ShutdownOverloaded {

    public void shutdownNow() {
    }

    public void shutdownNow(final boolean now) {
    }
  }

  public static class ShutdownErr {

    public void shutdown() {
      throw new UnsupportedOperationException();
    }
  }

  @Test
  public void defaults() throws Exception {
    new MockUnit(AmazonWebServiceClient.class)
        .run(unit -> {
          new AwsShutdownSupport(unit.get(AmazonWebServiceClient.class));
        });
  }

  @Test
  public void stop() throws Exception {
    new MockUnit(AmazonWebServiceClient.class)
        .expect(unit -> {
          unit.get(AmazonWebServiceClient.class).shutdown();
        })
        .run(unit -> {
          AwsShutdownSupport aws = new AwsShutdownSupport(unit.get(AmazonWebServiceClient.class));
          aws.run();
          aws.run();
        });
  }

  @Test
  public void nostop() throws Exception {
    new MockUnit(NoShutdown.class)
        .run(unit -> {
          AwsShutdownSupport aws = new AwsShutdownSupport(unit.get(NoShutdown.class));
          aws.run();
        });
  }

  @Test
  @SuppressWarnings("unused")
  public void shouldIgnorePrivateStop() throws Exception {
    new MockUnit()
        .run(unit -> {
          AwsShutdownSupport aws = new AwsShutdownSupport(new Object() {
            private void shutdown() {
              throw new UnsupportedOperationException();
            }
          });
          aws.run();
        });
  }

  @Test
  public void shutdownOverloaded() throws Exception {
    new MockUnit(ShutdownOverloaded.class)
        .expect(unit -> {
          unit.get(ShutdownOverloaded.class).shutdownNow();
        })
        .run(unit -> {
          AwsShutdownSupport aws = new AwsShutdownSupport(unit.get(ShutdownOverloaded.class));
          aws.run();
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void stopErr() throws Exception {
    new MockUnit()
        .run(unit -> {
          AwsShutdownSupport aws = new AwsShutdownSupport(new ShutdownErr());
          aws.run();
        });
  }

  @Test(expected = IllegalStateException.class)
  @SuppressWarnings("unused")
  public void stopNoRuntimeErr() throws Exception {
    new MockUnit()
        .run(unit -> {
          AwsShutdownSupport aws = new AwsShutdownSupport(new Object() {
            public void shutdown() throws Throwable {
              throw new Throwable();
            }
          });
          aws.run();
        });
  }

}
