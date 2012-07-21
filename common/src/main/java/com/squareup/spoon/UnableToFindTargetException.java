package com.squareup.spoon;

/** The {@link ExecutionTarget} was unable to find the specified {@link com.squareup.spoon.model.Device}. */
public class UnableToFindTargetException extends RuntimeException {
  public UnableToFindTargetException(String message) {
    super(message);
  }
}
