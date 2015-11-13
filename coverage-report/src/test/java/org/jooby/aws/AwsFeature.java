package org.jooby.aws;

import static org.junit.Assert.assertEquals;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class AwsFeature extends ServerFeature {

  {
    use(ConfigFactory
        .empty()
        .withValue("aws.email.accessKey",
            ConfigValueFactory.fromAnyRef("123"))
        .withValue("aws.email.secretKey",
            ConfigValueFactory.fromAnyRef("1234"))
        .withValue("aws.s3.accessKey",
            ConfigValueFactory.fromAnyRef("123"))
        .withValue("aws.s3.secretKey",
            ConfigValueFactory.fromAnyRef("1234")));

    use(new Aws()
        .with(creds -> new AmazonS3Client(creds))
        .with(creds -> new AmazonSimpleEmailServiceClient(creds))
        .doWith((final AmazonS3Client s3) -> {
          return new TransferManager(s3);
        }));

    get("/aws/s3", req -> {
      AmazonS3 s3 = req.require(AmazonS3.class);
      TransferManager trx = req.require(TransferManager.class);
      assertEquals(s3, trx.getAmazonS3Client());
      return "s3";
    });

    get("/aws/ses", req -> {
      req.require(AmazonSimpleEmailService.class);
      return "email";
    });
  }

  @Test
  public void s3() throws Exception {
    request()
        .get("/aws/s3")
        .expect("s3");
  }

  @Test
  public void ses() throws Exception {
    request()
        .get("/aws/ses")
        .expect("email");
  }

}
