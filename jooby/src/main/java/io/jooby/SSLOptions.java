package io.jooby;

import com.typesafe.config.Config;

import javax.annotation.Nonnull;
import java.util.Optional;

public final class SSLOptions {
  public static final String X509 = "X509";

  public static final String PKCS12 = "PKCS12";

  private String password;

  private String type;

  private String cert;

  private String privateKey;

  private SSLOptions() {
  }

  public String getType() {
    return type;
  }

  public SSLOptions setType(String type) {
    this.type = type;
    return this;
  }

  public String getCert() {
    return cert;
  }

  public SSLOptions setCert(String cert) {
    this.cert = cert;
    return this;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public SSLOptions setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
    return this;
  }

  public static SSLOptions x509(String crt, String key) {
    SSLOptions options = new SSLOptions();
    options.setType(X509);
    options.setPrivateKey(key);
    options.setCert(crt);
    return options;
  }

  public static SSLOptions x509() {
    return x509("io/jooby/ssl/localhost.crt", "io/jooby/ssl/localhost.key");
  }

  public static SSLOptions pkcs12(String cert, String password) {
    SSLOptions options = new SSLOptions();
    options.setType(PKCS12);
    options.setCert(cert);
    options.setPassword(password);
    return options;
  }

  public static SSLOptions pkcs12() {
    return pkcs12("io/jooby/ssl/localhost.p12", "changeit");
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPassword() {
    return password;
  }

//  public static @Nonnull Optional<SSLOptions> parse(@Nonnull Config conf) {
//    if (conf.hasPath("server.ssl")) {
//      SSLOptions options = new SSLOptions();
//      if (conf.hasPath("sever.ssl.type")) {
//        String type = conf.getString("server.ssl.type").toUpperCase();
//        if (type.equals(PKCS12)) {
//          options.setType(type);
//        } else if (type.equals(X509)) {
//          options.setType(type);
//        } else {
//          throw new IllegalArgumentException("Unsupported SSL type: " + type.toUpperCase());
//        }
//      } else {
//        options.setType(PKCS12);
//      }
//      String type = options.getType();
//      switch (type) {
//        case X509: {
//
//        }
//      }
//
//      return Optional.of(options);
//    }
//    return Optional.empty();
//  }

  @Override public String toString() {
    return type;
  }
}
