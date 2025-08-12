/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.museum.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request payload for creating new special events at the museum. */
public class CreateSpecialEventRequest {

  /** Name of the special event. `Pirate Coding Workshop` */
  private String name;

  /** Location where the special event is held. `Computer Room` */
  private String location;

  /** Description of the special event. */
  private String eventDescription;

  /** List of planned dates for the special event. */
  private List<LocalDate> dates;

  /** Price of a ticket for the special event. `25` */
  private BigDecimal price;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getEventDescription() {
    return eventDescription;
  }

  public void setEventDescription(String eventDescription) {
    this.eventDescription = eventDescription;
  }

  public List<LocalDate> getDates() {
    return dates;
  }

  public void setDates(List<LocalDate> dates) {
    this.dates = dates;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }
}
