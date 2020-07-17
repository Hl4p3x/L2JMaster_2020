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
package ai.npc.Teleports.CrumaTower;

import ai.npc.AbstractNpcAI;

import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * Cruma Tower teleport AI.
 * @author Plim
 */
public final class CrumaTower extends AbstractNpcAI
{
	// NPC
	private static final int MOZELLA = 30483;
	// Locations
	private static final Location TELEPORT_LOC1 = new Location(17776, 113968, -11671);
	private static final Location TELEPORT_LOC2 = new Location(17680, 113968, -11671);
	// Misc
	private static final int MAX_LEVEL = 55;
	
	private CrumaTower()
	{
		super(CrumaTower.class.getSimpleName(), "ai/npc/Teleports");
		addFirstTalkId(MOZELLA);
		addStartNpc(MOZELLA);
		addTalkId(MOZELLA);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		if (talker.getLevel() <= MAX_LEVEL)
		{
			talker.teleToLocation(getRandomBoolean() ? TELEPORT_LOC1 : TELEPORT_LOC2);
			return null;
		}
		return "30483-1.html";
	}
	
	public static void main(String[] args)
	{
		new CrumaTower();
	}
}