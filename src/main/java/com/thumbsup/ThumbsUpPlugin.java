package com.thumbsup;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;

@Slf4j
@PluginDescriptor(
		name = "Thumbs Up"
)
public class ThumbsUpPlugin extends Plugin
{
	private static final int SCRIPT_LOGOUT_LAYOUT_UPDATE = 2243;
	private static final String TEXT_BINARY = "Did you enjoy playing<br>Old School RuneScape today?";
	private static final String TEXT_5SCALE = "How much did you enjoy playing<br>Old School RuneScape today?";


	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Override
	protected void startUp() throws Exception
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::replaceVoteUI);
		}

	}

	@Override
	protected void shutDown() throws Exception
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::restoreVoteUI);
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event) {
		if (event.getScriptId() == SCRIPT_LOGOUT_LAYOUT_UPDATE) {
			replaceVoteUI();
		}
	}

	private void cycleVoteUI(int shouldHideId, int shouldShowId, String text)
	{
		if (client.getWidget(InterfaceID.Logout.LOGOUT) == null) return;

		Widget satisfactionText = client.getWidget(InterfaceID.Logout.SATISFACTION_TEXT);
		if (satisfactionText == null) return;
		satisfactionText.setText(text);

		Widget shouldHide = client.getWidget(shouldHideId);
		Widget shouldShow = client.getWidget(shouldShowId);
		if (shouldHide == null || shouldShow == null) return;
		shouldHide.setHidden(true);
		shouldShow.setHidden(false);
	}

	private void replaceVoteUI()
	{
		cycleVoteUI(InterfaceID.Logout.SATISFACTION_5SCALE, InterfaceID.Logout.SATISFACTION_BINARY, TEXT_BINARY);
	}

	private void restoreVoteUI()
	{
		cycleVoteUI(InterfaceID.Logout.SATISFACTION_BINARY, InterfaceID.Logout.SATISFACTION_5SCALE, TEXT_5SCALE);
	}
}
