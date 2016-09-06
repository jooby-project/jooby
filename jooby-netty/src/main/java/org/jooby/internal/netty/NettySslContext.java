/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.netty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.cert.CertificateException;
import java.util.Arrays;

import com.google.common.io.Closeables;
import com.typesafe.config.Config;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

public class NettySslContext {

  static SslContext build(final Config conf) throws IOException, CertificateException {
    String tmpdir = conf.getString("application.tmpdir");
    boolean http2 = conf.getBoolean("server.http2.enabled");
    File keyStoreCert = toFile(conf.getString("ssl.keystore.cert"), tmpdir);
    File keyStoreKey = toFile(conf.getString("ssl.keystore.key"), tmpdir);
    String keyStorePass = conf.hasPath("ssl.keystore.password")
        ? conf.getString("ssl.keystore.password") : null;
    SslContextBuilder scb = SslContextBuilder.forServer(keyStoreCert, keyStoreKey, keyStorePass);
    if (conf.hasPath("ssl.trust.cert")) {
      scb.trustManager(toFile(conf.getString("ssl.trust.cert"), tmpdir));
    }
    if (http2) {
      SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
      return scb.sslProvider(provider)
          .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
          .applicationProtocolConfig(new ApplicationProtocolConfig(
              Protocol.ALPN,
              SelectorFailureBehavior.NO_ADVERTISE,
              SelectedListenerFailureBehavior.ACCEPT,
              Arrays.asList(ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1)))
          .build();
    }
    return scb.build();
  }

  static File toFile(final String path, final String tmpdir) throws IOException {
    File file = new File(path);
    if (file.exists()) {
      return file;
    }
    file = new File(tmpdir, Paths.get(path).getFileName().toString());
    // classpath resource?
    InputStream in = NettyServer.class.getClassLoader().getResourceAsStream(path);
    if (in == null) {
      throw new FileNotFoundException(path);
    }
    try {
      Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
      file.deleteOnExit();
      return file;
    } finally {
      Closeables.close(in, true);
    }
  }

}
