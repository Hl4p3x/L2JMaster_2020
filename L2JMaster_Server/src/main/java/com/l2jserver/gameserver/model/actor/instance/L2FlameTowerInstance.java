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
package com.l2jserver.gameserver.model.actor.instance;

import java.util.List;

import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.instancemanager.ZoneManager;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Tower;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.zone.L2ZoneType;

/**
 * Class for Flame Control Tower instance.
 * @author JIV
 */
public class L2FlameTowerInstance extends L2Tower
{
	private int _upgradeLevel = 0;
	private List<Integer> _zoneList;
	
	/**
	 * Creates a flame tower.
	 * @param template the flame tower NPC template
	 */
	public L2FlameTowerInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2FlameTowerInstance);
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		enableZones(false);
		return super.doDie(killer);
	}
	
	@Override
	public boolean deleteMe()
	{
		enableZones(false);
		return super.deleteMe();
	}
	
	public final void enableZones(boolean state)
	{
		if ((_zoneList != null) && (_upgradeLevel != 0))
		{
			final int maxIndex = _upgradeLevel * 2;
			for (int i = 0; i < maxIndex; i++)
			{
				final L2ZoneType zone = ZoneManager.getInstance().getZoneById(_zoneList.get(i));
				if (zone != null)
				{
					zone.setEnabled(state);
				}
			}
		}
	}
	
	public final void setUpgradeLevel(int level)
	{
		_upgradeLevel = level;
	}
	
	public final void setZoneList(List<Integer> list)
	{
		_zoneList = list;
		enableZones(true);
	}
}