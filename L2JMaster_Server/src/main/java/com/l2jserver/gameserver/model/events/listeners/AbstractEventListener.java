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
package com.l2jserver.gameserver.model.events.listeners;

import com.l2jserver.gameserver.model.events.EventType;
import com.l2jserver.gameserver.model.events.ListenersContainer;
import com.l2jserver.gameserver.model.events.impl.IBaseEvent;
import com.l2jserver.gameserver.model.events.returns.AbstractEventReturn;

/**
 * @author UnAfraid
 */
public abstract class AbstractEventListener implements Comparable<AbstractEventListener>
{
	private int _priority = 0;
	private final ListenersContainer _container;
	private final EventType _type;
	private final Object _owner;
	
	public AbstractEventListener(ListenersContainer container, EventType type, Object owner)
	{
		_container = container;
		_type = type;
		_owner = owner;
	}
	
	/**
	 * @return the container on which this listener is being registered (Used to unregister when unloading scripts)
	 */
	public ListenersContainer getContainer()
	{
		return _container;
	}
	
	/**
	 * @return the type of event which listener is listening for.
	 */
	public EventType getType()
	{
		return _type;
	}
	
	/**
	 * @return the owner of the listener, the object that registered this listener.
	 */
	public Object getOwner()
	{
		return _owner;
	}
	
	/**
	 * @return priority of execution (Higher the sooner)
	 */
	public int getPriority()
	{
		return _priority;
	}
	
	/**
	 * Sets priority of execution.
	 * @param priority
	 */
	public void setPriority(int priority)
	{
		_priority = priority;
	}
	
	/**
	 * Method invoked by EventDispatcher that will use the callback.
	 * @param <R>
	 * @param event
	 * @param returnBackClass
	 * @return
	 */
	public abstract <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass);
	
	/**
	 * Unregisters detaches and unregisters current listener.
	 */
	public void unregisterMe()
	{
		getContainer().removeListener(this);
	}
	
	@Override
	public int compareTo(AbstractEventListener o)
	{
		return Integer.compare(o.getPriority(), getPriority());
	}
}
