package jooby;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Session {

  class Definition {

    private Store store;

    private Integer timeout;

    private Boolean preserveOnStop;

    private Cookie.Definition cookie;

    private Integer saveInterval;

    public Definition(final Store store) {
      this.store = requireNonNull(store, "A session store is required.");
      // sesion cookie will be signed
      cookie = new Cookie.Definition().signed(true);
    }

    public Definition timeout(final int timeout) {
      this.timeout = timeout;
      return this;
    }

    public Optional<Integer> timeout() {
      return Optional.ofNullable(timeout);
    }

    public Optional<Boolean> preserveOnStop() {
      return Optional.ofNullable(preserveOnStop);
    }

    public Definition preserveOnStop(final boolean preserveOnStop) {
      this.preserveOnStop = preserveOnStop;
      return this;
    }

    public Optional<Integer> saveInterval() {
      return Optional.ofNullable(saveInterval);
    }

    public Definition saveInterval(final int saveInterval) {
      this.saveInterval = saveInterval;
      return this;
    }

    public Store store() {
      return store;
    }

    public Cookie.Definition cookie() {
      return cookie;
    }
  }

  interface Store {

    enum SaveReason {
      NEW,

      DIRTY,

      TIME,

      PRESERVE_ON_STOP,

      RENEW_ID;
    }

    Store NOOP = new Store() {
      @Override
      public void save(final Session session, final SaveReason reason) {
      }

      @Override
      public Session get(final String id) {
        return null;
      }

      @Override
      public void delete(final String id) {
      }
    };

    Session get(String id) throws Exception;

    void save(Session session, SaveReason reason) throws Exception;

    void delete(String id) throws Exception;

    default String generateID(final long seed) {
      UUID uuid = UUID.randomUUID();
      return uuid.toString();
    }
  }

  /** The logging system. */
  Logger log = LoggerFactory.getLogger(Session.class);

  String id();

  long createdAt();

  long accessedAt();

  long expiryAt();

  <T> T get(final String name, final T defaults);

  default <T> T get(final String name) {
    return get(name, null);
  }

  Map<String, Object> attributes();

  default boolean isSet(final String name) {
    return get(name) != null;
  }

  Session set(final String name, final Object value);

  Session unset(final String name);

  Session unset();

  void destroy();

}
