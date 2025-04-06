package com.thumbsup;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptEvent;
import net.runelite.api.SpriteID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.widgets.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;

import java.util.Arrays;
import java.util.List;

import static net.runelite.api.widgets.WidgetUtil.packComponentId;

@Slf4j
@PluginDescriptor(
		name = "Thumbs Up"
)
public class ThumbsUpPlugin extends Plugin
{
	// desired dimensions
	private static final int REVIEW_BUTTON_WIDTH = 67;
	private static final int REVIEW_BUTTON_HEIGHT = 36;
	private static final int REVIEW_BUTTON_1_X = 36;
	private static final int REVIEW_BUTTON_5_X = -36;
	private static final int REVIEW_BUTTON_SPRITE_SIZE = 32;

	// original dimensions
	private static final int ORIG_REVIEW_BUTTON_WIDTH = 32;
	private static final int ORIG_REVIEW_BUTTON_HEIGHT = 36;
	private static final int ORIG_REVIEW_BUTTON_1_X = -74;
	private static final int ORIG_REVIEW_BUTTON_5_X = 74;
	private static final int ORIG_REVIEW_BUTTON_SPRITE_WIDTH = 18;
	private static final int ORIG_REVIEW_BUTTON_SPRITE_HEIGHT = 30;

	private static final int SCRIPT_LOGOUT_LAYOUT_UPDATE = 2243;
	private static final int SCRIPT_REVIEW_BUTTON_CLICKED = 2281;
	private static final int SCRIPT_REVIEW_BUTTONS_SELECTED = 2282;
	private static final int SCRIPT_REVIEW_BUTTONS_UPDATE = 2455;
	private static final int SCRIPT_REVIEW_GRAPHIC_UPDATE = 3440;

	private static final int SPRITE_REVIEW_BUTTON_1 = 6307;
	private static final int SPRITE_REVIEW_BUTTON_1_HOVERED = 6312;
	private static final int SPRITE_REVIEW_BUTTON_5 = 5273;
	private static final int SPRITE_REVIEW_BUTTON_5_HOVERED = 6308;

	private static final int WIDGET_LOGOUT_LAYOUT = packComponentId(InterfaceID.LOGOUT_PANEL, 0);
	private static final int WIDGET_REVIEW_TEXT = packComponentId(InterfaceID.LOGOUT_PANEL, 14);
	private static final int WIDGET_REVIEW_BUTTON_1 = packComponentId(InterfaceID.LOGOUT_PANEL, 19);
	private static final int WIDGET_REVIEW_BUTTON_2 = packComponentId(InterfaceID.LOGOUT_PANEL, 20);
	private static final int WIDGET_REVIEW_BUTTON_3 = packComponentId(InterfaceID.LOGOUT_PANEL, 21);
	private static final int WIDGET_REVIEW_BUTTON_4 = packComponentId(InterfaceID.LOGOUT_PANEL, 22);
	private static final int WIDGET_REVIEW_BUTTON_5 = packComponentId(InterfaceID.LOGOUT_PANEL, 23);

	private static final List<Integer> ALLOWED_SCRIPTS_PRE = Arrays.asList(
			SCRIPT_REVIEW_GRAPHIC_UPDATE,
			SCRIPT_REVIEW_BUTTON_CLICKED
	);
	private static final List<Integer> ALLOWED_SCRIPTS_POST = Arrays.asList(
			SCRIPT_LOGOUT_LAYOUT_UPDATE,
			SCRIPT_REVIEW_GRAPHIC_UPDATE,
			SCRIPT_REVIEW_BUTTON_CLICKED,
			SCRIPT_REVIEW_BUTTONS_SELECTED,
			SCRIPT_REVIEW_BUTTONS_UPDATE
	);

