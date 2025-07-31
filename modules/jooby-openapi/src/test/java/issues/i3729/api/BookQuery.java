/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

/**
 * Query books by complex filters.
 *
 * @param title Book's title. Optional.
 * @param author Book's author. Optional.
 * @param isbn Book's isbn. Optional.
 */
public record BookQuery(String title, String author, String isbn) {}
