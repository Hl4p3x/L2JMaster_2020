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
import java.util.Calendar;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.model.SiegeScheduleDate;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.data.xml.IXmlReader;

/**
 * Siege Schedule data.
 * @author UnAfraid
 */
public class SiegeScheduleData implements IXmlReader
{
	private final List<SiegeScheduleDate> _scheduleData = new ArrayList<>();
	
	protected SiegeScheduleData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_scheduleData.clear();
		parseDatapackFile("config/SiegeSchedule.xml");
		LOG.info("{}: Loaded: {} siege schedulers.", getClass().getSimpleName(), _scheduleData.size());
		if (_scheduleData.isEmpty())
		{
			_scheduleData.add(new SiegeScheduleDate(new StatsSet()));
			LOG.info("{}: Emergency Loaded: {} default siege schedulers.", getClass().getSimpleName(), _scheduleData.size());
		}
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node cd = n.getFirstChild(); cd != null; cd = cd.getNextSibling())
				{
					switch (cd.getNodeName())
					{
						case "schedule":
						{
							final StatsSet set = new StatsSet();
							final NamedNodeMap attrs = cd.getAttributes();
							for (int i = 0; i < attrs.getLength(); i++)
							{
								Node node = attrs.item(i);
								String key = node.getNodeName();
								String val = node.getNodeValue();
								if ("day".equals(key))
								{
									if (!Util.isDigit(val))
									{
										val = Integer.toString(getValueForField(val));
									}
								}
								set.set(key, val);
							}
							_scheduleData.add(new SiegeScheduleDate(set));
							break;
						}
					}
				}
			}
		}
	}
	
	private int getValueForField(String field)
	{
		try
		{
			return Calendar.class.getField(field).getInt(Calendar.class);
		}
		catch (Exception e)
		{
			LOG.warn("{}: Unable to get value!", getClass().getSimpleName(), e);
			return -1;
		}
	}
	
	public List<SiegeScheduleDate> getScheduleDates()
	{
		return _scheduleData;
	}
	
	public static final SiegeScheduleData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SiegeScheduleData _instance = new SiegeScheduleData();
	}
	
}
