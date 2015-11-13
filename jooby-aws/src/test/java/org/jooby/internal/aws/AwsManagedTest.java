package org.jooby.internal.aws;

import static org.junit.Assert.assertEquals;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.amazonaws.AmazonWebServiceClient;

public class AwsManagedTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(AmazonWebServiceClient.class)
        .run(unit -> {
          new AwsManaged(unit.get(AmazonWebServiceClient.class));
        });
  }

  @Test
  public void start() throws Exception {
    new MockUnit(AmazonWebServiceClient.class)
        .run(unit -> {
          new AwsManaged(unit.get(AmazonWebServiceClient.class)).start();
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(AmazonWebServiceClient.class)
        .run(unit -> {
          assertEquals(unit.get(AmazonWebServiceClient.class),
              new AwsManaged(unit.get(AmazonWebServiceClient.class)).get());
        });
  }

  @Test
  public void stop() throws Exception {
    new MockUnit(AmazonWebServiceClient.class)
        .expect(unit -> {
          unit.get(AmazonWebServiceClient.class).shutdown();
        })
        .run(unit -> {
          AwsManaged aws = new AwsManaged(unit.get(AmazonWebServiceClient.class));
          aws.stop();
          aws.stop();
        });
  }

}
