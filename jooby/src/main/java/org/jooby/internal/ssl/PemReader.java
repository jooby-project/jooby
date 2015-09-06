package org.jooby.internal.ssl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;

/**
 * Reads a PEM file and converts it into a list of DERs so that they are imported into a
 * {@link KeyStore} easily.
 */
final class PemReader {

  private static final Pattern CERT_PATTERN = Pattern.compile(
      "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
          "([a-z0-9+/=\\r\\n]+)" + // Base64 text
          "-+END\\s+.*CERTIFICATE[^-]*-+", // Footer
      Pattern.CASE_INSENSITIVE);
  private static final Pattern KEY_PATTERN = Pattern.compile(
      "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
          "([a-z0-9+/=\\r\\n]+)" + // Base64 text
          "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+", // Footer
      Pattern.CASE_INSENSITIVE);

  static List<ByteBuffer> readCertificates(final File file)
      throws CertificateException, IOException {
    String content = Files.toString(file, StandardCharsets.US_ASCII);

    BaseEncoding base64 = base64();
    List<ByteBuffer> certs = new ArrayList<ByteBuffer>();
    Matcher m = CERT_PATTERN.matcher(content);
    int start = 0;
    while (m.find(start)) {
      ByteBuffer buffer = ByteBuffer.wrap(base64.decode(m.group(1)));
      certs.add(buffer);

      start = m.end();
    }

    if (certs.isEmpty()) {
      throw new CertificateException("found no certificates: " + file);
    }

    return certs;
  }

  private static BaseEncoding base64() {
    return BaseEncoding.base64().withSeparator("\n", '\n');
  }

  static ByteBuffer readPrivateKey(final File file) throws KeyException, IOException {
    String content = Files.toString(file, StandardCharsets.US_ASCII);

    Matcher m = KEY_PATTERN.matcher(content);
    if (!m.find()) {
      throw new KeyException("found no private key: " + file);
    }

    String value = m.group(1);
    return ByteBuffer.wrap(base64().decode(value));
  }

  private PemReader() {
  }
}
