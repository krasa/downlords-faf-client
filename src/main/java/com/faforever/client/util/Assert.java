package com.faforever.client.util;

public final class Assert {

  private Assert() {
    throw new AssertionError("Not instantiatable");
  }

  public static void checkNull(Object object, String message) {
    if (object == null) {
      throw new NullPointerException(message);
    }
  }
}
