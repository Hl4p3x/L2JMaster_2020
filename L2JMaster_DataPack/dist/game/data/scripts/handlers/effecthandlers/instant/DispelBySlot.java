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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.CharEffectList;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.effects.AbstractEffect;
import com.l2jserver.gameserver.model.effects.L2EffectType;
import com.l2jserver.gameserver.model.skills.AbnormalType;
import com.l2jserver.gameserver.model.skills.BuffInfo;

/**
 * Dispel By Slot effect implementation.
 * @author Gnacik, Zoey76, Adry_85
 */
public final class DispelBySlot extends AbstractEffect
{
	private final String _dispel;
	private final Map<AbnormalType, Short> _dispelAbnormals;
	
	public DispelBySlot(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_dispel = params.getString("dispel", null);
		if ((_dispel != null) && !_dispel.isEmpty())
		{
			_dispelAbnormals = new EnumMap<>(AbnormalType.class);
			for (String ngtStack : _dispel.split(";"))
			{
				String[] ngt = ngtStack.split(",");
				_dispelAbnormals.put(AbnormalType.getAbnormalType(ngt[0]), Short.parseShort(ngt[1]));
			}
		}
		else
		{
			_dispelAbnormals = Collections.<AbnormalType, Short> emptyMap();
		}
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.DISPEL;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		if (_dispelAbnormals.isEmpty())
		{
			return;
		}
		
		final List<BuffInfo> canceled = new ArrayList<>();
		
		final L2Character effected = info.getEffected();
		final CharEffectList effectList = effected.getEffectList();
		// There is no need to iterate over all buffs,
		// Just iterate once over all slots to dispel and get the buff with that abnormal if exists,
		// Operation of O(n) for the amount of slots to dispel (which is usually small) and O(1) to get the buff.
		for (Entry<AbnormalType, Short> entry : _dispelAbnormals.entrySet())
		{
			// Dispel transformations (buff and by GM)
			if ((entry.getKey() == AbnormalType.TRANSFORM))
			{
				if (effected.isTransformed() || (effected.isPlayer() || (entry.getValue() == effected.getActingPlayer().getTransformationId()) || (entry.getValue() < 0)))
				{
					info.getEffected().stopTransformation(true);
					continue;
				}
			}
			
			final BuffInfo toDispel = effectList.getBuffInfoByAbnormalType(entry.getKey());
			if (toDispel == null)
			{
				continue;
			}
			
			if ((entry.getKey() == toDispel.getSkill().getAbnormalType()) && ((entry.getValue() < 0) || (entry.getValue() >= toDispel.getSkill().getAbnormalLvl())))
			{
				if (Config.RESTORE_CANCELED_BUFFS_ENABLED && info.getEffected().isPlayer() && !info.getEffected().equals(info.getEffector()) && !((L2PcInstance) info.getEffected()).isInOlympiadMode())
				{
					canceled.add(toDispel);
				}
				effectList.stopSkillEffects(true, entry.getKey());
				if ((effected.getSummon() != null) && (effected.getSummon().getEffectList() != null))
				{
					final CharEffectList servitorEffectList = effected.getSummon().getEffectList();
					if (!servitorEffectList.isEmpty())
					{
						servitorEffectList.stopSkillEffects(true, entry.getKey());
					}
				}
			}
		}
		
		if (Config.RESTORE_CANCELED_BUFFS_ENABLED && info.getEffected().isPlayer() && !info.getEffected().equals(info.getEffector()) && !canceled.isEmpty())
		{
			((L2PcInstance) info.getEffected()).recoverCancelledBuffs(canceled, Config.RESTORE_CANCELED_BUFFS_TIME);
		}
	}
}
