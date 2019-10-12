package io.jooby.ssl;

import io.jooby.SSLContextProvider;
import io.jooby.SSLOptions;
import io.jooby.SneakyThrows;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

public class SSLX509Provider implements SSLContextProvider {
  @Override public boolean supports(String type) {
    return "X509".equalsIgnoreCase(type);
  }

  @Override public SSLContext create(ClassLoader loader, SSLOptions options) {
    try (InputStream crt = loader.getResourceAsStream(options.getCert());
        InputStream key = loader.getResourceAsStream(options.getPrivateKey())) {
      JdkSslContext sslContext = (JdkSslContext) SslContextBuilder.forServer(crt, key)
          .sslProvider(SslProvider.JDK)
          .build();
      return sslContext.context();
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
