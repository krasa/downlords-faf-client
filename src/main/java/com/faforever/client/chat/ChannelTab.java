package com.faforever.client.chat;

import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.JavaFxUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelTab extends AbstractChatTab {

  @FXML
  WebView messagesWebView;

  @FXML
  Pane moderatorsPane;

  @FXML
  Pane friendsPane;

  @FXML
  Pane foesPane;

  @FXML
  Pane othersPane;

  @FXML
  Pane chatOnlyPane;

  @FXML
  TextField userSearchTextField;

  @FXML
  TextField messageTextField;

  @Autowired
  ChatController chatController;

  @Autowired
  ApplicationContext applicationContext;

  private final String channelName;

  /**
   * Keeps track of which ChatUserControl in which pane belongs to which user.
   */
  private final Map<String, Map<Pane, ChatUserControl>> userToChatUserControls;

  public ChannelTab(String channelName) {
    super(channelName, "channel_tab.fxml");
    this.channelName = channelName;

    userToChatUserControls = FXCollections.observableMap(new ConcurrentHashMap<>());

    setId(channelName);
    setText(channelName);
  }

  @Override
  void postConstruct() {
    super.postConstruct();

    chatService.addChannelUserListListener(channelName, change -> {
      if (change.wasAdded()) {
        onUserJoinedChannel(change.getValueAdded());
      } else if (change.wasRemoved()) {
        onUserLeft(change.getValueRemoved().getUsername());
      }
    });

    // Maybe there were already elements; fetch them
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        ObservableMap<String, ChatUser> chatUsersForChannel = chatService.getChatUsersForChannel(channelName);
        synchronized (chatUsersForChannel) {
          chatUsersForChannel.values().forEach(ChannelTab.this::onUserJoinedChannel);
        }
        return null;
      }
    });
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }

  @Override
  protected TextInputControl getMessageTextField() {
    return messageTextField;
  }

  @FXML
  void initialize() {
    userSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      filterChatUserControlsBySearchString();
    });

    setOnCloseRequest(event -> chatService.leaveChannel(channelName));
  }

  private void onUserJoinedChannel(ChatUser chatUser) {
    JavaFxUtil.assertBackgroundThread();

    String username = chatUser.getUsername();
    PlayerInfoBean playerInfoBean = playerService.registerAndGetPlayerForUsername(username);

    playerInfoBean.moderatorInChannelsProperty().bind(chatUser.moderatorInChannelsProperty());
    playerInfoBean.usernameProperty().bind(chatUser.usernameProperty());
    playerInfoBean.usernameProperty().addListener((observable, oldValue, newValue) -> {
      for (Map.Entry<Pane, ChatUserControl> entry : userToChatUserControls.get(oldValue).entrySet()) {
        Pane pane = entry.getKey();
        ChatUserControl chatUserControl = entry.getValue();

        pane.getChildren().remove(chatUserControl);
        addChatUserControlSorted(pane, chatUserControl);
      }
    });

    playerInfoBean.friendProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        addToPane(playerInfoBean, friendsPane);
        removeFromPane(playerInfoBean, foesPane);
        removeFromPane(playerInfoBean, othersPane);
      } else {
        removeFromPane(playerInfoBean, friendsPane);
        if (!playerInfoBean.isFoe()) {
          addToPane(playerInfoBean, othersPane);
        }
      }
    });
    playerInfoBean.foeProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        addToPane(playerInfoBean, foesPane);
        removeFromPane(playerInfoBean, friendsPane);
        removeFromPane(playerInfoBean, othersPane);
      } else {
        removeFromPane(playerInfoBean, foesPane);
        if (!playerInfoBean.isFriend()) {
          addToPane(playerInfoBean, othersPane);
        }
      }
    });
    playerInfoBean.chatOnlyProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        addToPane(playerInfoBean, chatOnlyPane);
        removeFromPane(playerInfoBean, othersPane);
      } else {
        removeFromPane(playerInfoBean, chatOnlyPane);
        if (!playerInfoBean.isFoe() && !playerInfoBean.isFriend() && !playerInfoBean.getModeratorInChannels().contains(channelName)) {
          addToPane(playerInfoBean, othersPane);
        }
      }
    });
    playerInfoBean.getModeratorInChannels().addListener((SetChangeListener<String>) change -> {
      if (change.wasAdded()) {
        addToPane(playerInfoBean, moderatorsPane);
        removeFromPane(playerInfoBean, othersPane);
      } else {
        removeFromPane(playerInfoBean, moderatorsPane);
        if (!playerInfoBean.isFoe() && !playerInfoBean.isFriend()) {
          addToPane(playerInfoBean, othersPane);
        }
      }
    });


    Collection<Pane> targetPanesForUser = getTargetPanesForUser(playerInfoBean);
    userToChatUserControls.putIfAbsent(username, new HashMap<>(targetPanesForUser.size(), 1));

    for (Pane pane : targetPanesForUser) {
      // Remove Plateform.runLater() as soon as RT-40417 is fixed
      Platform.runLater(() -> createChatUserControlForPlayerIfNecessary(pane, playerInfoBean));
    }
  }

  private void onUserLeft(String username) {
    JavaFxUtil.assertBackgroundThread();

    Map<Pane, ChatUserControl> paneToChatUserControlMap = userToChatUserControls.get(username);
    if (paneToChatUserControlMap == null) {
      return;
    }

    for (Map.Entry<Pane, ChatUserControl> entry : paneToChatUserControlMap.entrySet()) {
      Platform.runLater(() -> entry.getKey().getChildren().remove(entry.getValue()));
    }
    paneToChatUserControlMap.clear();
    userToChatUserControls.remove(username);
  }

  private void addToPane(PlayerInfoBean playerInfoBean, Pane pane) {
    createChatUserControlForPlayerIfNecessary(pane, playerInfoBean);
  }

  private void removeFromPane(PlayerInfoBean playerInfoBean, Pane pane) {
    // Re-add Plateform.runLater() as soon as RT-40417 is fixed
//        Platform.runLater(() -> {
    Map<Pane, ChatUserControl> paneChatUserControlMap = userToChatUserControls.get(playerInfoBean.getUsername());
    if (paneChatUserControlMap == null) {
      // User has not yet been added to this pane; no need to remove him
      return;
    }
    Platform.runLater(() -> {
      ChatUserControl chatUserControl = paneChatUserControlMap.remove(pane);
      pane.getChildren().remove(chatUserControl);
    });
//        });
  }

  /**
   * Creates a {@link com.faforever.client.chat.ChatUserControl} for the given playerInfoBean and adds it to the given
   * pane if there isn't already such a control in this pane. After the control has been added, the user search filter
   * is applied.
   */
  private void createChatUserControlForPlayerIfNecessary(Pane pane, PlayerInfoBean playerInfoBean) {
    String username = playerInfoBean.getUsername();
    if (!userToChatUserControls.containsKey(username)) {
      userToChatUserControls.put(username, new HashMap<>(1, 1));
    }

    Map<Pane, ChatUserControl> paneToChatUserControlMap = userToChatUserControls.get(username);

    ChatUserControl existingChatUserControl = paneToChatUserControlMap.get(pane);
    if (existingChatUserControl != null) {
      return;
    }

    ChatUserControl chatUserControl = applicationContext.getBean(ChatUserControl.class);
    chatUserControl.setPlayerInfoBean(playerInfoBean);
    paneToChatUserControlMap.put(pane, chatUserControl);

    Platform.runLater(() -> {
      addChatUserControlSorted(pane, chatUserControl);
      applyUserSearchFilter(chatUserControl);
    });
  }

  private void applyUserSearchFilter(ChatUserControl chatUserControl) {
    String lowerCaseSearchString = chatUserControl.getPlayerInfoBean().getUsername().toLowerCase();
    boolean display = lowerCaseSearchString.contains(userSearchTextField.getText().toLowerCase());
    chatUserControl.setVisible(display);
    chatUserControl.setManaged(display);
  }

  /**
   * Hides all chat user controls whose username does not contain the string entered in the {@link
   * #userSearchTextField}.
   */
  private void filterChatUserControlsBySearchString() {
    synchronized (userToChatUserControls) {
      for (Map<Pane, ChatUserControl> chatUserControlMap : userToChatUserControls.values()) {
        for (Map.Entry<Pane, ChatUserControl> chatUserControlEntry : chatUserControlMap.entrySet()) {
          ChatUserControl chatUserControl = chatUserControlEntry.getValue();

          applyUserSearchFilter(chatUserControl);
        }
      }
    }
  }

  /**
   * Inserts the given ChatUserControl into the given Pane such that it is correctly sorted alphabetically.
   */
  private void addChatUserControlSorted(Pane pane, ChatUserControl chatUserControl) {
    ObservableList<Node> children = pane.getChildren();
    for (Node child : children) {
      if (!(child instanceof ChatUserControl)) {
        continue;
      }

      String username1 = chatUserControl.getPlayerInfoBean().getUsername();
      String username2 = ((ChatUserControl) child).getPlayerInfoBean().getUsername();

      if (username1.compareTo(username2) < 0) {
        children.add(children.indexOf(child), chatUserControl);
        return;
      }
    }

    children.add(chatUserControl);
  }

  private Collection<Pane> getTargetPanesForUser(PlayerInfoBean playerInfoBean) {
    ArrayList<Pane> panes = new ArrayList<>(3);

    if (playerInfoBean.isFriend()) {
      panes.add(friendsPane);
    } else if (playerInfoBean.isFoe()) {
      panes.add(foesPane);
    }

    if (playerInfoBean.getModeratorInChannels().contains(channelName)) {
      panes.add(moderatorsPane);
    }

    if (playerInfoBean.isChatOnly()) {
      panes.add(chatOnlyPane);
    }

    if (panes.isEmpty()) {
      panes.add(othersPane);
    }

    return panes;
  }
}
