package org.jooby.internal.aws;

import static org.junit.Assert.assertEquals;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.amazonaws.AmazonWebServiceClient;

public class AwsGenericManagedTest {

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
          new AwsGenericManaged(unit.get(AmazonWebServiceClient.class));
        });
  }

  @Test
  public void start() throws Exception {
    new MockUnit(AmazonWebServiceClient.class)
        .run(unit -> {
          new AwsGenericManaged(unit.get(AmazonWebServiceClient.class)).start();
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(AmazonWebServiceClient.class)
        .run(unit -> {
          assertEquals(unit.get(AmazonWebServiceClient.class),
              new AwsGenericManaged(unit.get(AmazonWebServiceClient.class)).get());
        });
  }

  @Test
  public void stop() throws Exception {
    new MockUnit(AmazonWebServiceClient.class)
        .expect(unit -> {
          unit.get(AmazonWebServiceClient.class).shutdown();
        })
        .run(unit -> {
          AwsGenericManaged aws = new AwsGenericManaged(unit.get(AmazonWebServiceClient.class));
          aws.stop();
          aws.stop();
        });
  }

  @Test
  public void nostop() throws Exception {
    new MockUnit(NoShutdown.class)
        .run(unit -> {
          AwsGenericManaged aws = new AwsGenericManaged(unit.get(NoShutdown.class));
          aws.stop();
        });
  }

  @Test
  @SuppressWarnings("unused")
  public void shouldIgnorePrivateStop() throws Exception {
    new MockUnit()
        .run(unit -> {
          AwsGenericManaged aws = new AwsGenericManaged(new Object() {
            private void shutdown() {
              throw new UnsupportedOperationException();
            }
          });
          aws.stop();
        });
  }

  @Test
  public void shutdownOverloaded() throws Exception {
    new MockUnit(ShutdownOverloaded.class)
        .expect(unit -> {
          unit.get(ShutdownOverloaded.class).shutdownNow();
        })
        .run(unit -> {
          AwsGenericManaged aws = new AwsGenericManaged(unit.get(ShutdownOverloaded.class));
          aws.stop();
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void stopErr() throws Exception {
    new MockUnit()
        .run(unit -> {
          AwsGenericManaged aws = new AwsGenericManaged(new ShutdownErr());
          aws.stop();
        });
  }

  @Test(expected = IllegalStateException.class)
  @SuppressWarnings("unused")
  public void stopNoRuntimeErr() throws Exception {
    new MockUnit()
        .run(unit -> {
          AwsGenericManaged aws = new AwsGenericManaged(new Object() {
            public void shutdown() throws Throwable {
              throw new Throwable();
            }
          });
          aws.stop();
        });
  }

}
