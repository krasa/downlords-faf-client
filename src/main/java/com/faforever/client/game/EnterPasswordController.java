package com.faforever.client.game;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;

public class EnterPasswordController {

  public interface OnPasswordEnteredListener {

    void onPasswordEntered(String password);
  }

  private OnPasswordEnteredListener listener;

  @FXML
  Node enterPasswordRoot;

  @FXML
  TextField passwordTextField;

  public Node getRoot() {
    return enterPasswordRoot;
  }

  public void setOnPasswordEnteredListener(OnPasswordEnteredListener listener) {
    if (this.listener != null) {
      throw new IllegalStateException("Listener has already been set");
    }
    this.listener = listener;
  }

  @FXML
  void onPasswordEntered(ActionEvent event) {
    if (listener == null) {
      throw new IllegalStateException("No listener has been set");
    }
    listener.onPasswordEntered(passwordTextField.getText());
  }
}