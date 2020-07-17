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

import com.l2jserver.gameserver.GeoData;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.effects.AbstractEffect;
import com.l2jserver.gameserver.model.effects.EffectFlag;
import com.l2jserver.gameserver.model.skills.BuffInfo;
import com.l2jserver.gameserver.network.serverpackets.FlyToLocation;
import com.l2jserver.gameserver.network.serverpackets.FlyToLocation.FlyType;
import com.l2jserver.gameserver.network.serverpackets.ValidateLocation;

/**
 * Throw Up effect implementation.
 */
public final class ThrowUp extends AbstractEffect
{
	private final int _flyRadius;
	
	public ThrowUp(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_flyRadius = params.getInt("flyRadius", 0);
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.STUNNED.getMask();
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		L2Character target = info.getEffected();
		L2Character activeChar = info.getEffector();
		
		// Get current position of the L2Character
		final int curX = target.getX();
		final int curY = target.getY();
		final int curZ = target.getZ();
		
		// Calculate distance between effector and effected current position
		double dx = activeChar.getX() - curX;
		double dy = activeChar.getY() - curY;
		double dz = activeChar.getZ() - curZ;
		double distance = Math.hypot(dx, dy);
		if (distance > 2000)
		{
			_log.info("EffectThrow was going to use invalid coordinates for characters, getEffected: " + curX + "," + curY + " and getEffector: " + activeChar.getX() + "," + activeChar.getY());
			return;
		}
		int offset = Math.min((int) distance + _flyRadius, 1400);
		
		// approximation for moving futher when z coordinates are different
		// TODO: handle Z axis movement better
		offset += Math.abs(dz);
		if (offset < 5)
		{
			offset = 5;
		}
		
		// If no distance
		if (distance < 1)
		{
			return;
		}
		
		// Calculate movement angles needed
		double sin = dy / distance;
		double cos = dx / distance;
		
		// Calculate the new destination with offset included
		int x = activeChar.getX() - (int) (offset * cos);
		int y = activeChar.getY() - (int) (offset * sin);
		int z = target.getZ();
		
		final Location destination = GeoData.getInstance().moveCheck(curX, curY, curZ, x, y, z, target.getInstanceId());
		
		target.broadcastPacket(new FlyToLocation(target, destination, FlyType.THROW_UP));
		
		// TODO: Review.
		target.setXYZ(destination);
		target.broadcastPacket(new ValidateLocation(target));
	}
}