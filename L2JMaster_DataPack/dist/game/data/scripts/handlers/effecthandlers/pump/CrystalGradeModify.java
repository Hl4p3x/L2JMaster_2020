/*
 * Copyright (C) 2004-2020 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.effecthandlers.pump;

import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.effects.AbstractEffect;
import com.l2jserver.gameserver.model.skills.BuffInfo;

/**
 * Crystal Grade Modify effect implementation.
 * @author Zoey76
 */
public final class CrystalGradeModify extends AbstractEffect
{
	private final int _grade;
	
	public CrystalGradeModify(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_grade = params.getInt("grade", 0);
	}
	
	@Override
	public boolean canStart(BuffInfo info)
	{
		return info.getEffected().isPlayer();
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		final L2PcInstance player = info.getEffected().getActingPlayer();
		if (player != null)
		{
			player.setExpertisePenaltyBonus(0);
			player.refreshExpertisePenalty();
		}
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		final L2PcInstance player = info.getEffected().getActingPlayer();
		if (player != null)
		{
			player.setExpertisePenaltyBonus(_grade);
			player.refreshExpertisePenalty();
		}
	}
}