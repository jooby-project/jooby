package org.jooby.aws;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.typesafe.config.Config;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.Env;
import org.jooby.internal.aws.AwsShutdownSupport;
import org.jooby.internal.aws.CredentialsFactory;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Aws.class, CredentialsFactory.class})
public class AwsTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .run(unit -> {
          new Aws()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void withServiceWithInterface() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, AmazonWebServiceClient.class)
        .expect(credentialsFactory("s3"))
        .expect(unit -> {
          AmazonWebServiceClient aws = unit.get(AmazonWebServiceClient.class);
          expect(aws.getServiceName()).andReturn("s3");
        })
        .expect(unit -> {
          AmazonWebServiceClient aws = unit.get(AmazonWebServiceClient.class);

          AnnotatedBindingBuilder abbAWSC = unit.mock(AnnotatedBindingBuilder.class);
          abbAWSC.toInstance(aws);
          abbAWSC.toInstance(aws);

          Binder binder = unit.get(Binder.class);

          expect(binder.bind(aws.getClass())).andReturn(abbAWSC);
          expect(binder.bind(aws.getClass().getInterfaces()[0])).andReturn(abbAWSC);
        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.onStop(isA(AwsShutdownSupport.class))).andReturn(env);
        })
        .run(unit -> {
          new Aws()
              .with(creds -> unit.get(AmazonWebServiceClient.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void withServiceWithoutInterface() throws Exception {
    class AmazonFooServiceClient {}

    AmazonFooServiceClient fooService = new AmazonFooServiceClient();

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(credentialsFactory("foo"))
        .expect(unit -> {
          AnnotatedBindingBuilder abbAWSC = unit.mock(AnnotatedBindingBuilder.class);
          abbAWSC.toInstance(fooService);

          Binder binder = unit.get(Binder.class);

          expect(binder.bind(fooService.getClass())).andReturn(abbAWSC);
        })
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.onStop(isA(AwsShutdownSupport.class))).andReturn(env);
        })
        .run(unit -> {
          new Aws()
              .with(creds -> fooService)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private MockUnit.Block credentialsFactory(String serviceName) {
    return unit -> {
      unit.mockStatic(CredentialsFactory.class);

      AWSCredentialsProvider provider = unit.mock(AWSCredentialsProvider.class);
      unit.registerMock(AWSCredentialsProvider.class, provider);
      expect(CredentialsFactory.create(unit.get(Config.class), serviceName)).andReturn(provider);
    };
  }
}
