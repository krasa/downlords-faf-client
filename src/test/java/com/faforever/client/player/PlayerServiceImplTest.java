package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnFoeListListener;
import com.faforever.client.legacy.OnFriendListListener;
import com.faforever.client.legacy.OnPlayerInfoListener;
import com.faforever.client.user.UserService;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PlayerServiceImplTest {

  private PlayerServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new PlayerServiceImpl();
    instance.lobbyServerAccessor = mock(LobbyServerAccessor.class);
    instance.userService = mock(UserService.class);
  }

  @Test
  public void testInit() throws Exception {
    instance.init();

    verify(instance.lobbyServerAccessor).setOnPlayerInfoMessageListener(any(OnPlayerInfoListener.class));
    verify(instance.lobbyServerAccessor).setOnFoeListListener(any(OnFoeListListener.class));
    verify(instance.lobbyServerAccessor).setOnFriendListListener(any(OnFriendListListener.class));
  }

  @Test
  public void testGetPlayerForUsernameUsernameDoesNotExist() throws Exception {
    PlayerInfoBean playerInfoBean = instance.getPlayerForUsername("junit");
    assertNull(playerInfoBean);
  }

  @Test
  public void testGetPlayerForUsernameUsernameExists() throws Exception {
    instance.registerAndGetPlayerForUsername("junit");

    PlayerInfoBean playerInfoBean = instance.getPlayerForUsername("junit");

    assertNotNull(playerInfoBean);
    assertEquals("junit", playerInfoBean.getUsername());
  }

  @Test
  public void testGetPlayerForUsernameNull() throws Exception {
    PlayerInfoBean playerInfoBean = instance.getPlayerForUsername(null);
    assertNull(playerInfoBean);
  }

  @Test
  public void testRegisterAndGetPlayerForUsernameDoesNotExist() throws Exception {
    PlayerInfoBean playerInfoBean = instance.registerAndGetPlayerForUsername("junit");

    assertNotNull(playerInfoBean);
    assertEquals("junit", playerInfoBean.getUsername());
  }

  @Test(expected = NullPointerException.class)
  public void testRegisterAndGetPlayerForUsernameNull() throws Exception {
    instance.registerAndGetPlayerForUsername(null);
  }

  @Test
  public void testGetPlayerNamesEmpty() throws Exception {
    Set<String> playerNames = instance.getPlayerNames();
    assertThat(playerNames, empty());
  }

  @Test
  public void testGetPlayerNamesSomeInstances() throws Exception {
    instance.registerAndGetPlayerForUsername("player1");
    instance.registerAndGetPlayerForUsername("player2");

    Set<String> playerNames = instance.getPlayerNames();

    assertThat(playerNames, hasSize(2));
    assertThat(playerNames, containsInAnyOrder("player1", "player2"));
  }

  @Test
  public void testAddFriend() throws Exception {
    PlayerInfoBean lisa = instance.registerAndGetPlayerForUsername("lisa");
    PlayerInfoBean ashley = instance.registerAndGetPlayerForUsername("ashley");

    instance.addFriend("lisa");
    instance.addFriend("ashley");

    verify(instance.lobbyServerAccessor, times(2)).setFriends(eq(Arrays.asList("lisa", "ashley")));
    assertTrue("Property 'friend' was not set to true", lisa.getFriend());
    assertTrue("Property 'friend' was not set to true", ashley.getFriend());
  }

  @Test
  public void testAddFriendIsFoe() throws Exception {
    PlayerInfoBean playerInfoBean = instance.registerAndGetPlayerForUsername("player");
    playerInfoBean.setFoe(true);

    instance.addFriend("player");

    assertFalse("Property 'foe' is still true", playerInfoBean.getFoe());
  }

  @Test
  public void testRemoveFriend() throws Exception {
    PlayerInfoBean player1 = instance.registerAndGetPlayerForUsername("player1");
    PlayerInfoBean player2 = instance.registerAndGetPlayerForUsername("player2");

    // TODO check if this mockito usage is correct
    instance.addFriend("player1");
    verify(instance.lobbyServerAccessor).setFriends(eq(Arrays.asList("player1")));

    instance.addFriend("player2");
    verify(instance.lobbyServerAccessor, times(2)).setFriends(eq(Arrays.asList("player1", "player2")));

    instance.removeFriend("player1");
    verify(instance.lobbyServerAccessor, times(3)).setFriends(eq(Arrays.asList("player2")));

    assertFalse("Property 'friend' was not set to false", player1.getFriend());
    assertTrue("Property 'friend' was not set to true", player2.getFriend());
  }

  @Test
  public void testAddFoe() throws Exception {
    PlayerInfoBean player1 = instance.registerAndGetPlayerForUsername("player1");
    PlayerInfoBean player2 = instance.registerAndGetPlayerForUsername("player2");

    instance.addFoe("player1");
    instance.addFoe("player2");

    verify(instance.lobbyServerAccessor, times(2)).setFoes(Arrays.asList("player1", "player2"));
    assertTrue("Property 'foe' was not set to true", player1.getFoe());
    assertTrue("Property 'foe' was not set to true", player2.getFoe());
  }

  @Test
  public void testAddFoeIsFriend() throws Exception {
    PlayerInfoBean playerInfoBean = instance.registerAndGetPlayerForUsername("player");
    playerInfoBean.setFriend(true);

    instance.addFoe("player");

    assertFalse("Property 'friend' is still true", playerInfoBean.getFriend());
  }

  @Test
  public void testRemoveFoe() throws Exception {
    PlayerInfoBean player = instance.registerAndGetPlayerForUsername("player");

    instance.addFriend("player");
    instance.removeFriend("player");

    assertFalse("Property 'friend' was not set to false", player.getFriend());
  }

  @Test
  public void testOnPlayerInfo() throws Exception {

  }

  @Test
  public void testOnFoeList() throws Exception {

  }

  @Test
  public void testOnFriendList() throws Exception {

  }

  @Test
  public void testGetCurrentPlayer() throws Exception {

  }
}
