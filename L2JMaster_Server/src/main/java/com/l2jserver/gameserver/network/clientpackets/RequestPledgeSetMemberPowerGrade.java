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
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.gameserver.model.ClanPrivilege;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2ClanMember;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * Format: (ch) Sd
 * @author -Wooden-
 */
public final class RequestPledgeSetMemberPowerGrade extends L2GameClientPacket
{
	private static final String _C__D0_15_REQUESTPLEDGESETMEMBERPOWERGRADE = "[C] D0:15 RequestPledgeSetMemberPowerGrade";
	private String _member;
	private int _powerGrade;
	
	@Override
	protected void readImpl()
	{
		_member = readS();
		_powerGrade = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final L2Clan clan = activeChar.getClan();
		if (clan == null)
		{
			return;
		}
		
		if (!activeChar.hasClanPrivilege(ClanPrivilege.CL_MANAGE_RANKS))
		{
			return;
		}
		
		final L2ClanMember member = clan.getClanMember(_member);
		if (member == null)
		{
			return;
		}
		
		if (member.getObjectId() == clan.getLeaderId())
		{
			return;
		}
		
		if (member.getPledgeType() == L2Clan.SUBUNIT_ACADEMY)
		{
			// also checked from client side
			activeChar.sendMessage("You cannot change academy member grade");
			return;
		}
		
		member.setPowerGrade(_powerGrade);
		clan.broadcastClanStatus();
	}
	
	@Override
	public String getType()
	{
		return _C__D0_15_REQUESTPLEDGESETMEMBERPOWERGRADE;
	}
}