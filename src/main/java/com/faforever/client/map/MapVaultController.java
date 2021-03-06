package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.game.MapSize;
import com.faforever.client.util.Callback;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class MapVaultController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  Pane mapVaultRoot;

  @FXML
  TableView<MapInfoBean> mapTableView;

  @FXML
  TableColumn<MapInfoBean, String> nameColumn;

  @FXML
  TableColumn<MapInfoBean, String> descriptionColumn;

  @FXML
  TableColumn<MapInfoBean, Number> playsColumn;

  @FXML
  TableColumn<MapInfoBean, MapSize> sizeColumn;

  @FXML
  TableColumn<MapInfoBean, String> creatorColumn;

  @FXML
  TableColumn<MapInfoBean, Number> ratingColumn;

  @FXML
  TableColumn<MapInfoBean, Number> downloadsColumn;

  @FXML
  TableColumn<MapInfoBean, Number> playersColumn;

  @FXML
  TableColumn<MapInfoBean, Number> versionColumn;

  @Autowired
  MapService mapService;

  public Node getRoot() {
    return mapVaultRoot;
  }

  @FXML
  void initialize() {
    nameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
    descriptionColumn.setCellValueFactory(param -> param.getValue().descriptionProperty());
    playsColumn.setCellValueFactory(param -> param.getValue().playsProperty());
    // creatorColumn.setCellValueFactory(param -> param.getValue().creatorProperty());
    ratingColumn.setCellValueFactory(param -> param.getValue().ratingProperty());
    downloadsColumn.setCellValueFactory(param -> param.getValue().downloadsProperty());
    sizeColumn.setCellValueFactory(param -> param.getValue().sizeProperty());
    playersColumn.setCellValueFactory(param -> param.getValue().playersProperty());
    versionColumn.setCellValueFactory(param -> param.getValue().versionProperty());
  }

  public void setUpIfNecessary() {
    // FIXME test code so far
    mapService.readMapVaultInBackground(0, 100, new Callback<List<MapInfoBean>>() {
      @Override
      public void success(List<MapInfoBean> result) {
        mapTableView.setItems(FXCollections.observableList(result));
      }

      @Override
      public void error(Throwable e) {
        logger.warn("Failed", e);
      }
    });
  }
}
