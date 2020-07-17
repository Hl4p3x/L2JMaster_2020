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
package com.l2jserver.gameserver.ai;

import java.util.ArrayList;
import java.util.List;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2DefenderInstance;

/**
 * @author BiggBoss
 */
public final class L2SpecialSiegeGuardAI extends L2SiegeGuardAI
{
	private final List<Integer> _allied = new ArrayList<>();
	
	/**
	 * @param creature
	 */
	public L2SpecialSiegeGuardAI(L2DefenderInstance creature)
	{
		super(creature);
	}
	
	public List<Integer> getAlly()
	{
		return _allied;
	}
	
	@Override
	protected boolean autoAttackCondition(L2Character target)
	{
		if (_allied.contains(target.getObjectId()))
		{
			return false;
		}
		
		return super.autoAttackCondition(target);
	}
}
