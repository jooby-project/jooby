package org.jooby.internal.aws;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.typesafe.config.Config;

public class ConfigCredentialsProviderTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          new ConfigCredentialsProvider(unit.get(Config.class));
        });
  }

  @Test
  public void refresh() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          new ConfigCredentialsProvider(unit.get(Config.class)).refresh();
        });
  }

  @Test
  public void serviceWithDefaultConfig() throws Exception {
    String accessKey = "accessKey";
    String secretKey = "secretKey";
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("aws.s3.accessKey")).andReturn(false);
          expect(config.hasPath("aws.s3.secretKey")).andReturn(false);
          expect(config.hasPath("aws.s3.sessionToken")).andReturn(false);
          expect(config.hasPath("aws.sessionToken")).andReturn(false);
          expect(config.getString("aws.accessKey")).andReturn(accessKey);
          expect(config.getString("aws.secretKey")).andReturn(secretKey);
        })
        .run(unit -> {
          AWSCredentials creds = new ConfigCredentialsProvider(unit.get(Config.class))
              .service("s3").getCredentials();
          assertEquals("accessKey", creds.getAWSAccessKeyId());
          assertEquals("secretKey", creds.getAWSSecretKey());
        });
  }

  @Test
  public void serviceWithSessionToken() throws Exception {
    String accessKey = "accessKey";
    String secretKey = "secretKey";
    String sessionToken = "sessionToken";
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("aws.s3.accessKey")).andReturn(false);
          expect(config.hasPath("aws.s3.secretKey")).andReturn(false);
          expect(config.hasPath("aws.s3.sessionToken")).andReturn(false);
          expect(config.hasPath("aws.sessionToken")).andReturn(true);
          expect(config.getString("aws.accessKey")).andReturn(accessKey);
          expect(config.getString("aws.secretKey")).andReturn(secretKey);
          expect(config.getString("aws.sessionToken")).andReturn(sessionToken);
        })
        .run(unit -> {
          AWSSessionCredentials creds = (AWSSessionCredentials) new ConfigCredentialsProvider(
              unit.get(Config.class))
              .service("s3").getCredentials();
          assertEquals("accessKey", creds.getAWSAccessKeyId());
          assertEquals("secretKey", creds.getAWSSecretKey());
          assertEquals("sessionToken", creds.getSessionToken());
        });
  }

  @Test
  public void serviceWithCustomSessionToken() throws Exception {
    String accessKey = "accessKey";
    String secretKey = "secretKey";
    String sessionToken = "sessionToken";
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("aws.s3.accessKey")).andReturn(false);
          expect(config.hasPath("aws.s3.secretKey")).andReturn(false);
          expect(config.hasPath("aws.s3.sessionToken")).andReturn(true);
          expect(config.getString("aws.accessKey")).andReturn(accessKey);
          expect(config.getString("aws.secretKey")).andReturn(secretKey);
          expect(config.getString("aws.s3.sessionToken")).andReturn(sessionToken);
        })
        .run(unit -> {
          AWSSessionCredentials creds = (AWSSessionCredentials) new ConfigCredentialsProvider(
              unit.get(Config.class))
              .service("s3").getCredentials();
          assertEquals("accessKey", creds.getAWSAccessKeyId());
          assertEquals("secretKey", creds.getAWSSecretKey());
          assertEquals("sessionToken", creds.getSessionToken());
        });
  }

  @Test
  public void serviceWithCustomAccessKey() throws Exception {
    String accessKey = "s3accessKey";
    String secretKey = "secretKey";
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("aws.s3.accessKey")).andReturn(true);
          expect(config.hasPath("aws.s3.secretKey")).andReturn(false);
          expect(config.hasPath("aws.s3.sessionToken")).andReturn(false);
          expect(config.hasPath("aws.sessionToken")).andReturn(false);
          expect(config.getString("aws.s3.accessKey")).andReturn(accessKey);
          expect(config.getString("aws.secretKey")).andReturn(secretKey);
        })
        .run(unit -> {
          AWSCredentials creds = new ConfigCredentialsProvider(unit.get(Config.class))
              .service("s3").getCredentials();
          assertEquals("s3accessKey", creds.getAWSAccessKeyId());
          assertEquals("secretKey", creds.getAWSSecretKey());
        });
  }

  @Test
  public void serviceWithCustomSecretKey() throws Exception {
    String accessKey = "accessKey";
    String secretKey = "s3secretKey";
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("aws.s3.accessKey")).andReturn(false);
          expect(config.hasPath("aws.s3.secretKey")).andReturn(true);
          expect(config.hasPath("aws.s3.sessionToken")).andReturn(false);
          expect(config.hasPath("aws.sessionToken")).andReturn(false);
          expect(config.getString("aws.accessKey")).andReturn(accessKey);
          expect(config.getString("aws.s3.secretKey")).andReturn(secretKey);
        })
        .run(unit -> {
          AWSCredentials creds = new ConfigCredentialsProvider(unit.get(Config.class))
              .service("s3").getCredentials();
          assertEquals("accessKey", creds.getAWSAccessKeyId());
          assertEquals("s3secretKey", creds.getAWSSecretKey());
        });
  }

}
