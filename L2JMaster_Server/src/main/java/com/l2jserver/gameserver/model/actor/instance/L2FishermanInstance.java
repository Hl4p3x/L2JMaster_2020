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

import com.l2jserver.Config;
import com.l2jserver.gameserver.data.xml.impl.SkillTreesData;
import com.l2jserver.gameserver.datatables.SkillData;
import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.instancemanager.FishingChampionshipManager;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.base.AcquireSkillType;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

public final class L2FishermanInstance extends L2MerchantInstance
{
	/**
	 * Creates a fisherman.
	 * @param template the fisherman NPC template
	 */
	public L2FishermanInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2FishermanInstance);
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
		
		return "data/html/fisherman/" + pom + ".htm";
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.equalsIgnoreCase("FishSkillList"))
		{
			showFishSkillList(player);
		}
		else if (command.startsWith("FishingChampionship"))
		{
			if (Config.ALT_FISH_CHAMPIONSHIP_ENABLED)
			{
				FishingChampionshipManager.getInstance().showChampScreen(player, this);
			}
			else
			{
				sendHtml(player, this, "no_fish_event001.htm");
			}
		}
		else if (command.startsWith("FishingReward"))
		{
			if (Config.ALT_FISH_CHAMPIONSHIP_ENABLED)
			{
				if (FishingChampionshipManager.getInstance().isWinner(player.getName()))
				{
					FishingChampionshipManager.getInstance().getReward(player);
				}
				else
				{
					sendHtml(player, this, "no_fish_event_reward001.htm");
				}
			}
			else
			{
				sendHtml(player, this, "no_fish_event001.htm");
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public static void showFishSkillList(L2PcInstance player)
	{
		final List<L2SkillLearn> skills = SkillTreesData.getInstance().getAvailableFishingSkills(player);
		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.FISHING);
		
		int count = 0;
		
		for (L2SkillLearn s : skills)
		{
			final Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
			
			if (sk == null)
			{
				continue;
			}
			count++;
			asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getLevelUpSp(), 1);
		}
		
		if (count == 0)
		{
			final int minlLevel = SkillTreesData.getInstance().getMinLevelForNewSkill(player, SkillTreesData.getInstance().getFishingSkillTree());
			if (minlLevel > 0)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
				sm.addInt(minlLevel);
				player.sendPacket(sm);
			}
			else
			{
				player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
			}
		}
		else
		{
			player.sendPacket(asl);
		}
	}
	
	private static void sendHtml(L2PcInstance player, L2Npc npc, String htmlName)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player.getHtmlPrefix(), "data/html/fisherman/championship/" + htmlName);
		player.sendPacket(html);
	}
}
