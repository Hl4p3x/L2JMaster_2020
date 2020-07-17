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
package handlers.effecthandlers.instant;

import java.util.List;

import com.l2jserver.Config;
import com.l2jserver.gameserver.enums.DispelCategory;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.effects.AbstractEffect;
import com.l2jserver.gameserver.model.effects.L2EffectType;
import com.l2jserver.gameserver.model.skills.BuffInfo;
import com.l2jserver.gameserver.model.skills.EffectScope;
import com.l2jserver.gameserver.model.stats.Formulas;

/**
 * Steal Abnormal effect implementation.
 * @author Adry_85, Zoey76
 */
public final class StealAbnormal extends AbstractEffect
{
	private final DispelCategory _slot;
	private final int _rate;
	private final int _max;
	
	public StealAbnormal(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_slot = params.getEnum("slot", DispelCategory.class, DispelCategory.BUFF);
		_rate = params.getInt("rate", 0);
		_max = params.getInt("max", 0);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.STEAL_ABNORMAL;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if ((info.getEffected() != null) && info.getEffected().isPlayer() && (info.getEffector() != info.getEffected()))
		{
			final List<BuffInfo> toSteal = Formulas.calcStealEffects(info.getEffector(), info.getEffected(), info.getSkill(), _slot, _rate, _max);
			if (toSteal.isEmpty())
			{
				return;
			}
			
			for (BuffInfo infoToSteal : toSteal)
			{
				// Invert effected and effector.
				final BuffInfo stolen = new BuffInfo(info.getEffected(), info.getEffector(), infoToSteal.getSkill());
				stolen.setAbnormalTime(infoToSteal.getTime()); // Copy the remaining time.
				// To include all the effects, it's required to go through the template rather the buff info.
				infoToSteal.getSkill().applyEffectScope(EffectScope.GENERAL, stolen, true, true);
				info.getEffected().getEffectList().remove(true, infoToSteal);
				info.getEffector().getEffectList().add(stolen);
			}
			
			if (Config.RESTORE_STOLEN_BUFFS_ENABLED && info.getEffected().isPlayer() && !info.getEffected().equals(info.getEffector()) && !toSteal.isEmpty())
			{
				((L2PcInstance) info.getEffected()).recoverCancelledBuffs(toSteal, Config.RESTORE_STOLEN_BUFFS_TIME);
			}
		}
	}
}