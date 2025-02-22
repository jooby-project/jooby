/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3508.data;

import jakarta.validation.Valid;

@Valid @AvajePasswordsShouldMatch
public class AvajeNewAccountRequest extends NewAccountRequest {}
