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
package ai.npc.CastleBlacksmith;

import com.l2jserver.gameserver.model.ClanPrivilege;
import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

import ai.npc.AbstractNpcAI;

/**
 * Castle Blacksmith AI.
 * @author malyelfik
 */
public final class CastleBlacksmith extends AbstractNpcAI
{
	// Blacksmith IDs
	private static final int[] NPCS =
	{
		35098, // Blacksmith (Gludio)
		35140, // Blacksmith (Dion)
		35182, // Blacksmith (Giran)
		35224, // Blacksmith (Oren)
		35272, // Blacksmith (Aden)
		35314, // Blacksmith (Innadril)
		35361, // Blacksmith (Goddard)
		35507, // Blacksmith (Rune)
		35553, // Blacksmith (Schuttgart)
	};
	
	private CastleBlacksmith()
	{
		super(CastleBlacksmith.class.getSimpleName(), "ai/npc");
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	private boolean hasRights(L2PcInstance player, L2Npc npc)
	{
		return player.canOverrideCond(PcCondOverride.CASTLE_CONDITIONS) || npc.isMyLord(player) || ((player.getClanId() == npc.getCastle().getOwnerId()) && player.hasClanPrivilege(ClanPrivilege.CS_MANOR_ADMIN));
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		return (event.equalsIgnoreCase(npc.getId() + "-02.html") && hasRights(player, npc)) ? event : null;
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		return (hasRights(player, npc)) ? npc.getId() + "-01.html" : "no.html";
	}
	
	public static void main(String[] args)
	{
		new CastleBlacksmith();
	}
}