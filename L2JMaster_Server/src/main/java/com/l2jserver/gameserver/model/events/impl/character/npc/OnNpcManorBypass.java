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
package com.l2jserver.gameserver.model.events.impl.character.npc;

import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.events.EventType;
import com.l2jserver.gameserver.model.events.impl.IBaseEvent;

/**
 * @author malyelfik
 */
public final class OnNpcManorBypass implements IBaseEvent
{
	private final L2PcInstance _activeChar;
	private final L2Npc _target;
	private final int _request;
	private final int _manorId;
	private final boolean _nextPeriod;
	
	public OnNpcManorBypass(L2PcInstance activeChar, L2Npc target, int request, int manorId, boolean nextPeriod)
	{
		_activeChar = activeChar;
		_target = target;
		_request = request;
		_manorId = manorId;
		_nextPeriod = nextPeriod;
	}
	
	public L2PcInstance getActiveChar()
	{
		return _activeChar;
	}
	
	public L2Npc getTarget()
	{
		return _target;
	}
	
	public int getRequest()
	{
		return _request;
	}
	
	public int getManorId()
	{
		return _manorId;
	}
	
	public boolean isNextPeriod()
	{
		return _nextPeriod;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_MANOR_BYPASS;
	}
}