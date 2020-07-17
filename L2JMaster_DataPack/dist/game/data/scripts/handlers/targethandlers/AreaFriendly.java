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
package handlers.targethandlers;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.l2jserver.gameserver.GeoData;
import com.l2jserver.gameserver.handler.ITargetTypeHandler;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.model.skills.targets.L2TargetType;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.network.SystemMessageId;

/**
 * Area Friendly target handler implementation.
 * @author Adry_85, Zoey76
 */
public class AreaFriendly implements ITargetTypeHandler
{
	private static final CharComparator CHAR_COMPARATOR = new CharComparator();
	
	@Override
	public L2Object[] getTargetList(Skill skill, L2Character activeChar, boolean onlyFirst, L2Character target)
	{
		L2PcInstance player = activeChar.getActingPlayer();
		
		if (!checkTarget(player, target) && (skill.getCastRange() >= 0))
		{
			player.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return EMPTY_TARGET_LIST;
		}
		
		if (onlyFirst)
		{
			return new L2Character[]
			{
				target
			};
		}
		
		if (player.getActingPlayer().isInOlympiadMode())
		{
			return new L2Character[]
			{
				player
			};
		}
		
		final List<L2Character> targetList = new LinkedList<>();
		if (target != null)
		{
			// Add target to target list.
			targetList.add(target);
			
			final int maxTargets = skill.getAffectLimit();
			for (L2Character obj : target.getKnownList().getKnownCharactersInRadius(skill.getAffectRange()))
			{
				if ((maxTargets > 0) && (targetList.size() >= maxTargets))
				{
					break;
				}
				
				if (!checkTarget(player, obj) || (obj == activeChar))
				{
					continue;
				}
				
				targetList.add(obj);
			}
			
			// Sort creatures, the most injured first.
			Collections.sort(targetList, CHAR_COMPARATOR);
		}
		
		if (targetList.isEmpty())
		{
			return EMPTY_TARGET_LIST;
		}
		return targetList.toArray(new L2Character[targetList.size()]);
	}
	
	private boolean checkTarget(L2PcInstance activeChar, L2Character target)
	{
		if (!GeoData.getInstance().canSeeTarget(activeChar, target))
		{
			return false;
		}
		
		if ((target == null) || target.isAlikeDead() || target.isDoor() || (target instanceof L2SiegeFlagInstance) || target.isMonster())
		{
			return false;
		}
		
		// GMs and hidden creatures.
		if (target.isInvisible())
		{
			return false;
		}
		
		if (target.isPlayable())
		{
			L2PcInstance targetPlayer = target.getActingPlayer();
			
			if (activeChar == targetPlayer)
			{
				return true;
			}
			
			if (targetPlayer.inObserverMode() || targetPlayer.isInOlympiadMode())
			{
				return false;
			}
			
			if (activeChar.isInDuelWith(target))
			{
				return false;
			}
			
			if (activeChar.isInPartyWith(target))
			{
				return true;
			}
			
			// Only siege allies.
			if (activeChar.isInSiege() && !activeChar.isOnSameSiegeSideWith(targetPlayer))
			{
				return false;
			}
			
			if (target.isInsideZone(ZoneId.PVP))
			{
				return false;
			}
			
			if (activeChar.isInClanWith(target) || activeChar.isInAllyWith(target) || activeChar.isInCommandChannelWith(target))
			{
				return true;
			}
			
			if ((targetPlayer.getPvpFlag() > 0) || (targetPlayer.getKarma() > 0))
			{
				return false;
			}
		}
		return true;
	}
	
	public static class CharComparator implements Comparator<L2Character>
	{
		@Override
		public int compare(L2Character char1, L2Character char2)
		{
			return Double.compare((char1.getCurrentHp() / char1.getMaxHp()), (char2.getCurrentHp() / char2.getMaxHp()));
		}
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.AREA_FRIENDLY;
	}
}