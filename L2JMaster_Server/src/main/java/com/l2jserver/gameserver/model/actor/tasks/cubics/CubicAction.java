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
package com.l2jserver.gameserver.model.actor.tasks.cubics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2CubicInstance;
import com.l2jserver.gameserver.model.effects.L2EffectType;
import com.l2jserver.gameserver.model.skills.BuffInfo;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jserver.gameserver.taskmanager.AttackStanceTaskManager;
import com.l2jserver.util.Rnd;

/**
 * Cubic action task.
 * @author Zoey76
 */
public final class CubicAction implements Runnable
{
	private static final Logger _log = Logger.getLogger(CubicAction.class.getName());
	private final L2CubicInstance _cubic;
	private final AtomicInteger _currentCount = new AtomicInteger();
	private final int _chance;
	
	public CubicAction(L2CubicInstance cubic, int chance)
	{
		_cubic = cubic;
		_chance = chance;
	}
	
	@Override
	public void run()
	{
		if (_cubic == null)
		{
			return;
		}
		
		try
		{
			if (_cubic.getOwner().isDead() || !_cubic.getOwner().isOnline())
			{
				_cubic.stopAction();
				_cubic.getOwner().getCubics().remove(_cubic.getId());
				_cubic.getOwner().broadcastUserInfo();
				_cubic.cancelDisappear();
				return;
			}
			
			if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_cubic.getOwner()))
			{
				if (_cubic.getOwner().hasSummon())
				{
					if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_cubic.getOwner().getSummon()))
					{
						_cubic.stopAction();
						return;
					}
				}
				else
				{
					_cubic.stopAction();
					return;
				}
			}
			
			// The cubic has already reached its limit and it will stay idle until its duration ends.
			if ((_cubic.getCubicMaxCount() > -1) && (_currentCount.get() >= _cubic.getCubicMaxCount()))
			{
				_cubic.stopAction();
				return;
			}
			
			// Smart Cubic debuff cancel is 100%
			boolean useCubicCure = false;
			if ((_cubic.getId() >= L2CubicInstance.SMART_CUBIC_EVATEMPLAR) && (_cubic.getId() <= L2CubicInstance.SMART_CUBIC_SPECTRALMASTER))
			{
				for (BuffInfo info : _cubic.getOwner().getEffectList().getDebuffs())
				{
					if (!info.getSkill().isIrreplaceableBuff())
					{
						useCubicCure = true;
						info.getEffected().getEffectList().stopSkillEffects(true, info.getSkill());
					}
				}
			}
			
			if (useCubicCure)
			{
				// Smart Cubic debuff cancel is needed, no other skill is used in this activation period
				MagicSkillUse msu = new MagicSkillUse(_cubic.getOwner(), _cubic.getOwner(), L2CubicInstance.SKILL_CUBIC_CURE, 1, 0, 0);
				_cubic.getOwner().broadcastPacket(msu);
				
				// The cubic has done an action, increase the current count
				_currentCount.incrementAndGet();
			}
			else if (Rnd.get(1, 100) < _chance)
			{
				final Skill skill = _cubic.getSkills().get(Rnd.get(_cubic.getSkills().size()));
				if (skill == null)
				{
					return;
				}
				
				if (skill.getId() == L2CubicInstance.SKILL_CUBIC_HEAL)
				{
					// friendly skill, so we look a target in owner's party
					_cubic.cubicTargetForHeal();
				}
				else
				{
					// offensive skill, we look for an enemy target
					_cubic.getCubicTarget();
					if (!L2CubicInstance.isInCubicRange(_cubic.getOwner(), _cubic.getTarget()))
					{
						_cubic.setTarget(null);
					}
				}
				L2Character target = _cubic.getTarget();
				if ((target != null) && !target.isDead())
				{
					if (Config.DEBUG)
					{
						_log.info("L2CubicInstance: Action.run();");
						_log.info("Cubic ID: " + _cubic.getId() + " Target: " + target.getName() + " distance: " + target.calculateDistance(_cubic.getOwner(), true, false));
					}
					
					_cubic.getOwner().broadcastPacket(new MagicSkillUse(_cubic.getOwner(), target, skill.getId(), skill.getLevel(), 0, 0));
					
					L2Character[] targets =
					{
						target
					};
					
					if (skill.isContinuous())
					{
						if (Config.DEBUG)
						{
							_log.info("L2CubicInstance: Action.run() skill " + skill);
						}
						_cubic.useCubicContinuous(skill, targets);
					}
					else
					{
						skill.activateSkill(_cubic, targets);
						if (Config.DEBUG)
						{
							_log.info("L2CubicInstance: Action.run(); other handler");
						}
					}
					
					if (skill.hasEffectType(L2EffectType.MAGICAL_ATTACK))
					{
						if (Config.DEBUG)
						{
							_log.info("L2CubicInstance: Action.run() skill " + skill);
						}
						_cubic.useCubicMdam(skill, targets);
					}
					else if (skill.hasEffectType(L2EffectType.HP_DRAIN))
					{
						if (Config.DEBUG)
						{
							_log.info("L2CubicInstance: Action.run() skill " + skill);
						}
						_cubic.useCubicDrain(skill, targets);
					}
					else if (skill.hasEffectType(L2EffectType.STUN, L2EffectType.ROOT, L2EffectType.PARALYZE))
					{
						if (Config.DEBUG)
						{
							_log.info("L2CubicInstance: Action.run() skill " + skill);
						}
						_cubic.useCubicDisabler(skill, targets);
					}
					else if (skill.hasEffectType(L2EffectType.DMG_OVER_TIME))
					{
						if (Config.DEBUG)
						{
							_log.info("L2CubicInstance: Action.run() skill " + skill);
						}
						_cubic.useCubicContinuous(skill, targets);
					}
					else if (skill.hasEffectType(L2EffectType.AGGRESSION))
					{
						if (Config.DEBUG)
						{
							_log.info("L2CubicInstance: Action.run() skill " + skill);
						}
						_cubic.useCubicDisabler(skill, targets);
					}
					
					// The cubic has done an action, increase the current count
					_currentCount.incrementAndGet();
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
	}
}