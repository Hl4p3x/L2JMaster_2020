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
package com.l2jserver.gameserver.model.stats.functions;

import java.util.logging.Logger;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.model.stats.Stats;

/**
 * A Function object is a component of a Calculator created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...).<br>
 * In fact, each calculator is a table of functions object in which each function represents a mathematics function:<br>
 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<br>
 * When the calc method of a calculator is launched, each mathematics function is called according to its priority <B>_order</B>.<br>
 * Indeed, functions with lowest priority order is executed first and functions with the same order are executed in unspecified order.<br>
 * @author Zoey76
 */
public abstract class AbstractFunction
{
	/** Logger. */
	protected static final Logger LOG = Logger.getLogger(AbstractFunction.class.getName());
	/** Statistics, that is affected by this function (See L2Character.CALCULATOR_XXX constants) */
	private final Stats _stat;
	/**
	 * Order of functions calculation.<br>
	 * Functions with lower order are executed first.<br>
	 * Functions with the same order are executed in unspecified order.<br>
	 * Usually add/subtract functions has lowest order,<br>
	 * then bonus/penalty functions (multiply/divide) are applied, then functions that do more complex<br>
	 * calculations (non-linear functions).
	 */
	private final int _order;
	/**
	 * Owner can be an armor, weapon, skill, system event, quest, etc.<br>
	 * Used to remove all functions added by this owner.
	 */
	private final Object _funcOwner;
	/** Function may be disabled by attached condition. */
	private final Condition _applayCond;
	/** The value. */
	private final double _value;
	
	/**
	 * Constructor of Func.
	 * @param stat the stat
	 * @param order the order
	 * @param owner the owner
	 * @param value the value
	 * @param applayCond the apply condition
	 */
	public AbstractFunction(Stats stat, int order, Object owner, double value, Condition applayCond)
	{
		_stat = stat;
		_order = order;
		_funcOwner = owner;
		_value = value;
		_applayCond = applayCond;
	}
	
	/**
	 * Gets the apply condition
	 * @return the apply condition
	 */
	public Condition getApplayCond()
	{
		return _applayCond;
	}
	
	/**
	 * Gets the fuction owner.
	 * @return the function owner
	 */
	public final Object getFuncOwner()
	{
		return _funcOwner;
	}
	
	/**
	 * Gets the function order.
	 * @return the order
	 */
	public final int getOrder()
	{
		return _order;
	}
	
	/**
	 * Gets the stat.
	 * @return the stat
	 */
	public final Stats getStat()
	{
		return _stat;
	}
	
	/**
	 * Gets the value.
	 * @return the value
	 */
	public final double getValue()
	{
		return _value;
	}
	
	/**
	 * Run the mathematics function of the Func.
	 * @param effector the effector
	 * @param effected the effected
	 * @param skill the skill
	 * @param initVal the initial value
	 * @return the calculated value
	 */
	public abstract double calc(L2Character effector, L2Character effected, Skill skill, double initVal);
}
