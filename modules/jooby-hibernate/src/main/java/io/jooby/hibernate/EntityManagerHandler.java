package io.jooby.hibernate;

import io.jooby.SneakyThrows;

import javax.persistence.EntityManager;

public interface EntityManagerHandler {

  interface TransactionHandler {

    void commit();

    void rollback();
  }

  default void accept(SneakyThrows.Consumer<EntityManager> callback) {
    apply(((entityManager) -> {
      callback.accept(entityManager);
      return null;
    }));
  }

  default void accept(SneakyThrows.Consumer2<EntityManager, TransactionHandler> callback) {
    apply(((entityManager, transactionHandler) -> {
      callback.accept(entityManager, transactionHandler);
      return null;
    }));
  }

  default <T> T apply(SneakyThrows.Function<EntityManager, T> callback) {
    return apply(((entityManager, transactionHandler) -> callback.apply(entityManager)));
  }

  <T> T apply(SneakyThrows.Function2<EntityManager, TransactionHandler, T> callback);
}
