/*
 * Copyright (C) 2004-2020 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model.events.impl.character.player;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.events.EventType;
import com.l2jserver.gameserver.model.events.impl.IBaseEvent;

/**
 * @author UnAfraid
 */
public class OnPlayerDlgAnswer implements IBaseEvent
{
	private final L2PcInstance _activeChar;
	private final int _messageId;
	private final int _answer;
	private final int _requesterId;
	
	public OnPlayerDlgAnswer(L2PcInstance activeChar, int messageId, int answer, int requesterId)
	{
		_activeChar = activeChar;
		_messageId = messageId;
		_answer = answer;
		_requesterId = requesterId;
	}
	
	public L2PcInstance getActiveChar()
	{
		return _activeChar;
	}
	
	public int getMessageId()
	{
		return _messageId;
	}
	
	public int getAnswer()
	{
		return _answer;
	}
	
	public int getRequesterId()
	{
		return _requesterId;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_DLG_ANSWER;
	}
	
}
