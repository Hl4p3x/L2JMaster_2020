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
package com.l2jserver.gameserver.model.base;

import java.util.regex.Matcher;

/**
 * This class will hold the information of the player classes.
 * @author Zoey76
 */
public final class ClassInfo
{
	private final ClassId _classId;
	private final String _className;
	private final ClassId _parentClassId;
	
	/**
	 * Constructor for ClassInfo.
	 * @param classId the class ID
	 * @param className the in game class name
	 * @param parentClassId the parent class for the given {@code classId}
	 */
	public ClassInfo(ClassId classId, String className, ClassId parentClassId)
	{
		_classId = classId;
		_className = className;
		_parentClassId = parentClassId;
	}
	
	/**
	 * @return the class ID
	 */
	public ClassId getClassId()
	{
		return _classId;
	}
	
	/**
	 * @return the hardcoded in-game class name
	 */
	public String getClassName()
	{
		return _className;
	}
	
	/**
	 * @return the class client ID
	 */
	private int getClassClientId()
	{
		int classClientId = _classId.getId();
		if ((classClientId >= 0) && (classClientId <= 57))
		{
			classClientId += 247;
		}
		else if ((classClientId >= 88) && (classClientId <= 118))
		{
			classClientId += 1071;
		}
		else if ((classClientId >= 123) && (classClientId <= 136))
		{
			classClientId += 1438;
		}
		return classClientId;
	}
	
	/**
	 * @return the class client ID formatted to be displayed on a HTML.
	 */
	public String getClientCode()
	{
		return "&$" + getClassClientId() + ";";
	}
	
	/**
	 * @return the escaped class client ID formatted to be displayed on a HTML
	 */
	public String getEscapedClientCode()
	{
		return Matcher.quoteReplacement(getClientCode());
	}
	
	/**
	 * @return the parent class ID
	 */
	public ClassId getParentClassId()
	{
		return _parentClassId;
	}
}