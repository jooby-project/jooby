package jooby;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.io.BaseEncoding;

/**
 * A HTTP Cookie.
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Cookie {

  class Definition {

    private String name;

    private String value;

    private String domain;

    private String path;

    private String comment;

    private Boolean httpOnly;

    private Boolean secure;

    private Integer maxAge;

    private Boolean signed;

    public Definition(final String name) {
      name(name);
    }

    public Definition(final String name, final String value) {
      name(name);
      value(value);
    }

    protected Definition() {
    }

    public Cookie toCookie() {
      return toCookie(this);
    }

    private static Cookie toCookie(final Definition cookie) {
      return new Cookie() {

        @Override
        public String name() {
          return cookie.name;
        }

        @Override
        public String value() {
          return cookie.value;
        }

        @Override
        public String comment() {
          return cookie.comment;
        }

        @Override
        public String domain() {
          return cookie.domain;
        }

        @Override
        public int maxAge() {
          return cookie.maxAge().orElse(-1);
        }

        @Override
        public String path() {
          return cookie.path().orElse("/");
        }

        @Override
        public boolean secure() {
          return cookie.secure().orElse(Boolean.FALSE);
        }

        @Override
        public int version() {
          return 0;
        }

        @Override
        public boolean httpOnly() {
          return cookie.httpOnly().orElse(Boolean.FALSE);
        }

      };
    }

    public Definition name(final String name) {
      this.name = requireNonNull(name, "A cookie name is required.");
      return this;
    }

    public Optional<String> name() {
      return Optional.ofNullable(name);
    }

    public Definition value(final String value) {
      this.value = requireNonNull(value, "A cookie value is required.");
      return this;
    }

    public Optional<String> value() {
      return Optional.ofNullable(value);
    }

    public Definition domain(final String domain) {
      this.domain = requireNonNull(domain, "A cookie domain is required.");
      return this;
    }

    public Optional<String> domain() {
      return Optional.ofNullable(domain);
    }

    public Definition path(final String path) {
      this.path = requireNonNull(path, "A cookie path is required.");
      return this;
    }

    public Optional<String> path() {
      return Optional.ofNullable(path);
    }

    public Definition comment(final String comment) {
      this.comment = requireNonNull(comment, "A cookie comment is required.");
      return this;
    }

    public Optional<String> comment() {
      return Optional.ofNullable(comment);
    }

    public Definition httpOnly(final boolean httpOnly) {
      this.httpOnly = httpOnly;
      return this;
    }

    public Optional<Boolean> httpOnly() {
      return Optional.ofNullable(httpOnly);
    }

    public Definition secure(final boolean secure) {
      this.secure = secure;
      return this;
    }

    public Optional<Boolean> secure() {
      return Optional.ofNullable(secure);
    }

    public Definition maxAge(final int maxAge) {
      this.maxAge = maxAge;
      return this;
    }

    public Optional<Integer> maxAge() {
      return Optional.ofNullable(maxAge);
    }

    public Optional<Boolean> signed() {
      return Optional.ofNullable(signed);
    }

    public Definition signed(final boolean signed) {
      this.signed = signed;
      return this;
    }
  }

  class Signature {

    private static final Pattern EQ = Pattern.compile("=+$");

    private static final String HMAC_SHA256 = "HmacSHA256";

    private static final String SEP = "|";

    public static String sign(final String value, final String secret) throws Exception {
      requireNonNull(value, "A value is required.");
      requireNonNull(secret, "A secret is required.");
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(secret.getBytes(), HMAC_SHA256));
      byte[] bytes = mac.doFinal(value.getBytes());
      return value + SEP + EQ.matcher(BaseEncoding.base64().encode(bytes)).replaceAll("");
    }

    public static String unsign(final String value, final String secret) throws Exception {
      requireNonNull(value, "A value is required.");
      requireNonNull(secret, "A secret is required.");
      int dot = value.indexOf(SEP);
      if (dot <= 0) {
        dot = value.length();
      }
      return sign(value.substring(0, dot), secret);
    }

  }

  /**
   * @return Cookie's name.
   */
  String name();

  /**
   * @return Cookie's value.
   */
  String value();

  /**
   * @return An optional comment.
   */
  String comment();

  /**
   * @return Cookie's domain.
   */
  String domain();

  /**
   * @return Cookie's max age.
   */
  int maxAge();

  /**
   * @return Cookie's path.
   */
  String path();

  /**
   * @return True for secured cookies (https).
   */
  boolean secure();

  /**
   * @return Cookie's version.
   */
  int version();

  /**
   * @return True if HTTP Only.
   */
  boolean httpOnly();

}
