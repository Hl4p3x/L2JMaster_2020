/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.admincommandhandlers;

import java.util.StringTokenizer;

import com.l2jserver.Config;
import com.l2jserver.gameserver.handler.AdminCommandHandler;
import com.l2jserver.gameserver.handler.IAdminCommandHandler;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.util.BuilderUtil;
import com.l2jserver.gameserver.util.Util;

/**
 * A retail-like implementation of //gmspeed builder command.
 * @author lord_rex
 */
public final class AdminGmSpeed implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_gmspeed",
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance player)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.nextToken();
		
		if (cmd.equals("admin_gmspeed"))
		{
			if (!st.hasMoreTokens())
			{
				BuilderUtil.sendSysMessage(player, "//gmspeed [0...5]");
				return false;
			}
			final String token = st.nextToken();
			
			// Rollback feature for old custom way, in order to make everyone happy.
			if (Config.USE_SUPER_HASTE_AS_GM_SPEED)
			{
				AdminCommandHandler.getInstance().useAdminCommand(player, AdminSuperHaste.ADMIN_COMMANDS[0] + " " + token, false);
				return true;
			}
			
			if (!Util.isDouble(token))
			{
				BuilderUtil.sendSysMessage(player, "//gmspeed [0...5]");
				return false;
			}
			final double runSpeedBoost = Double.parseDouble(token);
			if ((runSpeedBoost < 0) || (runSpeedBoost > 5))
			{
				// Custom limit according to SDW's request - real retail limit is unknown.
				BuilderUtil.sendSysMessage(player, "//gmspeed [0...5]");
				return false;
			}
			
			L2Character targetCharacter = player;
			final L2Object target = player.getTarget();
			if ((target != null) && target.isCharacter())
			{
				targetCharacter = player;
			}
			
			targetCharacter.getStat().setGmSpeedMultiplier(runSpeedBoost > 0 ? runSpeedBoost : 1);
			
			if (targetCharacter.isPlayer())
			{
				((L2PcInstance) targetCharacter).broadcastUserInfo();
			}
			else
			{
				targetCharacter.broadcastInfo();
			}
			
			BuilderUtil.sendSysMessage(player, "Your speed is [" + (runSpeedBoost * 50) + "0]% fast.");
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}