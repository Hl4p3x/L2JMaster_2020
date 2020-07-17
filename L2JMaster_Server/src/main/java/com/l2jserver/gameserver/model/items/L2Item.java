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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.Elementals;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.events.ListenersContainer;
import com.l2jserver.gameserver.model.holders.SkillHolder;
import com.l2jserver.gameserver.model.interfaces.IIdentifiable;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.items.type.ActionType;
import com.l2jserver.gameserver.model.items.type.CrystalType;
import com.l2jserver.gameserver.model.items.type.EtcItemType;
import com.l2jserver.gameserver.model.items.type.ItemType;
import com.l2jserver.gameserver.model.items.type.ItemType1;
import com.l2jserver.gameserver.model.items.type.ItemType2;
import com.l2jserver.gameserver.model.items.type.MaterialType;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.model.stats.functions.AbstractFunction;
import com.l2jserver.gameserver.model.stats.functions.FuncTemplate;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.util.StringUtil;

/**
 * This class contains all informations concerning the item (weapon, armor, etc).<BR>
 * Mother class of :
 * <ul>
 * <li>L2Armor</li>
 * <li>L2EtcItem</li>
 * <li>L2Weapon</li>
 * </ul>
 */
public abstract class L2Item extends ListenersContainer implements IIdentifiable
{
	protected static final Logger _log = Logger.getLogger(L2Item.class.getName());
	
	public static final int SLOT_NONE = 0x0000;
	public static final int SLOT_UNDERWEAR = 0x0001;
	public static final int SLOT_R_EAR = 0x0002;
	public static final int SLOT_L_EAR = 0x0004;
	public static final int SLOT_LR_EAR = 0x00006;
	public static final int SLOT_NECK = 0x0008;
	public static final int SLOT_R_FINGER = 0x0010;
	public static final int SLOT_L_FINGER = 0x0020;
	public static final int SLOT_LR_FINGER = 0x0030;
	public static final int SLOT_HEAD = 0x0040;
	public static final int SLOT_R_HAND = 0x0080;
	public static final int SLOT_L_HAND = 0x0100;
	public static final int SLOT_GLOVES = 0x0200;
	public static final int SLOT_CHEST = 0x0400;
	public static final int SLOT_LEGS = 0x0800;
	public static final int SLOT_FEET = 0x1000;
	public static final int SLOT_BACK = 0x2000;
	public static final int SLOT_LR_HAND = 0x4000;
	public static final int SLOT_FULL_ARMOR = 0x8000;
	public static final int SLOT_HAIR = 0x010000;
	public static final int SLOT_ALLDRESS = 0x020000;
	public static final int SLOT_HAIR2 = 0x040000;
	public static final int SLOT_HAIRALL = 0x080000;
	public static final int SLOT_R_BRACELET = 0x100000;
	public static final int SLOT_L_BRACELET = 0x200000;
	public static final int SLOT_DECO = 0x400000;
	public static final int SLOT_BELT = 0x10000000;
	public static final int SLOT_WOLF = -100;
	public static final int SLOT_HATCHLING = -101;
	public static final int SLOT_STRIDER = -102;
	public static final int SLOT_BABYPET = -103;
	public static final int SLOT_GREATWOLF = -104;
	
	public static final int SLOT_MULTI_ALLWEAPON = SLOT_LR_HAND | SLOT_R_HAND;
	
	private final int _itemId;
	private final int _displayId;
	private final String _name;
	private final String _icon;
	private final int _weight;
	private final boolean _stackable;
	private final MaterialType _materialType;
	private final CrystalType _crystalType;
	private final int _equipReuseDelay;
	private final int _duration;
	private final int _time;
	private final int _autoDestroyTime;
	private final int _bodyPart;
	private final int _referencePrice;
	private final int _crystalCount;
	private final boolean _sellable;
	private final boolean _dropable;
	private final boolean _destroyable;
	private final boolean _tradeable;
	private final boolean _depositable;
	private final int _enchantable;
	private final boolean _elementable;
	private final boolean _questItem;
	private final boolean _freightable;
	private final boolean _allow_self_resurrection;
	private final boolean _is_oly_restricted;
	private final boolean _for_npc;
	private final boolean _common;
	private final boolean _heroItem;
	private final boolean _pvpItem;
	private final boolean _immediate_effect;
	private final boolean _ex_immediate_effect;
	private final int _defaultEnchantLevel;
	private final ActionType _defaultAction;
	
