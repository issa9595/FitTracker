package com.fittracker.common.error;

public class NotFoundException extends RuntimeException {
  private final String resource;
  private final Object identifier;

  public NotFoundException(String resource, Object identifier) {
    super("%s '%s' introuvable".formatted(resource, identifier));
    this.resource = resource;
    this.identifier = identifier;
  }

  public String getResource() {
    return resource;
  }

  public Object getIdentifier() {
    return identifier;
  }
}
