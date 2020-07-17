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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.commons.database.pool.impl.ConnectionFactory;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.SpawnTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.MapRegionManager;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.interfaces.IIdentifiable;
import com.l2jserver.gameserver.util.Broadcast;
import com.l2jserver.util.Rnd;

/**
 * Auto Spawn handler.<br>
 * Allows spawning of a NPC object based on a timer.<br>
 * (From the official idea used for the Merchant and Blacksmith of Mammon)<br>
 * General Usage: - Call registerSpawn() with the parameters listed below.<br>
 * int npcId int[][] spawnPoints or specify NULL to add points later.<br>
 * int initialDelay (If < 0 = default value) int respawnDelay (If < 0 = default value)<br>
 * int despawnDelay (If < 0 = default value or if = 0, function disabled)<br>
 * spawnPoints is a standard two-dimensional int array containing X,Y and Z coordinates.<br>
 * The default respawn/despawn delays are currently every hour (as for Mammon on official servers).<br>
 * The resulting AutoSpawnInstance object represents the newly added spawn index.<br>
 * The internal methods of this object can be used to adjust random spawning, for instance a call to setRandomSpawn(1, true); would set the spawn at index 1 to be randomly rather than sequentially-based.<br>
 * Also they can be used to specify the number of NPC instances to spawn using setSpawnCount(), and broadcast a message to all users using setBroadcast().<br>
 * Random Spawning = OFF by default Broadcasting = OFF by default
 * @author Tempy
 */
public class AutoSpawnHandler
{
	protected static final Logger _log = Logger.getLogger(AutoSpawnHandler.class.getName());
	
	private static final int DEFAULT_INITIAL_SPAWN = 30000; // 30 seconds after registration
	private static final int DEFAULT_RESPAWN = 3600000; // 1 hour in millisecs
	private static final int DEFAULT_DESPAWN = 3600000; // 1 hour in millisecs
	
	protected Map<Integer, AutoSpawnInstance> _registeredSpawns = new ConcurrentHashMap<>();
	protected Map<Integer, ScheduledFuture<?>> _runningSpawns = new ConcurrentHashMap<>();
	
	protected boolean _activeState = true;
	
	protected AutoSpawnHandler()
	{
		restoreSpawnData();
	}
	
	public static AutoSpawnHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public final int size()
	{
		return _registeredSpawns.size();
	}
	
	public void reload()
	{
		// stop all timers
		for (ScheduledFuture<?> sf : _runningSpawns.values())
		{
			if (sf != null)
			{
				sf.cancel(true);
			}
		}
		// unregister all registered spawns
		for (AutoSpawnInstance asi : _registeredSpawns.values())
		{
			if (asi != null)
			{
				this.removeSpawn(asi);
			}
		}
		
		// create clean list
		_registeredSpawns.clear();
		_runningSpawns.clear();
		
		// load
		restoreSpawnData();
	}
	
