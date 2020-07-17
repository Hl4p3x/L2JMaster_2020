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
package com.l2jserver.gameserver.model.itemcontainer;

import com.l2jserver.Config;
import com.l2jserver.gameserver.enums.ItemLocation;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.events.EventDispatcher;
import com.l2jserver.gameserver.model.events.impl.character.player.clanwh.OnPlayerClanWHItemAdd;
import com.l2jserver.gameserver.model.events.impl.character.player.clanwh.OnPlayerClanWHItemDestroy;
import com.l2jserver.gameserver.model.events.impl.character.player.clanwh.OnPlayerClanWHItemTransfer;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;

public final class ClanWarehouse extends Warehouse
{
	private final L2Clan _clan;
	
	public ClanWarehouse(L2Clan clan)
	{
		_clan = clan;
	}
	
	@Override
	public String getName()
	{
		return "ClanWarehouse";
	}
	
	@Override
	public int getOwnerId()
	{
		return _clan.getId();
	}
	
	@Override
	public L2PcInstance getOwner()
	{
		return _clan.getLeader().getPlayerInstance();
	}
	
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.CLANWH;
	}
	
	public String getLocationId()
	{
		return "0";
	}
	
	public int getLocationId(boolean dummy)
	{
		return 0;
	}
	
	public void setLocationId(L2PcInstance dummy)
	{
	}
	
	@Override
	public boolean validateCapacity(long slots)
	{
		return ((_items.size() + slots) <= Config.WAREHOUSE_SLOTS_CLAN);
	}
	
	@Override
	public L2ItemInstance addItem(String process, int itemId, long count, int enchantLevel, L2PcInstance actor, Object reference)
	{
		final L2ItemInstance item = super.addItem(process, itemId, count, enchantLevel, actor, reference);
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemAdd(process, actor, item, this), item.getItem());
		return item;
	}
	
	@Override
	public L2ItemInstance addItem(String process, L2ItemInstance item, L2PcInstance actor, Object reference)
	{
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemAdd(process, actor, item, this), item.getItem());
		return super.addItem(process, item, actor, reference);
	}
	
	@Override
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, long count, L2PcInstance actor, Object reference)
	{
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemDestroy(process, actor, item, count, this), item.getItem());
		return super.destroyItem(process, item, count, actor, reference);
	}
	
	@Override
	public L2ItemInstance transferItem(String process, int objectId, long count, ItemContainer target, L2PcInstance actor, Object reference)
	{
		final L2ItemInstance item = getItemByObjectId(objectId);
		
		// Notify to scripts
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemTransfer(process, actor, item, count, target), item.getItem());
		return super.transferItem(process, objectId, count, target, actor, reference);
	}
}
