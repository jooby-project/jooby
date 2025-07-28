/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import jakarta.validation.constraints.NotEmpty;

/**
 * Record documentation.
 *
 * @param id Person id.
 * @param name Person name. Example: edgar.
 */
public record RecordBeanDoc(String id, @NotEmpty String name) {}
