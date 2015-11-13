package org.jooby.aws;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import net.sf.cglib.proxy.Factory;

import org.jooby.Env;
import org.jooby.internal.aws.AwsManaged;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.typesafe.config.Config;

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
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("aws.s3.accessKey")).andReturn(false);
          expect(config.hasPath("aws.s3.secretKey")).andReturn(false);
          expect(config.hasPath("aws.s3.sessionToken")).andReturn(false);
          expect(config.hasPath("aws.sessionToken")).andReturn(false);
          expect(config.getString("aws.accessKey")).andReturn("accessKey");
          expect(config.getString("aws.secretKey")).andReturn("secretKey");
        })
        .expect(unit -> {
          AmazonWebServiceClient aws = unit.get(AmazonWebServiceClient.class);
          expect(aws.getServiceName()).andReturn("s3");
        })
        .expect(unit -> {
          AmazonWebServiceClient aws = unit.get(AmazonWebServiceClient.class);

          ScopedBindingBuilder sbbAWSC = unit.mock(ScopedBindingBuilder.class);
          sbbAWSC.asEagerSingleton();
          sbbAWSC.asEagerSingleton();

          AnnotatedBindingBuilder abbAWSC = unit.mock(AnnotatedBindingBuilder.class);
          expect(abbAWSC.toProvider(isA(AwsManaged.class))).andReturn(sbbAWSC).times(2);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Factory.class)).andReturn(abbAWSC);

          expect(binder.bind(aws.getClass())).andReturn(abbAWSC);
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
    AmazonWebServiceClient aws = new AmazonWebServiceClient(new ClientConfiguration()) {
      @Override
      public String getServiceName() {
        return "s3";
      }
    };
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("aws.s3.accessKey")).andReturn(false);
          expect(config.hasPath("aws.s3.secretKey")).andReturn(false);
          expect(config.hasPath("aws.s3.sessionToken")).andReturn(false);
          expect(config.hasPath("aws.sessionToken")).andReturn(false);
          expect(config.getString("aws.accessKey")).andReturn("accessKey");
          expect(config.getString("aws.secretKey")).andReturn("secretKey");
        })
        .expect(unit -> {
          ScopedBindingBuilder sbbAWSC = unit.mock(ScopedBindingBuilder.class);
          sbbAWSC.asEagerSingleton();

          AnnotatedBindingBuilder abbAWSC = unit.mock(AnnotatedBindingBuilder.class);
          expect(abbAWSC.toProvider(isA(AwsManaged.class))).andReturn(sbbAWSC);

          Binder binder = unit.get(Binder.class);

          expect(binder.bind(aws.getClass())).andReturn(abbAWSC);
        })
        .run(unit -> {
          new Aws()
              .with(creds -> aws)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

}
