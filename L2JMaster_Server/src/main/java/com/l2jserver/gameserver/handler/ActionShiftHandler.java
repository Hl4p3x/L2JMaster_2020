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
package com.l2jserver.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

import com.l2jserver.gameserver.enums.InstanceType;

/**
 * @author UnAfraid
 */
public class ActionShiftHandler implements IHandler<IActionShiftHandler, InstanceType>
{
	private final Map<InstanceType, IActionShiftHandler> _actionsShift;
	
	protected ActionShiftHandler()
	{
		_actionsShift = new HashMap<>();
	}
	
	@Override
	public void registerHandler(IActionShiftHandler handler)
	{
		_actionsShift.put(handler.getInstanceType(), handler);
	}
	
	@Override
	public synchronized void removeHandler(IActionShiftHandler handler)
	{
		_actionsShift.remove(handler.getInstanceType());
	}
	
	@Override
	public IActionShiftHandler getHandler(InstanceType iType)
	{
		IActionShiftHandler result = null;
		for (InstanceType t = iType; t != null; t = t.getParent())
		{
			result = _actionsShift.get(t);
			if (result != null)
			{
				break;
			}
		}
		return result;
	}
	
	@Override
	public int size()
	{
		return _actionsShift.size();
	}
	
	public static ActionShiftHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ActionShiftHandler _instance = new ActionShiftHandler();
	}
}