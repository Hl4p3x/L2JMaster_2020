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
package com.l2jserver.gameserver.data.xml.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.util.data.xml.IXmlReader;

/**
 * Holds all skill learn data for all NPCs.
 * @author xban1x
 */
public final class SkillLearnData implements IXmlReader
{
	private final Map<Integer, List<ClassId>> _skillLearn = new HashMap<>();
	
	protected SkillLearnData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_skillLearn.clear();
		parseDatapackFile("data/skillLearn.xml");
		LOG.info("{}: Loaded {} Skill Learn data.", getClass().getSimpleName(), _skillLearn.size());
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		for (Node node = doc.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling())
				{
					if ("npc".equalsIgnoreCase(list_node.getNodeName()))
					{
						final List<ClassId> classIds = new ArrayList<>();
						for (Node c = list_node.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("classId".equalsIgnoreCase(c.getNodeName()))
							{
								classIds.add(ClassId.getClassId(Integer.parseInt(c.getTextContent())));
							}
						}
						_skillLearn.put(parseInteger(list_node.getAttributes(), "id"), classIds);
					}
				}
			}
		}
	}
	
	/**
	 * @param npcId
	 * @return {@link List} of {@link ClassId}'s that this npcId can teach.
	 */
	public List<ClassId> getSkillLearnData(int npcId)
	{
		return _skillLearn.get(npcId);
	}
	
	/**
	 * Gets the single instance of SkillLearnData.
	 * @return single instance of SkillLearnData
	 */
	public static SkillLearnData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillLearnData _instance = new SkillLearnData();
	}
}
