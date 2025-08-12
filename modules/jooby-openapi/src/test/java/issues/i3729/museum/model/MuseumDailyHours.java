/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.museum.model;

import java.time.LocalDate;

/**
 * Daily operating hours for the museum.
 *
 * @param date Date the operating hours apply to. `2024-12-31`
 * @param timeOpen Time the museum opens on a specific date. Uses 24-hour time format HH:mm. `09:00`
 * @param timeClose Time the museum closes on a specific date. Uses 24-hour time format HH:mm.
 *     `18:00`
 */
public record MuseumDailyHours(LocalDate date, String timeOpen, String timeClose) {}
