/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.museum;

import java.time.LocalDate;
import java.util.List;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;
import issues.i3729.museum.model.MuseumDailyHours;

@Path("/museum-hours")
public class Museum {

  /**
   * Get museum hours
   *
   * <p>Get upcoming museum operating hours
   *
   * @param startDate The starting date to retrieve future operating hours from. Defaults to today's
   *     date. `2023-02-23`
   * @param page The page number to retrieve.
   * @param limit The number of days per page.
   * @return List of museum operating hours for consecutive days. `{ default: { summary: Museum
   *     opening hours, value: [ {date: 2023-09-11, timeOpen: 09:00, timeClose: 18:00}, {date:
   *     2023-09-12, timeOpen: 09:00, timeClose: 18:00}, {date: 2023-09-13, timeOpen: 09:00,
   *     timeClose: 18:00}, {date: 2023-09-17, timeOpen: 09:00, timeClose: 18:00} ] }, closed: {
   *     summary: The museum is closed, value: [] } }`
   * @throws IllegalArgumentException <code>400</code>
   * @throws java.util.NoSuchElementException <code>404</code>
   * @x-badges [{name: Beta, position: before, color: purple}]
   * @tag Operations
   */
  @GET
  public List<MuseumDailyHours> getMuseumHours(
      @QueryParam LocalDate startDate, @QueryParam Integer page, @QueryParam Integer limit) {
    return List.of();
  }
}
