package com.faforever.client.replay;

import com.faforever.client.game.GameService;
import com.faforever.client.game.OnGameStartedListener;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import com.google.common.primitives.Bytes;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class ReplayServerImpl implements ReplayServer, OnGameInfoListener, OnGameStartedListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int REPLAY_BUFFER_SIZE = 0x200;

  /**
   * This is a prefix used in the FA live replay protocol that needs to be stripped away when storing to a file.
   */
  private static final byte[] LIVE_REPLAY_PREFIX = new byte[]{'P', '/'};

  @Autowired
  Environment environment;

  @Autowired
  NotificationService notificationService;

  @Autowired
  I18n i18n;

  @Autowired
  GameService gameService;

  @Autowired
  UserService userService;

  @Autowired
  ReplayFileWriter replayFileWriter;

  private ReplayInfo replayInfo;


  @PostConstruct
  void postConstruct() {
    gameService.addOnGameInfoListener(this);
    gameService.addOnGameStartedListener(this);
    startInBackground();
  }

  void startInBackground() {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        Integer localReplayServerPort = environment.getProperty("localReplayServer.port", Integer.class);
        String fafReplayServerHost = environment.getProperty("fafReplayServer.host");
        Integer fafReplayServerPort = environment.getProperty("fafReplayServer.port", Integer.class);

        logger.debug("Opening local replay server on port {}", localReplayServerPort);

        try (ServerSocket serverSocket = new ServerSocket(localReplayServerPort);
             Socket fafReplayServerSocket = new Socket(fafReplayServerHost, fafReplayServerPort)) {
          while (!serverSocket.isClosed() && !fafReplayServerSocket.isClosed()) {
            recordAndRelay(serverSocket, new BufferedOutputStream(fafReplayServerSocket.getOutputStream()));
          }
        } catch (IOException e) {
          logger.warn("Error while recording replay", e);
          notificationService.addNotification(new PersistentNotification(
                  i18n.get("replayServer.listeningFailed", localReplayServerPort),
                  Severity.WARN,
                  Collections.singletonList(new Action(i18n.get("replayServer.retry"), event -> startInBackground()))
              )
          );
        }
        return null;
      }
    });
  }

  private void recordAndRelay(ServerSocket serverSocket, OutputStream fafRelayOutputStream) throws IOException {
    Socket socket = serverSocket.accept();
    logger.debug("Accepted connection from {}", socket.getRemoteSocketAddress());

    ByteArrayOutputStream replayData = new ByteArrayOutputStream();

    byte[] buffer = new byte[REPLAY_BUFFER_SIZE];
    try (InputStream inputStream = socket.getInputStream()) {
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        if (replayData.size() == 0 && Bytes.indexOf(buffer, LIVE_REPLAY_PREFIX) != -1) {
          int dataBeginIndex = Bytes.indexOf(buffer, (byte) 0x0000) + 1;
          replayData.write(buffer, dataBeginIndex, bytesRead - dataBeginIndex);
        } else {
          replayData.write(buffer, 0, bytesRead);
        }

        fafRelayOutputStream.write(buffer, 0, bytesRead);
      }
    } finally {
      fafRelayOutputStream.flush();
    }

    logger.debug("FAF has disconnected, writing replay data to file");
    finishReplayInfo();
    replayFileWriter.writeReplayDataToFile(replayData, replayInfo);
  }

  @Override
  public void onGameInfo(GameInfo gameInfo) {
    // As the game information can still change while the players are in the in-game lobby, we need to listen for any
    // changes and remember them in order to store the game information along with the replay file.
    if (replayInfo == null || !Objects.equals(gameInfo.uid, replayInfo.uid)) {
      return;
    }

    replayInfo.updateFromGameInfo(gameInfo);
  }

  @Override
  public void onGameStarted(int uid) {
    replayInfo = new ReplayInfo();
    replayInfo.uid = uid;
    replayInfo.gameTime = pythonTime();
    replayInfo.versionInfo = new HashMap<>();
    replayInfo.versionInfo.put("lobby", String.format("dfaf-%s", getClass().getPackage().getImplementationVersion()));
  }

  private void finishReplayInfo() {
    replayInfo.gameEnd = pythonTime();
    replayInfo.recorder = userService.getUsername();
    replayInfo.complete = true;
    replayInfo.state = GameState.CLOSED;
  }

  /**
   * Returns the current millis the same way as python does since this is what's stored in the replay files *yay*.
   */
  private static double pythonTime() {
    return System.currentTimeMillis() / 1000;
  }
}