	protected ItemType1 _type1; // needed for item list (inventory)
	protected ItemType2 _type2; // different lists for armor, weapon, etc
	protected Elementals[] _elementals = null;
	protected List<FuncTemplate> _funcTemplates;
	protected List<Condition> _preConditions;
	private SkillHolder[] _skillHolder;
	private SkillHolder _unequipSkill = null;
	
	private final int _useSkillDisTime;
	private final int _reuseDelay;
	private final int _sharedReuseGroup;
	
	/**
	 * Constructor of the L2Item that fill class variables.<BR>
	 * <BR>
	 * @param set : StatsSet corresponding to a set of couples (key,value) for description of the item
	 */
	protected L2Item(StatsSet set)
	{
		_itemId = set.getInt("item_id");
		_displayId = set.getInt("displayId", _itemId);
		_name = set.getString("name");
		_icon = set.getString("icon", null);
		_weight = set.getInt("weight", 0);
		_materialType = set.getEnum("material", MaterialType.class, MaterialType.STEEL);
		_equipReuseDelay = set.getInt("equip_reuse_delay", 0) * 1000;
		_duration = set.getInt("duration", -1);
		_time = set.getInt("time", -1);
		_autoDestroyTime = set.getInt("auto_destroy_time", -1) * 1000;
		_bodyPart = ItemTable.SLOTS.get(set.getString("bodypart", "none"));
		_referencePrice = set.getInt("price", 0);
		_crystalType = set.getEnum("crystal_type", CrystalType.class, CrystalType.NONE);
		_crystalCount = set.getInt("crystal_count", 0);
		
		_stackable = set.getBoolean("is_stackable", false);
		_sellable = set.getBoolean("is_sellable", true);
		_dropable = set.getBoolean("is_dropable", true);
		_destroyable = set.getBoolean("is_destroyable", true);
		_tradeable = set.getBoolean("is_tradable", true);
		_depositable = set.getBoolean("is_depositable", true);
		_elementable = set.getBoolean("element_enabled", false);
		_enchantable = set.getInt("enchant_enabled", 0);
		_questItem = set.getBoolean("is_questitem", false);
		_freightable = set.getBoolean("is_freightable", false);
		_allow_self_resurrection = set.getBoolean("allow_self_resurrection", false);
		_is_oly_restricted = set.getBoolean("is_oly_restricted", false);
		_for_npc = set.getBoolean("for_npc", false);
		
		_immediate_effect = set.getBoolean("immediate_effect", false);
		_ex_immediate_effect = set.getBoolean("ex_immediate_effect", false);
		
		_defaultAction = set.getEnum("default_action", ActionType.class, ActionType.NONE);
		_useSkillDisTime = set.getInt("useSkillDisTime", 0);
		_defaultEnchantLevel = set.getInt("enchanted", 0);
		_reuseDelay = set.getInt("reuse_delay", 0);
		_sharedReuseGroup = set.getInt("shared_reuse_group", 0);
		
		String skills = set.getString("item_skill", null);
		if (skills != null)
		{
			String[] skillsSplit = skills.split(";");
			_skillHolder = new SkillHolder[skillsSplit.length];
			int used = 0;
			
			for (String element : skillsSplit)
			{
				try
				{
					String[] skillSplit = element.split("-");
					int id = Integer.parseInt(skillSplit[0]);
					int level = Integer.parseInt(skillSplit[1]);
					
					if (id == 0)
					{
						_log.info(StringUtil.concat("Ignoring item_skill(", element, ") for item ", toString(), ". Skill id is 0!"));
						continue;
					}
					
					if (level == 0)
					{
						_log.info(StringUtil.concat("Ignoring item_skill(", element, ") for item ", toString(), ". Skill level is 0!"));
						continue;
					}
					
					_skillHolder[used] = new SkillHolder(id, level);
					++used;
				}
				catch (Exception e)
				{
					_log.warning(StringUtil.concat("Failed to parse item_skill(", element, ") for item ", toString(), "! Format: SkillId0-SkillLevel0[;SkillIdN-SkillLevelN]"));
				}
			}
			
			// this is only loading? just don't leave a null or use a collection?
			if (used != _skillHolder.length)
			{
				SkillHolder[] skillHolder = new SkillHolder[used];
				System.arraycopy(_skillHolder, 0, skillHolder, 0, used);
				_skillHolder = skillHolder;
			}
		}
		
		skills = set.getString("unequip_skill", null);
		if (skills != null)
		{
			String[] info = skills.split("-");
			if ((info != null) && (info.length == 2))
			{
				int id = 0;
				int level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, don't add new skill
					_log.info(StringUtil.concat("Couldnt parse ", skills, " in weapon unequip skills! item ", toString()));
				}
				if ((id > 0) && (level > 0))
				{
					_unequipSkill = new SkillHolder(id, level);
				}
			}
		}
		
