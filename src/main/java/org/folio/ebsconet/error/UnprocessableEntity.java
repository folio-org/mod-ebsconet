package org.folio.ebsconet.error;

public class UnprocessableEntity extends RuntimeException {
  public UnprocessableEntity(String message) {
    super(message);
  }
}
