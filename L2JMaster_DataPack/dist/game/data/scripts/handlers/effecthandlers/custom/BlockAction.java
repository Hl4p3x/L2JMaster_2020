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
package handlers.effecthandlers.custom;

import java.util.HashSet;
import java.util.Set;

import com.l2jserver.gameserver.datatables.BotReportTable;
import com.l2jserver.gameserver.instancemanager.PunishmentManager;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.effects.AbstractEffect;
import com.l2jserver.gameserver.model.punishment.PunishmentAffect;
import com.l2jserver.gameserver.model.punishment.PunishmentTask;
import com.l2jserver.gameserver.model.punishment.PunishmentType;
import com.l2jserver.gameserver.model.skills.BuffInfo;

/**
 * Block Action effect implementation.
 * @author BiggBoss
 */
public final class BlockAction extends AbstractEffect
{
	private final Set<Integer> _blockedActions = new HashSet<>();
	
	public BlockAction(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		final String[] actions = params.getString("blockedActions").split(",");
		for (String action : actions)
		{
			_blockedActions.add(Integer.parseInt(action));
		}
	}
	
	@Override
	public boolean canStart(BuffInfo info)
	{
		return (info.getEffected() != null) && info.getEffected().isPlayer();
	}
	
	@Override
	public boolean checkCondition(Object id)
	{
		return !_blockedActions.contains(id);
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		if (_blockedActions.contains(BotReportTable.PARTY_ACTION_BLOCK_ID))
		{
			PunishmentManager.getInstance().stopPunishment(info.getEffected().getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN);
		}
		if (_blockedActions.contains(BotReportTable.CHAT_BLOCK_ID))
		{
			PunishmentManager.getInstance().stopPunishment(info.getEffected().getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN);
		}
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (_blockedActions.contains(BotReportTable.PARTY_ACTION_BLOCK_ID))
		{
			PunishmentManager.getInstance().startPunishment(new PunishmentTask(0, info.getEffected().getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN, 0, "block action debuff", "system", true));
		}
		
		if (_blockedActions.contains(BotReportTable.CHAT_BLOCK_ID))
		{
			PunishmentManager.getInstance().startPunishment(new PunishmentTask(0, info.getEffected().getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN, 0, "block action debuff", "system", true));
		}
	}
}