	private void restoreSpawnData()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM random_spawn ORDER BY groupId ASC");
			PreparedStatement ps = con.prepareStatement("SELECT * FROM random_spawn_loc WHERE groupId=?"))
		{
			// Restore spawn group data, then the location data.
			while (rs.next())
			{
				// Register random spawn group, set various options on the
				// created spawn instance.
				AutoSpawnInstance spawnInst = registerSpawn(rs.getInt("npcId"), rs.getInt("initialDelay"), rs.getInt("respawnDelay"), rs.getInt("despawnDelay"));
				
				spawnInst.setSpawnCount(rs.getInt("count"));
				spawnInst.setBroadcast(rs.getBoolean("broadcastSpawn"));
				spawnInst.setRandomSpawn(rs.getBoolean("randomSpawn"));
				
				// Restore the spawn locations for this spawn group/instance.
				ps.setInt(1, rs.getInt("groupId"));
				try (ResultSet rs2 = ps.executeQuery())
				{
					ps.clearParameters();
					
					while (rs2.next())
					{
						// Add each location to the spawn group/instance.
						spawnInst.addSpawnLocation(rs2.getInt("x"), rs2.getInt("y"), rs2.getInt("z"), rs2.getInt("heading"));
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "AutoSpawnHandler: Could not restore spawn data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Registers a spawn with the given parameters with the spawner, and marks it as active.<br>
	 * Returns a AutoSpawnInstance containing info about the spawn.
	 * @param npcId
	 * @param spawnPoints
	 * @param initialDelay (If < 0 = default value)
	 * @param respawnDelay (If < 0 = default value)
	 * @param despawnDelay (If < 0 = default value or if = 0, function disabled)
	 * @return AutoSpawnInstance spawnInst
	 */
	public AutoSpawnInstance registerSpawn(int npcId, int[][] spawnPoints, int initialDelay, int respawnDelay, int despawnDelay)
	{
		if (initialDelay < 0)
		{
			initialDelay = DEFAULT_INITIAL_SPAWN;
		}
		
		if (respawnDelay < 0)
		{
			respawnDelay = DEFAULT_RESPAWN;
		}
		
		if (despawnDelay < 0)
		{
			despawnDelay = DEFAULT_DESPAWN;
		}
		
		AutoSpawnInstance newSpawn = new AutoSpawnInstance(npcId, initialDelay, respawnDelay, despawnDelay);
		
		if (spawnPoints != null)
		{
			for (int[] spawnPoint : spawnPoints)
			{
				newSpawn.addSpawnLocation(spawnPoint);
			}
		}
		
		int newId = IdFactory.getInstance().getNextId();
		newSpawn._objectId = newId;
		_registeredSpawns.put(newId, newSpawn);
		
		setSpawnActive(newSpawn, true);
		return newSpawn;
	}
	
	/**
	 * Registers a spawn with the given parameters with the spawner, and marks it as active.<br>
	 * Returns a AutoSpawnInstance containing info about the spawn.<br>
	 * <B>Warning:</B> Spawn locations must be specified separately using addSpawnLocation().
	 * @param npcId
	 * @param initialDelay (If < 0 = default value)
	 * @param respawnDelay (If < 0 = default value)
	 * @param despawnDelay (If < 0 = default value or if = 0, function disabled)
	 * @return AutoSpawnInstance spawnInst
	 */
	public AutoSpawnInstance registerSpawn(int npcId, int initialDelay, int respawnDelay, int despawnDelay)
	{
		return registerSpawn(npcId, null, initialDelay, respawnDelay, despawnDelay);
	}
	
	/**
	 * Remove a registered spawn from the list, specified by the given spawn instance.
	 * @param spawnInst
	 * @return boolean removedSuccessfully
	 */
	public boolean removeSpawn(AutoSpawnInstance spawnInst)
	{
		if (!isSpawnRegistered(spawnInst))
		{
			return false;
		}
		
		try
		{
			// Try to remove from the list of registered spawns if it exists.
			_registeredSpawns.remove(spawnInst.getId());
			
			// Cancel the currently associated running scheduled task.
			ScheduledFuture<?> respawnTask = _runningSpawns.remove(spawnInst._objectId);
			respawnTask.cancel(false);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "AutoSpawnHandler: Could not auto spawn for NPC ID " + spawnInst._npcId + " (Object ID = " + spawnInst._objectId + "): " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Remove a registered spawn from the list, specified by the given spawn object ID.
	 * @param objectId
	 */
	public void removeSpawn(int objectId)
	{
		removeSpawn(_registeredSpawns.get(objectId));
	}
	
	/**
	 * Sets the active state of the specified spawn.
	 * @param spawnInst
	 * @param isActive
	 */
	public void setSpawnActive(AutoSpawnInstance spawnInst, boolean isActive)
	{
		if (spawnInst == null)
		{
			return;
		}
		
		int objectId = spawnInst._objectId;
		
		if (isSpawnRegistered(objectId))
		{
			ScheduledFuture<?> spawnTask = null;
			
			if (isActive)
			{
				AutoSpawner rs = new AutoSpawner(objectId);
				
				if (spawnInst._desDelay > 0)
				{
					spawnTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(rs, spawnInst._initDelay, spawnInst._resDelay);
				}
				else
				{
					spawnTask = ThreadPoolManager.getInstance().scheduleEffect(rs, spawnInst._initDelay);
				}
				
				_runningSpawns.put(objectId, spawnTask);
			}
			else
			{
				AutoDespawner rd = new AutoDespawner(objectId);
				spawnTask = _runningSpawns.remove(objectId);
				
				if (spawnTask != null)
				{
					spawnTask.cancel(false);
				}
				
				ThreadPoolManager.getInstance().scheduleEffect(rd, 0);
			}
			
			spawnInst.setSpawnActive(isActive);
		}
	}
	
	/**
	 * Sets the active state of all auto spawn instances to that specified, and cancels the scheduled spawn task if necessary.
	 * @param isActive
	 */
	public void setAllActive(boolean isActive)
	{
		if (_activeState == isActive)
		{
			return;
		}
		
		for (AutoSpawnInstance spawnInst : _registeredSpawns.values())
		{
			setSpawnActive(spawnInst, isActive);
		}
		
		_activeState = isActive;
	}
	
	/**
	 * Returns the number of milliseconds until the next occurrence of the given spawn.
	 * @param spawnInst
	 * @return
	 */
	public final long getTimeToNextSpawn(AutoSpawnInstance spawnInst)
	{
		int objectId = spawnInst.getObjectId();
		
		if (!isSpawnRegistered(objectId))
		{
			return -1;
		}
		
		return (_runningSpawns.containsKey(objectId)) ? _runningSpawns.get(objectId).getDelay(TimeUnit.MILLISECONDS) : 0;
	}
	
	/**
	 * Attempts to return the AutoSpawnInstance associated with the given NPC or Object ID type.<br>
	 * Note: If isObjectId == false, returns first instance for the specified NPC ID.
	 * @param id
	 * @param isObjectId
	 * @return AutoSpawnInstance spawnInst
	 */
	public final AutoSpawnInstance getAutoSpawnInstance(int id, boolean isObjectId)
	{
		if (isObjectId)
		{
			if (isSpawnRegistered(id))
			{
				return _registeredSpawns.get(id);
			}
		}
		else
		{
			for (AutoSpawnInstance spawnInst : _registeredSpawns.values())
			{
				if (spawnInst.getId() == id)
				{
					return spawnInst;
				}
			}
		}
		return null;
	}
	
	public List<AutoSpawnInstance> getAutoSpawnInstances(int npcId)
	{
		final List<AutoSpawnInstance> result = new LinkedList<>();
		for (AutoSpawnInstance spawnInst : _registeredSpawns.values())
		{
			if (spawnInst.getId() == npcId)
			{
				result.add(spawnInst);
			}
		}
		return result;
	}
	
	/**
	 * Tests if the specified object ID is assigned to an auto spawn.
	 * @param objectId
	 * @return isAssigned
	 */
	public final boolean isSpawnRegistered(int objectId)
	{
		return _registeredSpawns.containsKey(objectId);
	}
	
	/**
	 * Tests if the specified spawn instance is assigned to an auto spawn.
	 * @param spawnInst
	 * @return boolean isAssigned
	 */
	public final boolean isSpawnRegistered(AutoSpawnInstance spawnInst)
	{
		return _registeredSpawns.containsValue(spawnInst);
	}
	
	/**
	 * AutoSpawner class<br>
	 * This handles the main spawn task for an auto spawn instance, and initializes a despawner if required.
	 * @author Tempy
	 */
	private class AutoSpawner implements Runnable
	{
		private final int _objectId;
		
		protected AutoSpawner(int objectId)
		{
			_objectId = objectId;
		}
		
		@Override
		public void run()
		{
			try
			{
				// Retrieve the required spawn instance for this spawn task.
				AutoSpawnInstance spawnInst = _registeredSpawns.get(_objectId);
				
				// If the spawn is not scheduled to be active, cancel the spawn
				// task.
				if (!spawnInst.isSpawnActive())
				{
					return;
				}
				
				Location[] locationList = spawnInst.getLocationList();
				
				// If there are no set co-ordinates, cancel the spawn task.
				if (locationList.length == 0)
				{
					_log.info("AutoSpawnHandler: No location co-ords specified for spawn instance (Object ID = " + _objectId + ").");
					return;
				}
				
				int locationCount = locationList.length;
				int locationIndex = Rnd.nextInt(locationCount);
				
				// If random spawning is disabled, the spawn at the next set of co-ordinates after the last.
				// If the index is greater than the number of possible spawns, reset the counter to zero.
				if (!spawnInst.isRandomSpawn())
				{
					locationIndex = spawnInst._lastLocIndex + 1;
					
					if (locationIndex == locationCount)
					{
						locationIndex = 0;
					}
					
					spawnInst._lastLocIndex = locationIndex;
				}
				
				// Set the X, Y and Z co-ordinates, where this spawn will take place.
				final int x = locationList[locationIndex].getX();
				final int y = locationList[locationIndex].getY();
				final int z = locationList[locationIndex].getZ();
				final int heading = locationList[locationIndex].getHeading();
				
				final L2Spawn newSpawn = new L2Spawn(spawnInst.getId());
				newSpawn.setX(x);
				newSpawn.setY(y);
				newSpawn.setZ(z);
				if (heading != -1)
				{
					newSpawn.setHeading(heading);
				}
				newSpawn.setAmount(spawnInst.getSpawnCount());
				if (spawnInst._desDelay == 0)
				{
					newSpawn.setRespawnDelay(spawnInst._resDelay);
				}
				
				// Add the new spawn information to the spawn table, but do not store it.
				SpawnTable.getInstance().addNewSpawn(newSpawn, false);
				L2Npc npcInst = null;
				
				if (spawnInst._spawnCount == 1)
				{
					npcInst = newSpawn.doSpawn();
					npcInst.setXYZ(npcInst.getX(), npcInst.getY(), npcInst.getZ());
					spawnInst.addNpcInstance(npcInst);
				}
				else
				{
					for (int i = 0; i < spawnInst._spawnCount; i++)
					{
						npcInst = newSpawn.doSpawn();
						
						// To prevent spawning of more than one NPC in the exact same spot, move it slightly by a small random offset.
						npcInst.setXYZ(npcInst.getX() + Rnd.nextInt(50), npcInst.getY() + Rnd.nextInt(50), npcInst.getZ());
						
						// Add the NPC instance to the list of managed instances.
						spawnInst.addNpcInstance(npcInst);
					}
				}
				
				if (npcInst != null)
				{
					String nearestTown = MapRegionManager.getInstance().getClosestTownName(npcInst);
					
					// Announce to all players that the spawn has taken place, with the nearest town location.
					if (spawnInst.isBroadcasting())
					{
						Broadcast.toAllOnlinePlayers("The " + npcInst.getName() + " has spawned near " + nearestTown + "!");
					}
				}
				
				// If there is no despawn time, do not create a despawn task.
				if (spawnInst.getDespawnDelay() > 0)
				{
					AutoDespawner rd = new AutoDespawner(_objectId);
					ThreadPoolManager.getInstance().scheduleAi(rd, spawnInst.getDespawnDelay() - 1000);
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "AutoSpawnHandler: An error occurred while initializing spawn instance (Object ID = " + _objectId + "): " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * AutoDespawner Class<br>
	 * Simply used as a secondary class for despawning an auto spawn instance.
	 * @author Tempy
	 */
	private class AutoDespawner implements Runnable
	{
		private final int _objectId;
		
		protected AutoDespawner(int objectId)
		{
			_objectId = objectId;
		}
		
		@Override
		public void run()
		{
			try
			{
				AutoSpawnInstance spawnInst = _registeredSpawns.get(_objectId);
				
				if (spawnInst == null)
				{
					_log.info("AutoSpawnHandler: No spawn registered for object ID = " + _objectId + ".");
					return;
				}
				
				for (L2Npc npcInst : spawnInst.getNPCInstanceList())
				{
					npcInst.deleteMe();
					SpawnTable.getInstance().deleteSpawn(npcInst.getSpawn(), false);
					spawnInst.removeNpcInstance(npcInst);
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "AutoSpawnHandler: An error occurred while despawning spawn (Object ID = " + _objectId + "): " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * AutoSpawnInstance Class<br>
	 * Stores information about a registered auto spawn.
	 * @author Tempy
	 */
	public static class AutoSpawnInstance implements IIdentifiable
	{
		protected int _objectId;
		
		protected int _spawnIndex;
		
		protected int _npcId;
		
		protected int _initDelay;
		
		protected int _resDelay;
		
		protected int _desDelay;
		
		protected int _spawnCount = 1;
		
		protected int _lastLocIndex = -1;
		
		private final Queue<L2Npc> _npcList = new ConcurrentLinkedQueue<>();
		
		private final List<Location> _locList = new CopyOnWriteArrayList<>();
		
		private boolean _spawnActive;
		
		private boolean _randomSpawn = false;
		
		private boolean _broadcastAnnouncement = false;
		
		protected AutoSpawnInstance(int npcId, int initDelay, int respawnDelay, int despawnDelay)
		{
			_npcId = npcId;
			_initDelay = initDelay;
			_resDelay = respawnDelay;
			_desDelay = despawnDelay;
		}
		
		protected void setSpawnActive(boolean activeValue)
		{
			_spawnActive = activeValue;
		}
		
		protected boolean addNpcInstance(L2Npc npcInst)
		{
			return _npcList.add(npcInst);
		}
		
		protected boolean removeNpcInstance(L2Npc npcInst)
		{
			return _npcList.remove(npcInst);
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public int getInitialDelay()
		{
			return _initDelay;
		}
		
		public int getRespawnDelay()
		{
			return _resDelay;
		}
		
		public int getDespawnDelay()
		{
			return _desDelay;
		}
		
		/**
		 * Gets the NPC ID.
		 * @return the NPC ID
		 */
		@Override
		public int getId()
		{
			return _npcId;
		}
		
		public int getSpawnCount()
		{
			return _spawnCount;
		}
		
		public Location[] getLocationList()
		{
			return _locList.toArray(new Location[_locList.size()]);
		}
		
		public Queue<L2Npc> getNPCInstanceList()
		{
			return _npcList;
		}
		
		public List<L2Spawn> getSpawns()
		{
			final List<L2Spawn> npcSpawns = new ArrayList<>();
			for (L2Npc npcInst : _npcList)
			{
				npcSpawns.add(npcInst.getSpawn());
			}
			return npcSpawns;
		}
		
		public void setSpawnCount(int spawnCount)
		{
			_spawnCount = spawnCount;
		}
		
		public void setRandomSpawn(boolean randValue)
		{
			_randomSpawn = randValue;
		}
		
		public void setBroadcast(boolean broadcastValue)
		{
			_broadcastAnnouncement = broadcastValue;
		}
		
		public boolean isSpawnActive()
		{
			return _spawnActive;
		}
		
		public boolean isRandomSpawn()
		{
			return _randomSpawn;
		}
		
		public boolean isBroadcasting()
		{
			return _broadcastAnnouncement;
		}
		
		public boolean addSpawnLocation(int x, int y, int z, int heading)
		{
			return _locList.add(new Location(x, y, z, heading));
		}
		
		public boolean addSpawnLocation(int[] spawnLoc)
		{
			if (spawnLoc.length != 3)
			{
				return false;
			}
			
			return addSpawnLocation(spawnLoc[0], spawnLoc[1], spawnLoc[2], -1);
		}
		
		public Location removeSpawnLocation(int locIndex)
		{
			try
			{
				return _locList.remove(locIndex);
			}
			catch (IndexOutOfBoundsException e)
			{
				return null;
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final AutoSpawnHandler _instance = new AutoSpawnHandler();
	}
}
