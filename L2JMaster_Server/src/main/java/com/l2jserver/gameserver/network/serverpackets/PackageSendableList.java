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
package com.l2jserver.gameserver.network.serverpackets;

import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;

/**
 * @author -Wooden-
 * @author UnAfraid, mrTJO
 */
public class PackageSendableList extends AbstractItemPacket
{
	private final L2ItemInstance[] _items;
	private final int _playerObjId;
	
	public PackageSendableList(L2ItemInstance[] items, int playerObjId)
	{
		_items = items;
		_playerObjId = playerObjId;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xD2);
		writeD(_playerObjId);
		writeQ(getClient().getActiveChar().getAdena());
		writeD(_items.length);
		for (L2ItemInstance item : _items)
		{
			writeItem(item);
			writeD(item.getObjectId());
		}
	}
}
