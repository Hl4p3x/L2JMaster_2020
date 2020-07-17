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
package handlers.voicedcommandhandlers;

import java.text.SimpleDateFormat;

import com.l2jserver.Config;
import com.l2jserver.gameserver.handler.IVoicedCommandHandler;
import com.l2jserver.gameserver.instancemanager.PremiumManager;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;

public class Premium implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"premium"
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (command.startsWith("premium") && Config.PREMIUM_SYSTEM_ENABLED)
		{
			SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
			long endDate = PremiumManager.getInstance().getPremiumEndDate(activeChar.getAccountName());
			if ((endDate == 0) && !activeChar.hasPremiumStatus())
			{
				NpcHtmlMessage msg = new NpcHtmlMessage(5);
				StringBuilder html = new StringBuilder("<html><body><title>Account Details</title><center>");
				html.append("<table>");
				html.append("<tr><td><center>Account Status: <font color=\"LEVEL\">Normal<br></font></td></tr>");
				html.append("<tr><td>Rate XP: <font color=\"LEVEL\"> x" + Config.RATE_XP + "<br1></font></td></tr>");
				html.append("<tr><td>Rate SP: <font color=\"LEVEL\"> x" + Config.RATE_SP + "<br1></font></td></tr>");
				html.append("<tr><td>Drop Chance: <font color=\"LEVEL\"> x" + Config.RATE_DEATH_DROP_CHANCE_MULTIPLIER + "<br1></font></td></tr><br>");
				html.append("<tr><td>Drop Amount: <font color=\"LEVEL\"> x" + Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER + "<br1></font></td></tr><br>");
				html.append("<tr><td><center>Premium Info & Rules<br></td></tr>");
				html.append("<tr><td>Rate XP: <font color=\"LEVEL\"> x" + (Config.RATE_XP * Config.PREMIUM_RATE_XP) + "<br1></font></td></tr>");
				html.append("<tr><td>Rate SP: <font color=\"LEVEL\"> x" + (Config.RATE_SP * Config.PREMIUM_RATE_SP) + "<br1></font></td></tr>");
				html.append("<tr><td>Drop Chance: <font color=\"LEVEL\"> x" + (Config.RATE_DEATH_DROP_CHANCE_MULTIPLIER * Config.RATE_DEATH_DROP_CHANCE_MULTIPLIER * Config.PREMIUM_RATE_DROP_CHANCE) + "<br1></font></td></tr>");
				html.append("<tr><td>Drop Amount: <font color=\"LEVEL\"> x" + (Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * Config.PREMIUM_RATE_DROP_AMOUNT) + "<br1></font></td></tr>");
				html.append("<tr><td> <font color=\"70FFCA\">1. Premium benefits CAN NOT BE TRANSFERED.<br1></font></td></tr>");
				html.append("<tr><td> <font color=\"70FFCA\">2. Premium does not effect party members.<br1></font></td></tr>");
				html.append("<tr><td> <font color=\"70FFCA\">3. Premium benefits effect ALL characters in same account.</font></td></tr>");
				html.append("</table>");
				html.append("</center></body></html>");
				msg.setHtml(html.toString());
				activeChar.sendPacket(msg);
			}
			else
			{
				NpcHtmlMessage msg = new NpcHtmlMessage(5);
				StringBuilder html = new StringBuilder("<html><body><title>Premium Account Details</title><center>");
				html.append("<table>");
				html.append("<tr><td><center>Account Status: <font color=\"LEVEL\">Premium<br></font></td></tr>");
				html.append("<tr><td>Rate XP: <font color=\"LEVEL\">x" + (Config.RATE_XP * Config.PREMIUM_RATE_XP) + " <br1></font></td></tr>");
				html.append("<tr><td>Rate SP: <font color=\"LEVEL\">x" + (Config.RATE_SP * Config.PREMIUM_RATE_SP) + "  <br1></font></td></tr>");
				html.append("<tr><td>Drop Chance: <font color=\"LEVEL\">x" + (Config.RATE_DEATH_DROP_CHANCE_MULTIPLIER * Config.PREMIUM_RATE_DROP_CHANCE) + " <br1></font></td></tr>");
				html.append("<tr><td>Drop Amount: <font color=\"LEVEL\">x" + (Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * Config.PREMIUM_RATE_DROP_AMOUNT) + " <br1></font></td></tr>");
				html.append("<tr><td>Expires: <font color=\"00A5FF\">" + String.valueOf(format.format(endDate)) + "</font></td></tr>");
				html.append("<tr><td>Current Date: <font color=\"70FFCA\">" + String.valueOf(format.format(System.currentTimeMillis())) + "<br><br></font></td></tr>");
				html.append("<tr><td><center>Premium Info & Rules<br></center></td></tr>");
				html.append("<tr><td><font color=\"70FFCA\">1. Premium accounts CAN NOT BE TRANSFERED.<br1></font></td></tr>");
				html.append("<tr><td><font color=\"70FFCA\">2. Premium does not effect party members.<br1></font></td></tr>");
				html.append("<tr><td><font color=\"70FFCA\">3. Premium account effects ALL characters in same account.<br><br><br></font></td></tr>");
				html.append("<tr><td><center>Thank you for supporting our server.</td></tr>");
				html.append("</table>");
				html.append("</center></body></html>");
				msg.setHtml(html.toString());
				activeChar.sendPacket(msg);
			}
		}
		else
		{
			return false;
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
