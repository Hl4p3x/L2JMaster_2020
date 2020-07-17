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
package com.l2jserver.gameserver.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.l2jserver.gameserver.data.xml.impl.AdminData;
import com.l2jserver.gameserver.enums.PlayerAction;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ConfirmDlg;

/**
 * @author UnAfraid
 */
public class AdminCommandHandler implements IHandler<IAdminCommandHandler, String>
{
	private static final Logger LOG = Logger.getLogger(AdminCommandHandler.class.getName());
	private final Map<String, IAdminCommandHandler> _datatable;
	
	protected AdminCommandHandler()
	{
		_datatable = new HashMap<>();
	}
	
	@Override
	public void registerHandler(IAdminCommandHandler handler)
	{
		String[] ids = handler.getAdminCommandList();
		for (String id : ids)
		{
			_datatable.put(id, handler);
		}
	}
	
	@Override
	public synchronized void removeHandler(IAdminCommandHandler handler)
	{
		String[] ids = handler.getAdminCommandList();
		for (String id : ids)
		{
			_datatable.remove(id);
		}
	}
	
	@Override
	public IAdminCommandHandler getHandler(String adminCommand)
	{
		String command = adminCommand;
		if (adminCommand.contains(" "))
		{
			command = adminCommand.substring(0, adminCommand.indexOf(" "));
		}
		return _datatable.get(command);
	}
	
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	public void useAdminCommand(L2PcInstance player, String fullCommand, boolean useConfirm) {
        String command = fullCommand.split(" ")[0];
        String commandNoPrefix = command.substring(6);
        IAdminCommandHandler handler = this.getHandler(command);
        if (handler == null) {
            if (player.isGM()) {
                player.sendMessage("The command '" + commandNoPrefix + "' does not exist!");
            }
            LOG.warning("No handler registered for admin command '" + command + "'");
            return;
        }
        if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel())) {
            player.sendMessage("You don't have the access rights to use this command!");
            LOG.warning("Player " + player.getName() + " tried to use admin command '" + command + "', without proper access level!");
            return;
        }
        if (useConfirm && AdminData.getInstance().requireConfirm(command)) {
            player.setAdminConfirmCmd(fullCommand);
            ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1);
            dlg.addString("Are you sure you want execute command '" + commandNoPrefix + "' ?");
            player.addAction(PlayerAction.ADMIN_COMMAND);
            player.sendPacket(dlg);
        }
    }
	
	public static AdminCommandHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AdminCommandHandler _instance = new AdminCommandHandler();
	}
}
