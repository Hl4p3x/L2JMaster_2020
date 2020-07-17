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
package com.l2jserver.gameserver.model.interfaces;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author UnAfraid
 */
public interface IEventListener
{
	/**
	 * @return {@code true} if player is on event, {@code false} otherwise.
	 */
	public boolean isOnEvent();
	
	/**
	 * @return {@code true} if player is blocked from leaving the game, {@code false} otherwise.
	 */
	public boolean isBlockingExit();
	
	/**
	 * @return {@code true} if player is blocked from receiving death penalty upon death, {@code false} otherwise.
	 */
	public boolean isBlockingDeathPenalty();
	
	/**
	 * @return {@code true} if player can revive after death, {@code false} otherwise.
	 */
	public boolean canRevive();
	
	public L2PcInstance getPlayer();
}
