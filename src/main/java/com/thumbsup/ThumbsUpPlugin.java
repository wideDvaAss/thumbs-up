package com.thumbsup;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
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
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() != VarbitID.CSAT_TYPE)
		{
			return;
		}

		clientThread.invokeLater(this::replaceVoteUI);
	}

	private void replaceVoteUI()
	{
		if (client.getVarbitValue(VarbitID.CSAT_TYPE) != 1)
		{
			client.setVarbit(VarbitID.CSAT_TYPE, 1);
			updateLogoutLayout();
		}
	}

	private void restoreVoteUI()
	{
		client.setVarbit(VarbitID.CSAT_TYPE, client.getServerVarbitValue(VarbitID.CSAT_TYPE));
		updateLogoutLayout();
	}

	private void updateLogoutLayout()
	{
		client.runScript(
			SCRIPT_LOGOUT_LAYOUT_UPDATE,
			InterfaceID.Logout.LOGOUT_BUTTONS,
			InterfaceID.Logout.SATISFACTION,
			InterfaceID.Logout.SATISFACTION_TEXT,
			InterfaceID.Logout.SATISFACTION_BINARY,
			InterfaceID.Logout.SATISFACTION_5SCALE
		);
	}
}
