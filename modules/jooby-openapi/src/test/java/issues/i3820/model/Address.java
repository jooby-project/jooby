/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820.model;

/**
 * A reusable way to store address details (Street, City, Zip). We can reuse this on Authors,
 * Publishers, or Users.
 */
public class Address {
  /**
   * The specific street address.
   *
   * <p>Includes the house number, street name, and apartment number if applicable. Example: "123
   * Maple Avenue, Apt 4B".
   */
  public String street;

  /**
   * The town, city, or municipality.
   *
   * <p>Used for grouping authors by location or calculating shipping regions.
   */
  public String city;

  /**
   * The postal or zip code.
   *
   * <p>Stored as text (String) rather than a number to support codes that start with zero (e.g.,
   * "02138") or contain letters (e.g., "K1A 0B1").
   */
  public String zip;
}