		_common = ((_itemId >= 11605) && (_itemId <= 12361));
		_heroItem = ((_itemId >= 6611) && (_itemId <= 6621)) || ((_itemId >= 9388) && (_itemId <= 9390)) || (_itemId == 6842);
		_pvpItem = ((_itemId >= 10667) && (_itemId <= 10835)) || ((_itemId >= 12852) && (_itemId <= 12977)) || ((_itemId >= 14363) && (_itemId <= 14525)) || (_itemId == 14528) || (_itemId == 14529) || (_itemId == 14558) || ((_itemId >= 15913) && (_itemId <= 16024))
			|| ((_itemId >= 16134) && (_itemId <= 16147)) || (_itemId == 16149) || (_itemId == 16151) || (_itemId == 16153) || (_itemId == 16155) || (_itemId == 16157) || (_itemId == 16159) || ((_itemId >= 16168) && (_itemId <= 16176)) || ((_itemId >= 16179) && (_itemId <= 16220));
	}
	
	/**
	 * Returns the itemType.
	 * @return Enum
	 */
	public abstract ItemType getItemType();
	
	/**
	 * Verifies if the item is a magic weapon.
	 * @return {@code true} if the weapon is magic, {@code false} otherwise
	 */
	public boolean isMagicWeapon()
	{
		return false;
	}
	
	/**
	 * @return the _equipReuseDelay
	 */
	public int getEquipReuseDelay()
	{
		return _equipReuseDelay;
	}
	
	/**
	 * Returns the duration of the item
	 * @return int
	 */
	public final int getDuration()
	{
		return _duration;
	}
	
	/**
	 * Returns the time of the item
	 * @return int
	 */
	public final int getTime()
	{
		return _time;
	}
	
	/**
	 * @return the auto destroy time of the item in seconds: 0 or less - default
	 */
	public final int getAutoDestroyTime()
	{
		return _autoDestroyTime;
	}
	
	/**
	 * Returns the ID of the item
	 * @return int
	 */
	@Override
	public final int getId()
	{
		return _itemId;
	}
	
	/**
	 * Returns the ID of the item
	 * @return int
	 */
	public final int getDisplayId()
	{
		return _displayId;
	}
	
	public abstract int getItemMask();
	
	/**
	 * Return the type of material of the item
	 * @return MaterialType
	 */
	public final MaterialType getMaterialType()
	{
		return _materialType;
	}
	
	/**
	 * Returns the type 2 of the item
	 * @return ItemType2
	 */
	public final ItemType2 getType2()
	{
		return _type2;
	}
	
	/**
	 * Returns the weight of the item
	 * @return int
	 */
	public final int getWeight()
	{
		return _weight;
	}
	
	/**
	 * Returns if the item is crystallizable
	 * @return boolean
	 */
	public final boolean isCrystallizable()
	{
		return (_crystalType != CrystalType.NONE) && (_crystalCount > 0);
	}
	
	/**
	 * Return the type of crystal if item is crystallizable
	 * @return CrystalType
	 */
	public final CrystalType getCrystalType()
	{
		return _crystalType;
	}
	
	/**
	 * Return the ID of crystal if item is crystallizable
	 * @return int
	 */
	public final int getCrystalItemId()
	{
		return _crystalType.getCrystalId();
	}
	
	/**
	 * Returns the grade of the item.<BR>
	 * <BR>
	 * <U><I>Concept :</I></U><BR>
	 * In fact, this function returns the type of crystal of the item.
	 * @return CrystalType
	 */
	public final CrystalType getItemGrade()
	{
		return getCrystalType();
	}
	
	/**
	 * For grades S80 and S84 return S
	 * @return the grade of the item.
	 */
	public final CrystalType getItemGradeSPlus()
	{
		switch (getItemGrade())
		{
			case S80:
			case S84:
				return CrystalType.S;
			default:
				return getItemGrade();
		}
	}
	
	/**
	 * @return the quantity of crystals for crystallization.
	 */
	public final int getCrystalCount()
	{
		return _crystalCount;
	}
	
	/**
	 * @param enchantLevel
	 * @return the quantity of crystals for crystallization on specific enchant level
	 */
	public final int getCrystalCount(int enchantLevel)
	{
		if (enchantLevel > 3)
		{
			switch (_type2)
			{
				case SHIELD_ARMOR:
				case ACCESSORY:
					return _crystalCount + (getCrystalType().getCrystalEnchantBonusArmor() * ((3 * enchantLevel) - 6));
				case WEAPON:
					return _crystalCount + (getCrystalType().getCrystalEnchantBonusWeapon() * ((2 * enchantLevel) - 3));
				default:
					return _crystalCount;
			}
		}
		else if (enchantLevel > 0)
		{
			switch (_type2)
			{
				case SHIELD_ARMOR:
				case ACCESSORY:
					return _crystalCount + (getCrystalType().getCrystalEnchantBonusArmor() * enchantLevel);
				case WEAPON:
					return _crystalCount + (getCrystalType().getCrystalEnchantBonusWeapon() * enchantLevel);
				default:
					return _crystalCount;
			}
		}
		else
		{
			return _crystalCount;
		}
	}
	
	/**
	 * @return the name of the item.
	 */
	public final String getName()
	{
		return _name;
	}
	
	/**
	 * @return the base elemental of the item.
	 */
	public final Elementals[] getElementals()
	{
		return _elementals;
	}
	
	public Elementals getElemental(byte attribute)
	{
		for (Elementals elm : _elementals)
		{
			if (elm.getElement() == attribute)
			{
				return elm;
			}
		}
		return null;
	}
	
	/**
	 * Sets the base elemental of the item.
	 * @param element the element to set.
	 */
	public void setElementals(Elementals element)
	{
		if (_elementals == null)
		{
			_elementals = new Elementals[1];
			_elementals[0] = element;
		}
		else
		{
			Elementals elm = getElemental(element.getElement());
			if (elm != null)
			{
				elm.setValue(element.getValue());
			}
			else
			{
				elm = element;
				Elementals[] array = new Elementals[_elementals.length + 1];
				System.arraycopy(_elementals, 0, array, 0, _elementals.length);
				array[_elementals.length] = elm;
				_elementals = array;
			}
		}
	}
	
	/**
	 * @return the part of the body used with the item.
	 */
	public final int getBodyPart()
	{
		return _bodyPart;
	}
	
	/**
	 * @return the type 1 of the item.
	 */
	public final ItemType1 getType1()
	{
		return _type1;
	}
	
	/**
	 * @return {@code true} if the item is stackable, {@code false} otherwise.
	 */
	public final boolean isStackable()
	{
		return _stackable;
	}
	
	/**
	 * @return {@code true} if the item can be equipped, {@code false} otherwise.
	 */
	public boolean isEquipable()
	{
		return (getBodyPart() != 0) && !(getItemType() instanceof EtcItemType);
	}
	
	/**
	 * @return the price of reference of the item.
	 */
	public final int getReferencePrice()
	{
		return _referencePrice;
	}
	
	/**
	 * @return {@code true} if the item can be sold, {@code false} otherwise.
	 */
	public final boolean isSellable()
	{
		return _sellable;
	}
	
	/**
	 * @return {@code true} if the item can be dropped, {@code false} otherwise.
	 */
	public final boolean isDropable()
	{
		return _dropable;
	}
	
	/**
	 * @return {@code true} if the item can be destroyed, {@code false} otherwise.
	 */
	public final boolean isDestroyable()
	{
		return _destroyable;
	}
	
	/**
	 * @return {@code true} if the item can be traded, {@code false} otherwise.
	 */
	public final boolean isTradeable()
	{
		return _tradeable;
	}
	
	/**
	 * @return {@code true} if the item can be put into warehouse, {@code false} otherwise.
	 */
	public final boolean isDepositable()
	{
		return _depositable;
	}
	
	/**
	 * This method also check the enchant blacklist.
	 * @return {@code true} if the item can be enchanted, {@code false} otherwise.
	 */
	public final int isEnchantable()
	{
		return Arrays.binarySearch(Config.ENCHANT_BLACKLIST, getId()) < 0 ? _enchantable : 0;
	}
	
	/**
	 * @return {@code true} if the item can be elemented, {@code false} otherwise.
	 */
	public final boolean isElementable()
	{
		return _elementable;
	}
	
	/**
	 * Returns if item is common
	 * @return boolean
	 */
	public final boolean isCommon()
	{
		return _common;
	}
	
	/**
	 * Returns if item is hero-only
	 * @return
	 */
	public final boolean isHeroItem()
	{
		return _heroItem;
	}
	
	/**
	 * Returns if item is pvp
	 * @return
	 */
	public final boolean isPvpItem()
	{
		return _pvpItem;
	}
	
	public boolean isPotion()
	{
		return (getItemType() == EtcItemType.POTION);
	}
	
	public boolean isElixir()
	{
		return (getItemType() == EtcItemType.ELIXIR);
	}
	
	public boolean isScroll()
	{
		return (getItemType() == EtcItemType.SCROLL);
	}
	
	/**
	 * Get the functions used by this item.
	 * @param item : L2ItemInstance pointing out the item
	 * @param player : L2Character pointing out the player
	 * @return the list of functions
	 */
	public final List<AbstractFunction> getStatFuncs(L2ItemInstance item, L2Character player)
	{
		if ((_funcTemplates == null) || _funcTemplates.isEmpty())
		{
			return Collections.<AbstractFunction> emptyList();
		}
		
		final List<AbstractFunction> funcs = new ArrayList<>(_funcTemplates.size());
		for (FuncTemplate t : _funcTemplates)
		{
			AbstractFunction f = t.getFunc(player, player, item, item);
			if (f != null)
			{
				funcs.add(f);
			}
		}
		return funcs;
	}
	
	/**
	 * Add the FuncTemplate f to the list of functions used with the item
	 * @param f : FuncTemplate to add
	 */
	public void attach(FuncTemplate f)
	{
		switch (f.getStat())
		{
			case FIRE_RES:
			case FIRE_POWER:
				setElementals(new Elementals(Elementals.FIRE, (int) f.getValue()));
				break;
			case WATER_RES:
			case WATER_POWER:
				setElementals(new Elementals(Elementals.WATER, (int) f.getValue()));
				break;
			case WIND_RES:
			case WIND_POWER:
				setElementals(new Elementals(Elementals.WIND, (int) f.getValue()));
				break;
			case EARTH_RES:
			case EARTH_POWER:
				setElementals(new Elementals(Elementals.EARTH, (int) f.getValue()));
				break;
			case HOLY_RES:
			case HOLY_POWER:
				setElementals(new Elementals(Elementals.HOLY, (int) f.getValue()));
				break;
			case DARK_RES:
			case DARK_POWER:
				setElementals(new Elementals(Elementals.DARK, (int) f.getValue()));
				break;
		}
		
		if (_funcTemplates == null)
		{
			_funcTemplates = new ArrayList<>(1);
		}
		_funcTemplates.add(f);
	}
	
	public final void attach(Condition c)
	{
		if (_preConditions == null)
		{
			_preConditions = new ArrayList<>(1);
		}
		if (!_preConditions.contains(c))
		{
			_preConditions.add(c);
		}
	}
	
	public boolean hasSkills()
	{
		return _skillHolder != null;
	}
	
	/**
	 * Method to retrieve skills linked to this item armor and weapon: passive skills etcitem: skills used on item use <-- ???
	 * @return Skills linked to this item as SkillHolder[]
	 */
	public final SkillHolder[] getSkills()
	{
		return _skillHolder;
	}
	
	/**
	 * @return skill that activates, when player unequip this weapon or armor
	 */
	public final Skill getUnequipSkill()
	{
		return _unequipSkill == null ? null : _unequipSkill.getSkill();
	}
	
	public boolean checkCondition(L2Character activeChar, L2Object object, boolean sendMessage)
	{
		if (activeChar.canOverrideCond(PcCondOverride.ITEM_CONDITIONS) && !Config.GM_ITEM_RESTRICTION)
		{
			return true;
		}
		
		// Don't allow hero equipment and restricted items during Olympiad
		if ((isOlyRestrictedItem() || isHeroItem()) && ((activeChar instanceof L2PcInstance) && activeChar.getActingPlayer().isInOlympiadMode()))
		{
			if (isEquipable())
			{
				activeChar.sendPacket(SystemMessageId.THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT);
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			}
			return false;
		}
		
		if (!isConditionAttached())
		{
			return true;
		}
		
		final L2Character target = (object instanceof L2Character) ? (L2Character) object : null;
		for (Condition preCondition : _preConditions)
		{
			if (preCondition == null)
			{
				continue;
			}
			
			if (!preCondition.test(activeChar, target, null, null))
			{
				if (activeChar instanceof L2Summon)
				{
					activeChar.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
					return false;
				}
				
				if (sendMessage)
				{
					String msg = preCondition.getMessage();
					int msgId = preCondition.getMessageId();
					if (msg != null)
					{
						activeChar.sendMessage(msg);
					}
					else if (msgId != 0)
					{
						SystemMessage sm = SystemMessage.getSystemMessage(msgId);
						if (preCondition.isAddName())
						{
							sm.addItemName(_itemId);
						}
						activeChar.sendPacket(sm);
					}
				}
				return false;
			}
		}
		return true;
	}
	
	public boolean isConditionAttached()
	{
		return (_preConditions != null) && !_preConditions.isEmpty();
	}
	
	public boolean isQuestItem()
	{
		return _questItem;
	}
	
	public boolean isFreightable()
	{
		return _freightable;
	}
	
	public boolean isAllowSelfResurrection()
	{
		return _allow_self_resurrection;
	}
	
	public boolean isOlyRestrictedItem()
	{
		return _is_oly_restricted || Config.LIST_OLY_RESTRICTED_ITEMS.contains(_itemId);
	}
	
	public boolean isForNpc()
	{
		return _for_npc;
	}
	
	/**
	 * Returns the name of the item followed by the item ID.
	 * @return the name and the ID of the item
	 */
	@Override
	public String toString()
	{
		return _name + "(" + _itemId + ")";
	}
	
	/**
	 * Verifies if the item has effects immediately.<br>
	 * <i>Used for herbs mostly.</i>
	 * @return {@code true} if the item applies effects immediately, {@code false} otherwise
	 */
	public boolean hasExImmediateEffect()
	{
		return _ex_immediate_effect;
	}
	
	/**
	 * Verifies if the item has effects immediately.
	 * @return {@code true} if the item applies effects immediately, {@code false} otherwise
	 */
	public boolean hasImmediateEffect()
	{
		return _immediate_effect;
	}
	
	/**
	 * @return the _default_action
	 */
	public ActionType getDefaultAction()
	{
		return _defaultAction;
	}
	
	public int useSkillDisTime()
	{
		return _useSkillDisTime;
	}
	
	/**
	 * Gets the item reuse delay time in seconds.
	 * @return the reuse delay time
	 */
	public int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	/**
	 * Gets the shared reuse group.<br>
	 * Items with the same reuse group will render reuse delay upon those items when used.
	 * @return the shared reuse group
	 */
	public int getSharedReuseGroup()
	{
		return _sharedReuseGroup;
	}
	
	/**
	 * Usable in HTML windows.
	 * @return the icon link in client files
	 */
	public String getIcon()
	{
		return _icon;
	}
	
	public int getDefaultEnchantLevel()
	{
		return _defaultEnchantLevel;
	}
	
	public boolean isPetItem()
	{
		return getItemType() == EtcItemType.PET_COLLAR;
	}
	
	public Skill getEnchant4Skill()
	{
		return null;
	}
}