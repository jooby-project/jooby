package org.jooby.internal.ssl;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.net.ssl.SSLContext;

import com.google.common.base.Throwables;
import com.typesafe.config.Config;

public class SslContextProvider implements Provider<SSLContext> {

  private Config conf;

  @Inject
  public SslContextProvider(final Config conf) {
    this.conf = requireNonNull(conf, "SSL config is required.");
  }

  @Override
  public SSLContext get() {
    try {
      String tmpdir = conf.getString("application.tmpdir");
      File keyStoreCert = toFile(conf.getString("ssl.keystore.cert"), tmpdir);
      File keyStoreKey = toFile(conf.getString("ssl.keystore.key"), tmpdir);
      String keyStorePass = conf.hasPath("ssl.keystore.password")
          ? conf.getString("ssl.keystore.password") : null;

      File trustCert = conf.hasPath("ssl.trust.cert")
          ? toFile(conf.getString("ssl.trust.cert"), tmpdir) : null;

      return SslContext
          .newServerContextInternal(trustCert, keyStoreCert, keyStoreKey, keyStorePass,
              conf.getLong("ssl.session.cacheSize"), conf.getLong("ssl.session.timeout"))
          .context();
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }

  private File toFile(final String path, final String tmpdir) throws IOException {
    File file = new File(path);
    if (file.exists()) {
      return file;
    }
    file = new File(tmpdir, Paths.get(path).getFileName().toString());
    // classpath resource?
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new FileNotFoundException(path);
      }
      Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    file.deleteOnExit();
    return file;
  }

}
