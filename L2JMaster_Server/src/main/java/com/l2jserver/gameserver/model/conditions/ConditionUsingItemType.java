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
package com.l2jserver.gameserver.model.conditions;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.items.type.ArmorType;
import com.l2jserver.gameserver.model.skills.Skill;

/**
 * The Class ConditionUsingItemType.
 * @author mkizub
 */
public final class ConditionUsingItemType extends Condition
{
	private final boolean _armor;
	private final int _mask;
	
	/**
	 * Instantiates a new condition using item type.
	 * @param mask the mask
	 */
	public ConditionUsingItemType(int mask)
	{
		_mask = mask;
		_armor = (_mask & (ArmorType.MAGIC.mask() | ArmorType.LIGHT.mask() | ArmorType.HEAVY.mask())) != 0;
	}
	
	@Override
	public boolean testImpl(L2Character effector, L2Character effected, Skill skill, L2Item item)
	{
		if (effector == null)
		{
			return false;
		}
		
		if (!effector.isPlayer())
		{
			return _armor ? false : (_mask & effector.getAttackType().mask()) != 0;
		}
		
		final Inventory inv = effector.getInventory();
		// If ConditionUsingItemType is one between Light, Heavy or Magic
		if (_armor)
		{
			// Get the itemMask of the weared chest (if exists)
			L2ItemInstance chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if (chest == null)
			{
				return false;
			}
			int chestMask = chest.getItem().getItemMask();
			
			// If chest armor is different from the condition one return false
			if ((_mask & chestMask) == 0)
			{
				return false;
			}
			
			// So from here, chest armor matches conditions
			
			int chestBodyPart = chest.getItem().getBodyPart();
			// return True if chest armor is a Full Armor
			if (chestBodyPart == L2Item.SLOT_FULL_ARMOR)
			{
				return true;
			}
			// check legs armor
			L2ItemInstance legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			if (legs == null)
			{
				return false;
			}
			int legMask = legs.getItem().getItemMask();
			// return true if legs armor matches too
			return (_mask & legMask) != 0;
		}
		return (_mask & inv.getWearedMask()) != 0;
	}
}