	private volatile int lastChangedSpriteId = -1;
	private volatile int lastClickedReviewButton = -1;

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
	public void onScriptPreFired(ScriptPreFired event)
	{
		int scriptId = event.getScriptId();
		if (!ALLOWED_SCRIPTS_PRE.contains(scriptId)) return;
		ScriptEvent scriptEvent = event.getScriptEvent();
		Object[] args = scriptEvent.getArguments();
		if (args == null) return;
		switch (scriptId)
		{
			case SCRIPT_REVIEW_GRAPHIC_UPDATE:
				if (args.length < 4) return;
				lastChangedSpriteId = (Integer) args[3];
				break;
			case SCRIPT_REVIEW_BUTTON_CLICKED:
				if (args.length < 3) return;
				lastClickedReviewButton = (Integer) args[2];
				break;
			default:
				return;
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		int scriptId = event.getScriptId();

		if (!ALLOWED_SCRIPTS_POST.contains(scriptId)) return;

		switch (scriptId)
		{
			case SCRIPT_LOGOUT_LAYOUT_UPDATE:
				clientThread.invokeLater(this::replaceVoteUI);
				break;
			case SCRIPT_REVIEW_GRAPHIC_UPDATE:
				int spriteId = lastChangedSpriteId;
				if (spriteId == -1) return;
				lastChangedSpriteId = -1;
				replaceSprite(spriteId);
				break;
			case SCRIPT_REVIEW_BUTTON_CLICKED:
				int buttonClicked = lastClickedReviewButton;
				if (buttonClicked == -1) return;
				replaceButtons(true, buttonClicked);
				replaceSprite(SPRITE_REVIEW_BUTTON_1);
				replaceSprite(SPRITE_REVIEW_BUTTON_5);
				break;
			case SCRIPT_REVIEW_BUTTONS_SELECTED:
				int buttonSelected = lastClickedReviewButton;
				if (buttonSelected == -1) return;
				lastClickedReviewButton = -1;
				replaceButtons(true, buttonSelected);
				replaceSprite(SPRITE_REVIEW_BUTTON_1);
				replaceSprite(SPRITE_REVIEW_BUTTON_5);
				break;
			case SCRIPT_REVIEW_BUTTONS_UPDATE:
				replaceSprite(SPRITE_REVIEW_BUTTON_1);
				replaceSprite(SPRITE_REVIEW_BUTTON_5);
				break;
			default:
				return;
		}
	}

	private void replaceSprite(int spriteId)
	{
		int sprite;
		Widget button;
		Widget child;
		switch (spriteId)
		{
			case SPRITE_REVIEW_BUTTON_1:
				sprite = SpriteID.LOGOUT_THUMB_DOWN;
				button = client.getWidget(WIDGET_REVIEW_BUTTON_1);
				break;
			case SPRITE_REVIEW_BUTTON_1_HOVERED:
				sprite = SpriteID.LOGOUT_THUMB_DOWN_HOVERED;
				button = client.getWidget(WIDGET_REVIEW_BUTTON_1);
				break;
			case SPRITE_REVIEW_BUTTON_5:
				sprite = SpriteID.LOGOUT_THUMB_UP;
				button = client.getWidget(WIDGET_REVIEW_BUTTON_5);
				break;
			case SPRITE_REVIEW_BUTTON_5_HOVERED:
				sprite = SpriteID.LOGOUT_THUMB_UP_HOVERED;
				button = client.getWidget(WIDGET_REVIEW_BUTTON_5);
				break;
			case SpriteID.LOGOUT_THUMB_UP:
				sprite = SPRITE_REVIEW_BUTTON_5;
				button = client.getWidget(WIDGET_REVIEW_BUTTON_5);
				break;
			case SpriteID.LOGOUT_THUMB_DOWN:
				sprite = SPRITE_REVIEW_BUTTON_1;
				button = client.getWidget(WIDGET_REVIEW_BUTTON_1);
				break;
			default:
				return;
		}
		if (button == null) return;
		child = button.getChild(9);
		if (child == null) return;
		child.setSpriteId(sprite);
		if (sprite == SPRITE_REVIEW_BUTTON_1 || sprite == SPRITE_REVIEW_BUTTON_5)
		{
			child.setOriginalWidth(ORIG_REVIEW_BUTTON_SPRITE_WIDTH);
			child.setOriginalHeight(ORIG_REVIEW_BUTTON_SPRITE_HEIGHT);
		}
		else
		{
			child.setOriginalWidth(REVIEW_BUTTON_SPRITE_SIZE);
			child.setOriginalHeight(REVIEW_BUTTON_SPRITE_SIZE);
		}
		child.revalidate();
	}

	private void replaceVoteUI()
	{
		if (client.getWidget(WIDGET_LOGOUT_LAYOUT) == null) return;
		hideUnusedButtons(true);
		replaceReviewText(true);
		replaceButtons(true, -1);
		replaceSprite(SPRITE_REVIEW_BUTTON_5);
		replaceSprite(SPRITE_REVIEW_BUTTON_1);
	}

	private void restoreVoteUI()
	{
		if (client.getWidget(WIDGET_LOGOUT_LAYOUT) == null) return;
		hideUnusedButtons(false);
		replaceReviewText(false);
		replaceButtons(false, -1);
		replaceSprite(SpriteID.LOGOUT_THUMB_UP);
		replaceSprite(SpriteID.LOGOUT_THUMB_DOWN);
	}

	private void hideUnusedButtons(boolean hide)
	{
		Widget b2 = client.getWidget(WIDGET_REVIEW_BUTTON_2);
		Widget b3 = client.getWidget(WIDGET_REVIEW_BUTTON_3);
		Widget b4 = client.getWidget(WIDGET_REVIEW_BUTTON_4);
		if (b2 == null || b3 == null || b4 == null) return;
		b2.setHidden(hide);
		b3.setHidden(hide);
		b4.setHidden(hide);
	}

	private void replaceReviewText(boolean replace)
	{
		Widget reviewText = client.getWidget(WIDGET_REVIEW_TEXT);
		if (reviewText == null) return;
		if (replace)
		{
			reviewText.setText("Did you enjoy playing<br>Old School RuneScape today?");
		}
		else
		{
			reviewText.setText("How much did you enjoy playing<br>Old School RuneScape today?");
		}
	}
	private void replaceButton(Widget button, int rbw, int rbh, int rbx)
	{
		Widget child;
		button.setOriginalWidth(rbw);
		button.setOriginalHeight(rbh);
		button.setOriginalX(rbx);
		button.revalidate();
		for (int i = 0; i < 9; i++)
		{
			child = button.getChild(i);
			if (child == null) return;
			child.revalidate();
		}
	}

	private void replaceButtons(boolean replace, int selectedButton)
	{
		Widget thumbsUp = client.getWidget(WIDGET_REVIEW_BUTTON_5);
		Widget thumbsDown = client.getWidget(WIDGET_REVIEW_BUTTON_1);
		if (thumbsUp == null || thumbsDown == null) return;
		if (replace)
		{
			replaceButton(
					thumbsUp,
					REVIEW_BUTTON_WIDTH,
					REVIEW_BUTTON_HEIGHT,
					REVIEW_BUTTON_5_X
			);
			replaceButton(
					thumbsDown,
					REVIEW_BUTTON_WIDTH,
					REVIEW_BUTTON_HEIGHT,
					REVIEW_BUTTON_1_X
			);
		}
		else
		{
			replaceButton(
					thumbsUp,
					ORIG_REVIEW_BUTTON_WIDTH,
					ORIG_REVIEW_BUTTON_HEIGHT,
					ORIG_REVIEW_BUTTON_5_X
			);
			replaceButton(
					thumbsDown,
					ORIG_REVIEW_BUTTON_WIDTH,
					ORIG_REVIEW_BUTTON_HEIGHT,
					ORIG_REVIEW_BUTTON_1_X
			);
		}

		switch (selectedButton)
		{
			case -1:
				break;
			case 1:
				unselect(thumbsUp);
				break;
			case 5:
				unselect(thumbsDown);
				break;
			default:
				return;
		}
	}

	private void select(Widget button)
	{
		for (int i = 0; i < 9; i++) {
			Widget child = button.getChild(i);
			if (child == null) return;
			child.setSpriteId(1150 + i);
		}
	}

	private void unselect(Widget button)
	{
		for (int i = 0; i < 9; i++)
		{
			Widget child = button.getChild(i);
			if (child == null) return;
			child.setSpriteId(1141 + i);
		}
	}
}
