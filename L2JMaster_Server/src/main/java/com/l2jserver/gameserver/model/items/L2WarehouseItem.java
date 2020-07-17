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
package com.l2jserver.gameserver.model.items;

import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.items.type.CrystalType;
import com.l2jserver.gameserver.model.items.type.ItemType;
import com.l2jserver.gameserver.model.items.type.ItemType1;
import com.l2jserver.gameserver.model.items.type.ItemType2;

/**
 * This class contains L2ItemInstance<BR>
 * Use to sort L2ItemInstance of :
 * <ul>
 * <li>L2Armor</li>
 * <li>L2EtcItem</li>
 * <li>L2Weapon</li>
 * </ul>
 * @version $Revision: 1.7.2.2.2.5 $ $Date: 2005/04/06 18:25:18 $
 */
public class L2WarehouseItem
{
	private final L2Item _item;
	private final int _object;
	private final long _count;
	private final int _owner;
	private final int _locationSlot;
	private final int _enchant;
	private final CrystalType _grade;
	private boolean _isAugmented;
	private int _augmentationId;
	private final int _customType1;
	private final int _customType2;
	private final int _mana;
	
	private int _elemAtkType = -2;
	private int _elemAtkPower = 0;
	
	private final int[] _elemDefAttr =
	{
		0,
		0,
		0,
		0,
		0,
		0
	};
	
	private final int[] _enchantOptions;
	
	private final int _time;
	
	public L2WarehouseItem(L2ItemInstance item)
	{
		_item = item.getItem();
		_object = item.getObjectId();
		_count = item.getCount();
		_owner = item.getOwnerId();
		_locationSlot = item.getLocationSlot();
		_enchant = item.getEnchantLevel();
		_customType1 = item.getCustomType1();
		_customType2 = item.getCustomType2();
		_grade = item.getItem().getItemGrade();
		if (item.isAugmented())
		{
			_isAugmented = true;
			_augmentationId = item.getAugmentation().getAugmentationId();
		}
		else
		{
			_isAugmented = false;
		}
		_mana = item.getMana();
		_time = item.isTimeLimitedItem() ? (int) (item.getRemainingTime() / 1000) : -1;
		
		_elemAtkType = item.getAttackElementType();
		_elemAtkPower = item.getAttackElementPower();
		for (byte i = 0; i < 6; i++)
		{
			_elemDefAttr[i] = item.getElementDefAttr(i);
		}
		_enchantOptions = item.getEnchantOptions();
	}
	
	/**
	 * @return the item.
	 */
	public L2Item getItem()
	{
		return _item;
	}
	
	/**
	 * @return the unique objectId.
	 */
	public final int getObjectId()
	{
		return _object;
	}
	
	/**
	 * @return the owner.
	 */
	public final int getOwnerId()
	{
		return _owner;
	}
	
	/**
	 * @return the location slot.
	 */
	public final int getLocationSlot()
	{
		return _locationSlot;
	}
	
	/**
	 * @return the count.
	 */
	public final long getCount()
	{
		return _count;
	}
	
	/**
	 * @return the first type.
	 */
	public final ItemType1 getType1()
	{
		return _item.getType1();
	}
	
	/**
	 * @return the second type.
	 */
	public final ItemType2 getType2()
	{
		return _item.getType2();
	}
	
	/**
	 * @return the second type.
	 */
	public final ItemType getItemType()
	{
		return _item.getItemType();
	}
	
	/**
	 * @return the ItemId.
	 */
	public final int getItemId()
	{
		return _item.getId();
	}
	
	/**
	 * @return the part of body used with this item.
	 */
	public final int getBodyPart()
	{
		return _item.getBodyPart();
	}
	
	/**
	 * @return the enchant level.
	 */
	public final int getEnchantLevel()
	{
		return _enchant;
	}
	
	/**
	 * @return the item grade
	 */
	public final CrystalType getItemGrade()
	{
		return _grade;
	}
	
	/**
	 * @return {@code true} if the item is a weapon, {@code false} otherwise.
	 */
	public final boolean isWeapon()
	{
		return (_item instanceof L2Weapon);
	}
	
	/**
	 * @return {@code true} if the item is an armor, {@code false} otherwise.
	 */
	public final boolean isArmor()
	{
		return (_item instanceof L2Armor);
	}
	
	/**
	 * @return {@code true} if the item is an etc item, {@code false} otherwise.
	 */
	public final boolean isEtcItem()
	{
		return (_item instanceof L2EtcItem);
	}
	
	/**
	 * @return the name of the item
	 */
	public String getItemName()
	{
		return _item.getName();
	}
	
	/**
	 * @return {@code true} if the item is augmented, {@code false} otherwise.
	 */
	public boolean isAugmented()
	{
		return _isAugmented;
	}
	
	/**
	 * @return the augmentation If.
	 */
	public int getAugmentationId()
	{
		return _augmentationId;
	}
	
	/**
	 * @return the name of the item
	 */
	public String getName()
	{
		return _item.getName();
	}
	
	public final int getCustomType1()
	{
		return _customType1;
	}
	
	public final int getCustomType2()
	{
		return _customType2;
	}
	
	public final int getMana()
	{
		return _mana;
	}
	
	public int getAttackElementType()
	{
		return _elemAtkType;
	}
	
	public int getAttackElementPower()
	{
		return _elemAtkPower;
	}
	
	public int getElementDefAttr(byte i)
	{
		return _elemDefAttr[i];
	}
	
	public int[] getEnchantOptions()
	{
		return _enchantOptions;
	}
	
	public int getTime()
	{
		return _time;
	}
	
	/**
	 * @return the name of the item
	 */
	@Override
	public String toString()
	{
		return _item.toString();
	}
}
