package io.jooby.internal;

import io.jooby.SSLOptions;
import io.jooby.SSLContextProvider;
import io.jooby.SneakyThrows;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SSLDefaultProvider implements SSLContextProvider {

  private static final String PKCS12 = "PKCS12";

  @Override public boolean supports(String type) {
    return PKCS12.equalsIgnoreCase(type);
  }

  @Override public SSLContext create(ClassLoader loader, SSLOptions options) {
    try (InputStream crt = SSLContextProvider.loadFile(loader, options.getCert())) {
      KeyStore store = KeyStore.getInstance(options.getType());
      store.load(crt, options.getPassword().toCharArray());
      KeyManagerFactory kmf = KeyManagerFactory
          .getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(store, options.getPassword().toCharArray());
      KeyManager[] kms = kmf.getKeyManagers();
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(kms, null, SecureRandom.getInstanceStrong());
      return context;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
