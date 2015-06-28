package com.faforever.client.legacy.domain;

import java.util.Collection;

public class FoesMessage extends ClientMessage {

  private final Collection<String> foes;

  public FoesMessage(Collection<String> foes) {
    this.command = "social";
    this.foes = foes;
  }
}
