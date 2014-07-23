package jooby;

/**
 * A HTTP Cookie.
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Cookie {

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
