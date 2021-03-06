package com.faforever.client.game;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.util.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.annotation.Autowired;


public class PopupPlayerCardController {
  @FXML
  Label playerInfo;

  @FXML
  ImageView playerFlag;

  @Autowired
  CountryFlagService countryFlagService;

  @Autowired
  I18n i18n;

  public void setPlayer(PlayerInfoBean playerInfoBean){
    playerFlag.setImage(countryFlagService.loadCountryFlag(playerInfoBean.getCountry()));

    String playerInfoLocalized = i18n.get("playerInfoTooltipFormat", playerInfoBean.getUsername(), RatingUtil.getRating(playerInfoBean));
    playerInfo.setText(playerInfoLocalized);
  }

  public Node getRoot(){
    return playerInfo;
  }
}
