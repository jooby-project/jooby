package org.jooby.hbm.data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;

public class Car {

  @NotEmpty(message = "may not be empty")
  private String manufacturer;

  @NotEmpty(message = "may not be empty")
  @Size(min = 2, max = 14)
  private String licensePlate;

  @Min(value = 2, message = "must be greater than or equal to 2")
  private int seatCount;

  public Car(final String manufacturer, final String licencePlate, final int seatCount) {
    this.manufacturer = manufacturer;
    this.licensePlate = licencePlate;
    this.seatCount = seatCount;
  }

  public String getManufacturer() {
    return manufacturer;
  }

  public void setManufacturer(final String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public String getLicensePlate() {
    return licensePlate;
  }

  public void setLicensePlate(final String licensePlate) {
    this.licensePlate = licensePlate;
  }

  public int getSeatCount() {
    return seatCount;
  }

  public void setSeatCount(final int seatCount) {
    this.seatCount = seatCount;
  }

  @Override
  public String toString() {
    return manufacturer;
  }

}
