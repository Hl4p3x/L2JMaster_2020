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

import com.l2jserver.Config;
import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.util.Evolve;

public class L2PetManagerInstance extends L2MerchantInstance
{
	/**
	 * Creates a pet manager.
	 * @param template the pet manager NPC template.
	 */
	public L2PetManagerInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2PetManagerInstance);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/petmanager/" + pom + ".htm";
	}
	
	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filename = "data/html/petmanager/" + getId() + ".htm";
		if ((getId() == 36478) && player.hasSummon())
		{
			filename = "data/html/petmanager/restore-unsummonpet.htm";
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getHtmlPrefix(), filename);
		if (Config.ALLOW_RENTPET && Config.LIST_PET_RENT_NPC.contains(getId()))
		{
			html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass -h npc_%objectId%_Quest");
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("exchange"))
		{
			String[] params = command.split(" ");
			int val = Integer.parseInt(params[1]);
			switch (val)
			{
				case 1:
					exchange(player, 7585, 6650);
					break;
				case 2:
					exchange(player, 7583, 6648);
					break;
				case 3:
					exchange(player, 7584, 6649);
					break;
			}
			return;
		}
		else if (command.startsWith("evolve"))
		{
			String[] params = command.split(" ");
			int val = Integer.parseInt(params[1]);
			boolean ok = false;
			switch (val)
			{
			// Info evolve(player, "curent pet summon item", "new pet summon item", "lvl required to evolve")
			// To ignore evolve just put value 0 where do you like example: evolve(player, 0, 9882, 55);
				case 1:
					ok = Evolve.doEvolve(player, this, 2375, 9882, 55);
					break;
				case 2:
					ok = Evolve.doEvolve(player, this, 9882, 10426, 70);
					break;
				case 3:
					ok = Evolve.doEvolve(player, this, 6648, 10311, 55);
					break;
				case 4:
					ok = Evolve.doEvolve(player, this, 6650, 10313, 55);
					break;
				case 5:
					ok = Evolve.doEvolve(player, this, 6649, 10312, 55);
					break;
			}
			if (!ok)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/petmanager/evolve_no.htm");
				player.sendPacket(html);
			}
			return;
		}
		else if (command.startsWith("restore"))
		{
			String[] params = command.split(" ");
			int val = Integer.parseInt(params[1]);
			boolean ok = false;
			switch (val)
			{
			// Info evolve(player, "curent pet summon item", "new pet summon item", "lvl required to evolve")
				case 1:
					ok = Evolve.doRestore(player, this, 10307, 9882, 55);
					break;
				case 2:
					ok = Evolve.doRestore(player, this, 10611, 10426, 70);
					break;
				case 3:
					ok = Evolve.doRestore(player, this, 10308, 4422, 55);
					break;
				case 4:
					ok = Evolve.doRestore(player, this, 10309, 4423, 55);
					break;
				case 5:
					ok = Evolve.doRestore(player, this, 10310, 4424, 55);
					break;
			}
			if (!ok)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/petmanager/restore_no.htm");
				player.sendPacket(html);
			}
			return;
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public final void exchange(L2PcInstance player, int itemIdtake, int itemIdgive)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (player.destroyItemByItemId("Consume", itemIdtake, 1, this, true))
		{
			player.addItem("", itemIdgive, 1, this, true);
			html.setFile(player.getHtmlPrefix(), "data/html/petmanager/" + getId() + ".htm");
			player.sendPacket(html);
		}
		else
		{
			html.setFile(player.getHtmlPrefix(), "data/html/petmanager/exchange_no.htm");
			player.sendPacket(html);
		}
	}
}
