package org.jooby.internal.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import static org.easymock.EasyMock.expect;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ForwardingCredentialsProviderTest {

  @Test
  public void credentials() throws Exception {
    BasicAWSCredentials creds = new BasicAWSCredentials("accessKey", "secretKey");
    new MockUnit(AWSCredentialsProvider.class)
        .expect(unit -> {
          AWSCredentialsProvider provider = unit.get(AWSCredentialsProvider.class);
          expect(provider.getCredentials()).andReturn(creds);
        })
        .run(unit -> {
          ForwardingCredentialsProvider forwarding = new ForwardingCredentialsProvider();
          forwarding.setProvider(unit.get(AWSCredentialsProvider.class));

          assertEquals(creds, forwarding.getCredentials());
        });
  }

  @Test
  public void refresh() throws Exception {
    new MockUnit(AWSCredentialsProvider.class)
        .expect(unit -> {
          AWSCredentialsProvider provider = unit.get(AWSCredentialsProvider.class);
          provider.refresh();
        })
        .run(unit -> {
          ForwardingCredentialsProvider forwarding = new ForwardingCredentialsProvider();
          forwarding.setProvider(unit.get(AWSCredentialsProvider.class));

          forwarding.refresh();
        });
  }
}
