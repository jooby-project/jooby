/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import io.jooby.SneakyThrows;

import javax.persistence.EntityManager;

/**
 * Allows you to open a JPA session on demand by acquiring an instance
 * of a class implementing this interface via the service registry or DI.
 * <p>
 * Usage:
 *
 * <pre>{@code
 * {
 *   get("/pets", ctx -> require(EntityManagerHandler.class)
 *       .apply(em -> em.createQuery("from Pet", Pet.class).getResultList()));
 * }
 * }</pre>
 *
 * Automatically manages the lifecycle of the {@link EntityManager} instance
 * and transactions. After the code block passed to {@code apply} or {@code accept}
 * returns the transaction is being committed and the {@link EntityManager} closed.
 * <p>
 * If the code block throws an exception, the transaction is rolled back and the
 * {@link EntityManager} is released as well.
 * <p>
 * You may access a {@link TransactionHandler} instance to be able to work with
 * multiple transactions:
 *
 * <pre>{@code
 * {
 *   get("/update", ctx -> require(EntityManagerHandler.class)
 *       .apply((em, txh) -> {
 *         em.createQuery("from Pet", Pet.class).getResultList().forEach(pet -> {
 *           pet.setName(pet.getName() + " Updated");
 *           txh.commit(); // update each entity in a separate transaction
 *         });
 *         return "ok";
 *       }));
 * }
 * }</pre>
 *
 * A call to {@link TransactionHandler#commit()} commits the current transaction
 * and automatically begins a new one. Similarly you can issue a rollback using
 * {@link TransactionHandler#rollback()}.
 * <p>
 * {@link EntityManagerHandler} does NOT allow nesting:
 *
 * <pre>{@code
 * {
 *   get("/nope", ctx -> require(EntityManagerHandler.class)
 *       .apply(em -> {
 *
 *         // will lead to exception
 *         require(EntityManagerHandler.class).accept(...);
 *
 *         return "ok";
 *       }));
 * }
 * }</pre>
 *
 * Also don't use it together with {@link SessionRequest} or {@link TransactionalRequest}:
 *
 * <pre>{@code
 * {
 *   decorator(new TransactionalRequest());
 *
 *   // will lead to exception
 *   get("/nope", ctx -> require(EntityManagerHandler.class)
 *       .apply(em -> em.createQuery("from Pet", Pet.class).getResultList()));
 * }
 * }</pre>
 */
public interface EntityManagerHandler {

  /**
   * Allows committing or rolling back the current transaction, immediately
   * beginning a new one.
   */
  interface TransactionHandler {

    /**
     * Commits the current transaction and begins a new one.
     */
    void commit();

    /**
     * Rolls the current transaction back and begins a new one.
     */
    void rollback();
  }

  /**
   * Runs the specified code block passing a reference to an {@link EntityManager} to it.
   *
   * @param callback the block to execute
   */
  default void accept(SneakyThrows.Consumer<EntityManager> callback) {
    apply(((entityManager) -> {
      callback.accept(entityManager);
      return null;
    }));
  }

  /**
   * Runs the specified code block passing a reference to an {@link EntityManager} and
   * a {@link TransactionHandler} to it.
   *
   * @param callback the block to execute
   */
  default void accept(SneakyThrows.Consumer2<EntityManager, TransactionHandler> callback) {
    apply(((entityManager, transactionHandler) -> {
      callback.accept(entityManager, transactionHandler);
      return null;
    }));
  }

  /**
   * Runs the specified code block passing a reference to an {@link EntityManager} to it,
   * and returns it's result.
   *
   * @param <T> type of return value
   * @param callback the block to execute
   * @return the result of the callback
   */
  default <T> T apply(SneakyThrows.Function<EntityManager, T> callback) {
    return apply(((entityManager, transactionHandler) -> callback.apply(entityManager)));
  }

  /**
   * Runs the specified code block passing a reference to an {@link EntityManager} and
   * a {@link TransactionHandler} to it, and returns it's result.
   *
   * @param <T> type of return value
   * @param callback the block to execute
   * @return the result of the callback
   */
  <T> T apply(SneakyThrows.Function2<EntityManager, TransactionHandler, T> callback);
}
