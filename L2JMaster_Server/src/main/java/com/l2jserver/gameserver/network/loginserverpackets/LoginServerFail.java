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
package com.l2jserver.gameserver.network.loginserverpackets;

import com.l2jserver.util.network.BaseRecievePacket;

public class LoginServerFail extends BaseRecievePacket
{
	
	private static final String[] REASONS =
	{
		"None",
		"Reason: ip banned",
		"Reason: ip reserved",
		"Reason: wrong hexid",
		"Reason: id reserved",
		"Reason: no free ID",
		"Not authed",
		"Reason: already logged in"
	};
	private final int _reason;
	
	/**
	 * @param decrypt
	 */
	public LoginServerFail(byte[] decrypt)
	{
		super(decrypt);
		_reason = readC();
	}
	
	public String getReasonString()
	{
		return REASONS[_reason];
	}
	
	public int getReason()
	{
		return _reason;
	}
	
}