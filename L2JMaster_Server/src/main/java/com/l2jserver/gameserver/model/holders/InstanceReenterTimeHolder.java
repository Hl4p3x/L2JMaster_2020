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
package com.l2jserver.gameserver.model.holders;

import java.time.DayOfWeek;

/**
 * Simple class for storing Reenter Data for Instances.
 * @author FallenAngel
 */
public final class InstanceReenterTimeHolder
{
	private final DayOfWeek _day;
	private final int _hour;
	private final int _minute;
	private final long _time;
	
	public InstanceReenterTimeHolder(long time)
	{
		_time = time;
		_day = null;
		_hour = -1;
		_minute = -1;
	}
	
	public InstanceReenterTimeHolder(DayOfWeek day, int hour, int minute)
	{
		_time = -1;
		_day = day;
		_hour = hour;
		_minute = minute;
	}
	
	public final Long getTime()
	{
		return _time;
	}
	
	public final DayOfWeek getDay()
	{
		return _day;
	}
	
	public final int getHour()
	{
		return _hour;
	}
	
	public final int getMinute()
	{
		return _minute;
	}
}