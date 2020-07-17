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

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExVariationCancelResult;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.util.Util;

/**
 * Format(ch) d
 * @author -Wooden-
 */
public final class RequestRefineCancel extends L2GameClientPacket
{
	private static final String _C__D0_43_REQUESTREFINECANCEL = "[C] D0:43 RequestRefineCancel";
	private int _targetItemObjId;
	
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
		{
			activeChar.sendPacket(new ExVariationCancelResult(0));
			return;
		}
		if (targetItem.getOwnerId() != activeChar.getObjectId())
		{
			Util.handleIllegalPlayerAction(getClient().getActiveChar(), "Warning!! Character " + getClient().getActiveChar().getName() + " of account " + getClient().getActiveChar().getAccountName() + " tryied to augment item that doesn't own.", Config.DEFAULT_PUNISH);
			return;
		}
		// cannot remove augmentation from a not augmented item
		if (!targetItem.isAugmented())
		{
			activeChar.sendPacket(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			activeChar.sendPacket(new ExVariationCancelResult(0));
			return;
		}
		
		// get the price
		int price = 0;
		switch (targetItem.getItem().getCrystalType())
		{
			case C:
				if (targetItem.getCrystalCount() < 1720)
				{
					price = 95000;
				}
				else if (targetItem.getCrystalCount() < 2452)
				{
					price = 150000;
				}
				else
				{
					price = 210000;
				}
				break;
			case B:
				if (targetItem.getCrystalCount() < 1746)
				{
					price = 240000;
				}
				else
				{
					price = 270000;
				}
				break;
			case A:
				if (targetItem.getCrystalCount() < 2160)
				{
					price = 330000;
				}
				else if (targetItem.getCrystalCount() < 2824)
				{
					price = 390000;
				}
				else
				{
					price = 420000;
				}
				break;
			case S:
				price = 480000;
				break;
			case S80:
			case S84:
				price = 920000;
				break;
			// any other item type is not augmentable
			default:
				activeChar.sendPacket(new ExVariationCancelResult(0));
				return;
		}
		
		// try to reduce the players adena
		if (!activeChar.reduceAdena("RequestRefineCancel", price, null, true))
		{
			activeChar.sendPacket(new ExVariationCancelResult(0));
			activeChar.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}
		
		// unequip item
		if (targetItem.isEquipped())
		{
			activeChar.disarmWeapons();
		}
		
		// remove the augmentation
		targetItem.removeAugmentation();
		
		// send ExVariationCancelResult
		activeChar.sendPacket(new ExVariationCancelResult(1));
		
		// send inventory update
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		activeChar.sendPacket(iu);
	}
	
	@Override
	public String getType()
	{
		return _C__D0_43_REQUESTREFINECANCEL;
	}
}
