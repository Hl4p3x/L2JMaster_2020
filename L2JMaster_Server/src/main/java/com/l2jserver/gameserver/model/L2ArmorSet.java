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
package com.l2jserver.gameserver.model;

import java.util.ArrayList;
import java.util.List;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.holders.SkillHolder;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;

/**
 * @author Luno
 */
public final class L2ArmorSet
{
	private int _chestId;
	private final List<Integer> _legs;
	private final List<Integer> _head;
	private final List<Integer> _gloves;
	private final List<Integer> _feet;
	private final List<Integer> _shield;
	
	private final List<SkillHolder> _skills;
	private final List<SkillHolder> _shieldSkills;
	private final List<SkillHolder> _enchant6Skill;
	
	private int _con;
	private int _dex;
	private int _str;
	private int _men;
	private int _wit;
	private int _int;
	
	public L2ArmorSet()
	{
		_legs = new ArrayList<>();
		_head = new ArrayList<>();
		_gloves = new ArrayList<>();
		_feet = new ArrayList<>();
		_shield = new ArrayList<>();
		
		_skills = new ArrayList<>();
		_shieldSkills = new ArrayList<>();
		_enchant6Skill = new ArrayList<>();
	}
	
	public void addChest(int id)
	{
		_chestId = id;
	}
	
	public void addLegs(int id)
	{
		_legs.add(id);
	}
	
	public void addHead(int id)
	{
		_head.add(id);
	}
	
	public void addGloves(int id)
	{
		_gloves.add(id);
	}
	
	public void addFeet(int id)
	{
		_feet.add(id);
	}
	
	public void addShield(int id)
	{
		_shield.add(id);
	}
	
	public void addSkill(SkillHolder holder)
	{
		_skills.add(holder);
	}
	
	public void addShieldSkill(SkillHolder holder)
	{
		_shieldSkills.add(holder);
	}
	
	public void addEnchant6Skill(SkillHolder holder)
	{
		_enchant6Skill.add(holder);
	}
	
	public void addCon(int val)
	{
		_con = val;
	}
	
	public void addDex(int val)
	{
		_dex = val;
	}
	
	public void addStr(int val)
	{
		_str = val;
	}
	
	public void addMen(int val)
	{
		_men = val;
	}
	
	public void addWit(int val)
	{
		_wit = val;
	}
	
	public void addInt(int val)
	{
		_int = val;
	}
	
	/**
	 * Checks if player have equipped all items from set (not checking shield)
	 * @param player whose inventory is being checked
	 * @return True if player equips whole set
	 */
	public boolean containAll(L2PcInstance player)
	{
		Inventory inv = player.getInventory();
		
		L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;
		
		if (legsItem != null)
		{
			legs = legsItem.getId();
		}
		if (headItem != null)
		{
			head = headItem.getId();
		}
		if (glovesItem != null)
		{
			gloves = glovesItem.getId();
		}
		if (feetItem != null)
		{
			feet = feetItem.getId();
		}
		
		return containAll(_chestId, legs, head, gloves, feet);
	}
	
	public boolean containAll(int chest, int legs, int head, int gloves, int feet)
	{
		if ((_chestId != 0) && (_chestId != chest))
		{
			return false;
		}
		if (!_legs.isEmpty() && !_legs.contains(legs))
		{
			return false;
		}
		if (!_head.isEmpty() && !_head.contains(head))
		{
			return false;
		}
		if (!_gloves.isEmpty() && !_gloves.contains(gloves))
		{
			return false;
		}
		if (!_feet.isEmpty() && !_feet.contains(feet))
		{
			return false;
		}
		
		return true;
	}
	
	public boolean containItem(int slot, int itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				return _chestId == itemId;
			case Inventory.PAPERDOLL_LEGS:
				return _legs.contains(itemId);
			case Inventory.PAPERDOLL_HEAD:
				return _head.contains(itemId);
			case Inventory.PAPERDOLL_GLOVES:
				return _gloves.contains(itemId);
			case Inventory.PAPERDOLL_FEET:
				return _feet.contains(itemId);
			default:
				return false;
		}
	}
	
	public int getChestId()
	{
		return _chestId;
	}
	
	public List<SkillHolder> getSkills()
	{
		return _skills;
	}
	
	public boolean containShield(L2PcInstance player)
	{
		Inventory inv = player.getInventory();
		
		L2ItemInstance shieldItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		return ((shieldItem != null) && _shield.contains(Integer.valueOf(shieldItem.getId())));
	}
	
	public boolean containShield(int shield_id)
	{
		if (_shield.isEmpty())
		{
			return false;
		}
		
		return _shield.contains(Integer.valueOf(shield_id));
	}
	
	public List<SkillHolder> getShieldSkillId()
	{
		return _shieldSkills;
	}
	
	public List<SkillHolder> getEnchant6skillId()
	{
		return _enchant6Skill;
	}
	
	/**
	 * @param player
	 * @return true if all parts of set are enchanted to +6 or more
	 */
	public boolean isEnchanted6(L2PcInstance player)
	{
		// Player don't have full set
		if (!containAll(player))
		{
			return false;
		}
		
		Inventory inv = player.getInventory();
		
		L2ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		if ((chestItem == null) || (chestItem.getEnchantLevel() < 6))
		{
			return false;
		}
		if (!_legs.isEmpty() && ((legsItem == null) || (legsItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		if (!_gloves.isEmpty() && ((glovesItem == null) || (glovesItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		if (!_head.isEmpty() && ((headItem == null) || (headItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		if (!_feet.isEmpty() && ((feetItem == null) || (feetItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		
		return true;
	}
	
	public int getCON()
	{
		return _con;
	}
	
	public int getDEX()
	{
		return _dex;
	}
	
	public int getSTR()
	{
		return _str;
	}
	
	public int getMEN()
	{
		return _men;
	}
	
	public int getWIT()
	{
		return _wit;
	}
	
	public int getINT()
	{
		return _int;
	}
}
