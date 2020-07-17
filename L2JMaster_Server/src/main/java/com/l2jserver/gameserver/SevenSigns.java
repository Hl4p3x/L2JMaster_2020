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
package com.l2jserver.gameserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.Config;
import com.l2jserver.commons.database.pool.impl.ConnectionFactory;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.model.AutoSpawnHandler;
import com.l2jserver.gameserver.model.AutoSpawnHandler.AutoSpawnInstance;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.TeleportWhereType;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.skills.CommonSkill;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SSQInfo;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.util.Broadcast;

/**
 * Seven Signs engine.
 * @author Tempy
 */
public class SevenSigns
{
	protected static final Logger _log = LoggerFactory.getLogger(SevenSigns.class);
	
	// Basic Seven Signs Constants \\
	public static final String SEVEN_SIGNS_HTML_PATH = "data/html/seven_signs/";
	
	public static final int CABAL_NULL = 0;
	public static final int CABAL_DUSK = 1;
	public static final int CABAL_DAWN = 2;
	
	public static final int SEAL_NULL = 0;
	public static final int SEAL_AVARICE = 1;
	public static final int SEAL_GNOSIS = 2;
	public static final int SEAL_STRIFE = 3;
	
	public static final int PERIOD_COMP_RECRUITING = 0;
	public static final int PERIOD_COMPETITION = 1;
	public static final int PERIOD_COMP_RESULTS = 2;
	public static final int PERIOD_SEAL_VALIDATION = 3;
	
	public static final int PERIOD_START_HOUR = 18;
	public static final int PERIOD_START_MINS = 00;
	public static final int PERIOD_START_DAY = Calendar.MONDAY;
	
	// The quest event and seal validation periods last for approximately one week
	// with a 15 minutes "interval" period sandwiched between them.
	public static final int PERIOD_MINOR_LENGTH = 900000;
	public static final int PERIOD_MAJOR_LENGTH = 604800000 - PERIOD_MINOR_LENGTH;
	
	public static final int RECORD_SEVEN_SIGNS_ID = 5707;
	public static final int RECORD_SEVEN_SIGNS_COST = 500;
	
	// NPC Related Constants \\
	public static final int ORATOR_NPC_ID = 31094;
	public static final int PREACHER_NPC_ID = 31093;
	public static final int MAMMON_MERCHANT_ID = 31113;
	public static final int MAMMON_BLACKSMITH_ID = 31126;
	public static final int MAMMON_MARKETEER_ID = 31092;
	public static final int LILITH_NPC_ID = 25283;
	public static final int ANAKIM_NPC_ID = 25286;
	public static final int CREST_OF_DAWN_ID = 31170;
	public static final int CREST_OF_DUSK_ID = 31171;
	// Seal Stone Related Constants
	public static final int SEAL_STONE_BLUE_ID = 6360;
	public static final int SEAL_STONE_GREEN_ID = 6361;
	public static final int SEAL_STONE_RED_ID = 6362;
	
	public static final int[] SEAL_STONE_IDS =
	{
		SEAL_STONE_BLUE_ID,
		SEAL_STONE_GREEN_ID,
		SEAL_STONE_RED_ID
	};
	
	public static final int SEAL_STONE_BLUE_VALUE = 3;
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	public static final int SEAL_STONE_RED_VALUE = 10;
	
	public static final int BLUE_CONTRIB_POINTS = 3;
	public static final int GREEN_CONTRIB_POINTS = 5;
	public static final int RED_CONTRIB_POINTS = 10;
	
	private final Calendar _nextPeriodChange = Calendar.getInstance();
	
	protected int _activePeriod;
	protected int _currentCycle;
	protected double _dawnStoneScore;
	protected double _duskStoneScore;
	protected int _dawnFestivalScore;
	protected int _duskFestivalScore;
	protected int _compWinner;
	protected int _previousWinner;
	protected Calendar _lastSave = Calendar.getInstance();
	
	protected Map<Integer, StatsSet> _signsPlayerData = new LinkedHashMap<>();
	private final Map<Integer, Integer> _signsSealOwners = new LinkedHashMap<>();
	private final Map<Integer, Integer> _signsDuskSealTotals = new LinkedHashMap<>();
	private final Map<Integer, Integer> _signsDawnSealTotals = new LinkedHashMap<>();
	
	private static final String LOAD_DATA = "SELECT charId, cabal, seal, red_stones, green_stones, blue_stones, " + "ancient_adena_amount, contribution_score FROM seven_signs";
	
	private static final String LOAD_STATUS = "SELECT * FROM seven_signs_status WHERE id=0";
	
	private static final String INSERT_PLAYER = "INSERT INTO seven_signs (charId, cabal, seal) VALUES (?,?,?)";
	
	private static final String UPDATE_PLAYER = "UPDATE seven_signs SET cabal=?, seal=?, red_stones=?, green_stones=?, blue_stones=?, " + "ancient_adena_amount=?, contribution_score=? WHERE charId=?";
	
	private static final String UPDATE_STATUS = "UPDATE seven_signs_status SET current_cycle=?, active_period=?, previous_winner=?, " + "dawn_stone_score=?, dawn_festival_score=?, dusk_stone_score=?, dusk_festival_score=?, "
		+ "avarice_owner=?, gnosis_owner=?, strife_owner=?, avarice_dawn_score=?, gnosis_dawn_score=?, " + "strife_dawn_score=?, avarice_dusk_score=?, gnosis_dusk_score=?, strife_dusk_score=?, " + "festival_cycle=?, accumulated_bonus0=?, accumulated_bonus1=?, accumulated_bonus2=?,"
		+ "accumulated_bonus3=?, accumulated_bonus4=?, date=? WHERE id=0";
		
	protected SevenSigns()
	{
		try
		{
			restoreSevenSignsData();
		}
		catch (Exception e)
		{
			_log.error("SevenSigns: Failed to load configuration!", e);
		}
		
		_log.info("SevenSigns: Currently in the {} period.", getCurrentPeriodName());
		
		initializeSeals();
		
		if (isSealValidationPeriod())
		{
			if (getCabalHighestScore() == CABAL_NULL)
			{
				_log.info("SevenSigns: The competition ended with a tie last week.");
			}
			else
			{
				_log.info("SevenSigns: The {} were victorious last week.", getCabalName(getCabalHighestScore()));
			}
		}
		else if (getCabalHighestScore() == CABAL_NULL)
		{
			_log.info("SevenSigns: The competition, if the current trend continues, will end in a tie this week.");
		}
		else
		{
			_log.info("SevenSigns: The {} are in the lead this week.", getCabalName(getCabalHighestScore()));
		}
		
		long milliToChange = 0;
		if (isNextPeriodChangeInPast())
		{
			_log.info("SevenSigns: Next period change was in the past (server was offline), changing periods now!");
		}
		else
		{
			setCalendarForNextPeriodChange();
			milliToChange = getMilliToPeriodChange();
		}
		
		// Schedule a time for the next period change.
		SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
		ThreadPoolManager.getInstance().scheduleGeneral(sspc, milliToChange);
		
		// Thanks to http://rainbow.arch.scriptmania.com/scripts/timezone_countdown.html for help with this.
		double numSecs = (milliToChange / 1000) % 60;
		double countDown = ((milliToChange / 1000.0) - numSecs) / 60;
		int numMins = (int) Math.floor(countDown % 60);
		countDown = (countDown - numMins) / 60;
		int numHours = (int) Math.floor(countDown % 24);
		int numDays = (int) Math.floor((countDown - numHours) / 24);
		
		_log.info("SevenSigns: Next period begins in {} days, {} hours and {} mins.", numDays, numHours, numMins);
		
	}
	
	private boolean isNextPeriodChangeInPast()
	{
		Calendar lastPeriodChange = Calendar.getInstance();
		switch (getCurrentPeriod())
		{
			case PERIOD_SEAL_VALIDATION:
			case PERIOD_COMPETITION:
				lastPeriodChange.set(Calendar.DAY_OF_WEEK, PERIOD_START_DAY);
				lastPeriodChange.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				lastPeriodChange.set(Calendar.MINUTE, PERIOD_START_MINS);
				lastPeriodChange.set(Calendar.SECOND, 0);
				// if we hit next week, just turn back 1 week
				if (Calendar.getInstance().before(lastPeriodChange))
				{
					lastPeriodChange.add(Calendar.HOUR, -24 * 7);
				}
				break;
			case PERIOD_COMP_RECRUITING:
			case PERIOD_COMP_RESULTS:
				// because of the short duration of this period, just check it from last save
				lastPeriodChange.setTimeInMillis(_lastSave.getTimeInMillis() + PERIOD_MINOR_LENGTH);
				break;
		}
		
		// because of previous "date" column usage, check only if it already contains usable data for us
		if ((_lastSave.getTimeInMillis() > 7) && _lastSave.before(lastPeriodChange))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Registers all random spawns and auto-chats for Seven Signs NPCs, along with spawns for the Preachers of Doom and Orators of Revelations at the beginning of the Seal Validation period.
	 */
	public void spawnSevenSignsNPC()
	{
		final AutoSpawnInstance merchantSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(MAMMON_MERCHANT_ID, false);
		final AutoSpawnInstance blacksmithSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(MAMMON_BLACKSMITH_ID, false);
		final AutoSpawnInstance lilithSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(LILITH_NPC_ID, false);
		final AutoSpawnInstance anakimSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(ANAKIM_NPC_ID, false);
		final List<AutoSpawnInstance> crestOfDawnSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(CREST_OF_DAWN_ID);
		final List<AutoSpawnInstance> crestOfDuskSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(CREST_OF_DUSK_ID);
		final List<AutoSpawnInstance> oratorSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(ORATOR_NPC_ID);
		final List<AutoSpawnInstance> preacherSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(PREACHER_NPC_ID);
		final List<AutoSpawnInstance> marketeerSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(MAMMON_MARKETEER_ID);
		
		if (isSealValidationPeriod() || isCompResultsPeriod())
		{
			for (AutoSpawnInstance spawnInst : marketeerSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
			}
			
			if ((getSealOwner(SEAL_GNOSIS) == getCabalHighestScore()) && (getSealOwner(SEAL_GNOSIS) != CABAL_NULL))
			{
				if (!Config.ANNOUNCE_MAMMON_SPAWN)
				{
					blacksmithSpawn.setBroadcast(false);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(blacksmithSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(blacksmithSpawn, true);
				}
				
				for (AutoSpawnInstance spawnInst : oratorSpawns)
				{
					if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
					}
				}
				
				for (AutoSpawnInstance spawnInst : preacherSpawns)
				{
					if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
					}
				}
			}
			else
			{
				AutoSpawnHandler.getInstance().setSpawnActive(blacksmithSpawn, false);
				
				for (AutoSpawnInstance spawnInst : oratorSpawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
				}
				
				for (AutoSpawnInstance spawnInst : preacherSpawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
				}
			}
			
			if ((getSealOwner(SEAL_AVARICE) == getCabalHighestScore()) && (getSealOwner(SEAL_AVARICE) != CABAL_NULL))
			{
				if (!Config.ANNOUNCE_MAMMON_SPAWN)
				{
					merchantSpawn.setBroadcast(false);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(merchantSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(merchantSpawn, true);
				}
				
				switch (getCabalHighestScore())
				{
					case CABAL_DAWN:
						if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(lilithSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, true);
						}
						
						AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, false);
						
						for (AutoSpawnInstance dawnCrest : crestOfDawnSpawns)
						{
							if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(dawnCrest.getObjectId(), true).isSpawnActive())
							{
								AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, true);
							}
						}
						
						for (AutoSpawnInstance duskCrest : crestOfDuskSpawns)
						{
							AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, false);
						}
						break;
						
					case CABAL_DUSK:
						if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(anakimSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, true);
						}
						
						AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, false);
						
						for (AutoSpawnInstance duskCrest : crestOfDuskSpawns)
						{
							if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(duskCrest.getObjectId(), true).isSpawnActive())
							{
								AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, true);
							}
						}
						
						for (AutoSpawnInstance dawnCrest : crestOfDawnSpawns)
						{
							AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
						}
						break;
				}
			}
			else
			{
				AutoSpawnHandler.getInstance().setSpawnActive(merchantSpawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, false);
				for (AutoSpawnInstance dawnCrest : crestOfDawnSpawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
				}
				for (AutoSpawnInstance duskCrest : crestOfDuskSpawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, false);
				}
			}
		}
		else
		{
			AutoSpawnHandler.getInstance().setSpawnActive(merchantSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(blacksmithSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, false);
			for (AutoSpawnInstance dawnCrest : crestOfDawnSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
			}
			for (AutoSpawnInstance duskCrest : crestOfDuskSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, false);
			}
			for (AutoSpawnInstance spawnInst : oratorSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
			
			for (AutoSpawnInstance spawnInst : preacherSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
			
			for (AutoSpawnInstance spawnInst : marketeerSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
		}
	}
	
	public static SevenSigns getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static long calcContributionScore(long blueCount, long greenCount, long redCount)
	{
		long contrib = blueCount * BLUE_CONTRIB_POINTS;
		contrib += greenCount * GREEN_CONTRIB_POINTS;
		contrib += redCount * RED_CONTRIB_POINTS;
		
		return contrib;
	}
	
	public static long calcAncientAdenaReward(long blueCount, long greenCount, long redCount)
	{
		long reward = blueCount * SEAL_STONE_BLUE_VALUE;
		reward += greenCount * SEAL_STONE_GREEN_VALUE;
		reward += redCount * SEAL_STONE_RED_VALUE;
		
		return reward;
	}
	
	public static final String getCabalShortName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
				return "dawn";
			case CABAL_DUSK:
				return "dusk";
		}
		
		return "No Cabal";
	}
	
	public static final String getCabalName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
				return "Lords of Dawn";
			case CABAL_DUSK:
				return "Revolutionaries of Dusk";
		}
		
		return "No Cabal";
	}
	
	public static final String getSealName(int seal, boolean shortName)
	{
		String sealName = (!shortName) ? "Seal of " : "";
		
		switch (seal)
		{
			case SEAL_AVARICE:
				sealName += "Avarice";
				break;
			case SEAL_GNOSIS:
				sealName += "Gnosis";
				break;
			case SEAL_STRIFE:
				sealName += "Strife";
				break;
		}
		
		return sealName;
	}
	
	public final int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	public final int getCurrentPeriod()
	{
		return _activePeriod;
	}
	
	private final int getDaysToPeriodChange()
	{
		int numDays = _nextPeriodChange.get(Calendar.DAY_OF_WEEK) - PERIOD_START_DAY;
		if (numDays < 0)
		{
			return 0 - numDays;
		}
		return 7 - numDays;
	}
	
	public final long getMilliToPeriodChange()
	{
		return (_nextPeriodChange.getTimeInMillis() - System.currentTimeMillis());
	}
	
	protected void setCalendarForNextPeriodChange()
	{
		// Calculate the number of days until the next period
		// A period starts at 18:00 pm (local time), like on official servers.
		switch (getCurrentPeriod())
		{
			case PERIOD_SEAL_VALIDATION:
			case PERIOD_COMPETITION:
				int daysToChange = getDaysToPeriodChange();
				
				if (daysToChange == 7)
				{
					if (_nextPeriodChange.get(Calendar.HOUR_OF_DAY) < PERIOD_START_HOUR)
					{
						daysToChange = 0;
					}
					else if ((_nextPeriodChange.get(Calendar.HOUR_OF_DAY) == PERIOD_START_HOUR) && (_nextPeriodChange.get(Calendar.MINUTE) < PERIOD_START_MINS))
					{
						daysToChange = 0;
					}
				}
				
				// Otherwise...
				if (daysToChange > 0)
				{
					_nextPeriodChange.add(Calendar.DATE, daysToChange);
				}
				
				_nextPeriodChange.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				_nextPeriodChange.set(Calendar.MINUTE, PERIOD_START_MINS);
				break;
			case PERIOD_COMP_RECRUITING:
			case PERIOD_COMP_RESULTS:
				_nextPeriodChange.add(Calendar.MILLISECOND, PERIOD_MINOR_LENGTH);
				break;
		}
		_log.info("SevenSigns: Next period change set to {}.", _nextPeriodChange.getTime());
	}
	
	public final String getCurrentPeriodName()
	{
		String periodName = null;
		
		switch (_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
				periodName = "Quest Event Initialization";
				break;
			case PERIOD_COMPETITION:
				periodName = "Competition (Quest Event)";
				break;
			case PERIOD_COMP_RESULTS:
				periodName = "Quest Event Results";
				break;
			case PERIOD_SEAL_VALIDATION:
				periodName = "Seal Validation";
				break;
		}
		return periodName;
	}
	
	/**
	 * @return {@code true} if it's competition period, {@code false} otherwise
	 */
	public final boolean isCompetitionPeriod()
	{
		return (_activePeriod == PERIOD_COMPETITION);
	}
	
	public final boolean isSealValidationPeriod()
	{
		return (_activePeriod == PERIOD_SEAL_VALIDATION);
	}
	
	public final boolean isCompResultsPeriod()
	{
		return (_activePeriod == PERIOD_COMP_RESULTS);
	}
	
	/**
	 * returns true if the given date is in Seal Validation or in Quest Event Results period
	 * @param date
	 * @return
	 */
	public boolean isDateInSealValidPeriod(Calendar date)
	{
		long nextPeriodChange = getMilliToPeriodChange();
		long nextQuestStart = 0;
		long nextValidStart = 0;
		long tillDate = date.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
		while (((2 * PERIOD_MAJOR_LENGTH) + (2 * PERIOD_MINOR_LENGTH)) < tillDate)
		{
			tillDate -= ((2 * PERIOD_MAJOR_LENGTH) + (2 * PERIOD_MINOR_LENGTH));
		}
		while (tillDate < 0)
		{
			tillDate += ((2 * PERIOD_MAJOR_LENGTH) + (2 * PERIOD_MINOR_LENGTH));
		}
		
		switch (getCurrentPeriod())
		{
			case PERIOD_COMP_RECRUITING:
				nextValidStart = nextPeriodChange + PERIOD_MAJOR_LENGTH;
				nextQuestStart = nextValidStart + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			case PERIOD_COMPETITION:
				nextValidStart = nextPeriodChange;
				nextQuestStart = nextPeriodChange + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			case PERIOD_COMP_RESULTS:
				nextQuestStart = nextPeriodChange + PERIOD_MAJOR_LENGTH;
				nextValidStart = nextQuestStart + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			case PERIOD_SEAL_VALIDATION:
				nextQuestStart = nextPeriodChange;
				nextValidStart = nextPeriodChange + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
		}
		
		if (((nextQuestStart < tillDate) && (tillDate < nextValidStart)) || ((nextValidStart < nextQuestStart) && ((tillDate < nextValidStart) || (nextQuestStart < tillDate))))
		{
			return false;
		}
		return true;
	}
	
	public final int getCurrentScore(int cabal)
	{
		double totalStoneScore = _dawnStoneScore + _duskStoneScore;
		
		switch (cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return Math.round((float) (_dawnStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _dawnFestivalScore;
			case CABAL_DUSK:
				return Math.round((float) (_duskStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _duskFestivalScore;
		}
		
		return 0;
	}
	
	public final double getCurrentStoneScore(int cabal)
	{
		switch (cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return _dawnStoneScore;
			case CABAL_DUSK:
				return _duskStoneScore;
		}
		
		return 0;
	}
	
	public final int getCurrentFestivalScore(int cabal)
	{
		switch (cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return _dawnFestivalScore;
			case CABAL_DUSK:
				return _duskFestivalScore;
		}
		
		return 0;
	}
	
	public final int getCabalHighestScore()
	{
		if (getCurrentScore(CABAL_DUSK) == getCurrentScore(CABAL_DAWN))
		{
			return CABAL_NULL;
		}
		else if (getCurrentScore(CABAL_DUSK) > getCurrentScore(CABAL_DAWN))
		{
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_DAWN;
		}
	}
	
	public final int getSealOwner(int seal)
	{
		return _signsSealOwners.get(seal);
	}
	
	public final int getSealProportion(int seal, int cabal)
	{
		if (cabal == CABAL_NULL)
		{
			return 0;
		}
		else if (cabal == CABAL_DUSK)
		{
			return _signsDuskSealTotals.get(seal);
		}
		else
		{
			return _signsDawnSealTotals.get(seal);
		}
	}
	
	public final int getTotalMembers(int cabal)
	{
		int cabalMembers = 0;
		String cabalName = getCabalShortName(cabal);
		
		for (StatsSet sevenDat : _signsPlayerData.values())
		{
			if (sevenDat.getString("cabal").equals(cabalName))
			{
				cabalMembers++;
			}
		}
		
		return cabalMembers;
	}
	
	public int getPlayerStoneContrib(int objectId)
	{
		final StatsSet currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return 0;
		}
		
		int stoneCount = 0;
		stoneCount += currPlayer.getInt("red_stones");
		stoneCount += currPlayer.getInt("green_stones");
		stoneCount += currPlayer.getInt("blue_stones");
		
		return stoneCount;
	}
	
	public int getPlayerContribScore(int objectId)
	{
		final StatsSet currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return 0;
		}
		
		return currPlayer.getInt("contribution_score");
	}
	
	public int getPlayerAdenaCollect(int objectId)
	{
		final StatsSet currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return 0;
		}
		
		return currPlayer.getInt("ancient_adena_amount");
	}
	
	public int getPlayerSeal(int objectId)
	{
		final StatsSet currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return SEAL_NULL;
		}
		
		return currPlayer.getInt("seal");
	}
	
	public int getPlayerCabal(int objectId)
	{
		final StatsSet currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return CABAL_NULL;
		}
		
		String playerCabal = currPlayer.getString("cabal");
		if (playerCabal.equalsIgnoreCase("dawn"))
		{
			return CABAL_DAWN;
		}
		else if (playerCabal.equalsIgnoreCase("dusk"))
		{
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_NULL;
		}
	}
	
	/**
	 * Restores all Seven Signs data and settings, usually called at server startup.
	 */
	protected void restoreSevenSignsData()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection())
		{
			try (Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(LOAD_DATA))
			{
				StatsSet sevenDat = null;
				int charObjId;
				while (rs.next())
				{
					charObjId = rs.getInt("charId");
					
					sevenDat = new StatsSet();
					sevenDat.set("charId", charObjId);
					sevenDat.set("cabal", rs.getString("cabal"));
					sevenDat.set("seal", rs.getInt("seal"));
					sevenDat.set("red_stones", rs.getInt("red_stones"));
					sevenDat.set("green_stones", rs.getInt("green_stones"));
					sevenDat.set("blue_stones", rs.getInt("blue_stones"));
					sevenDat.set("ancient_adena_amount", rs.getDouble("ancient_adena_amount"));
					sevenDat.set("contribution_score", rs.getDouble("contribution_score"));
					_signsPlayerData.put(charObjId, sevenDat);
				}
			}
			
			try (Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(LOAD_STATUS))
			{
				
				while (rs.next())
				{
					_currentCycle = rs.getInt("current_cycle");
					_activePeriod = rs.getInt("active_period");
					_previousWinner = rs.getInt("previous_winner");
					
					_dawnStoneScore = rs.getDouble("dawn_stone_score");
					_dawnFestivalScore = rs.getInt("dawn_festival_score");
					_duskStoneScore = rs.getDouble("dusk_stone_score");
					_duskFestivalScore = rs.getInt("dusk_festival_score");
					
					_signsSealOwners.put(SEAL_AVARICE, rs.getInt("avarice_owner"));
					_signsSealOwners.put(SEAL_GNOSIS, rs.getInt("gnosis_owner"));
					_signsSealOwners.put(SEAL_STRIFE, rs.getInt("strife_owner"));
					
					_signsDawnSealTotals.put(SEAL_AVARICE, rs.getInt("avarice_dawn_score"));
					_signsDawnSealTotals.put(SEAL_GNOSIS, rs.getInt("gnosis_dawn_score"));
					_signsDawnSealTotals.put(SEAL_STRIFE, rs.getInt("strife_dawn_score"));
					_signsDuskSealTotals.put(SEAL_AVARICE, rs.getInt("avarice_dusk_score"));
					_signsDuskSealTotals.put(SEAL_GNOSIS, rs.getInt("gnosis_dusk_score"));
					_signsDuskSealTotals.put(SEAL_STRIFE, rs.getInt("strife_dusk_score"));
					
					_lastSave.setTimeInMillis(rs.getLong("date"));
				}
			}
		}
		catch (SQLException e)
		{
			_log.error("SevenSigns: Unable to load Seven Signs data from database!", e);
		}
		// Festival data is loaded now after the Seven Signs engine data.
	}
	
	/**
	 * Saves all Seven Signs player data.<br>
	 * Should be called on period change and shutdown only.
	 */
	public void saveSevenSignsData()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_PLAYER))
		{
			for (StatsSet sevenDat : _signsPlayerData.values())
			{
				ps.setString(1, sevenDat.getString("cabal"));
				ps.setInt(2, sevenDat.getInt("seal"));
				ps.setInt(3, sevenDat.getInt("red_stones"));
				ps.setInt(4, sevenDat.getInt("green_stones"));
				ps.setInt(5, sevenDat.getInt("blue_stones"));
				ps.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
				ps.setDouble(7, sevenDat.getDouble("contribution_score"));
				ps.setInt(8, sevenDat.getInt("charId"));
				ps.addBatch();
			}
			ps.executeBatch();
		}
		catch (SQLException e)
		{
			_log.error("SevenSigns: Unable to save data to database!", e);
		}
	}
	
	public final void saveSevenSignsData(int objectId)
	{
		StatsSet sevenDat = _signsPlayerData.get(objectId);
		if (sevenDat == null)
		{
			return;
		}
		
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_PLAYER))
		{
			ps.setString(1, sevenDat.getString("cabal"));
			ps.setInt(2, sevenDat.getInt("seal"));
			ps.setInt(3, sevenDat.getInt("red_stones"));
			ps.setInt(4, sevenDat.getInt("green_stones"));
			ps.setInt(5, sevenDat.getInt("blue_stones"));
			ps.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
			ps.setDouble(7, sevenDat.getDouble("contribution_score"));
			ps.setInt(8, sevenDat.getInt("charId"));
			ps.execute();
		}
		catch (SQLException e)
		{
			_log.error("SevenSigns: Unable to save data to database!", e);
		}
	}
	
	public final void saveSevenSignsStatus()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_STATUS))
		{
			ps.setInt(1, _currentCycle);
			ps.setInt(2, _activePeriod);
			ps.setInt(3, _previousWinner);
			ps.setDouble(4, _dawnStoneScore);
			ps.setInt(5, _dawnFestivalScore);
			ps.setDouble(6, _duskStoneScore);
			ps.setInt(7, _duskFestivalScore);
			ps.setInt(8, _signsSealOwners.get(SEAL_AVARICE));
			ps.setInt(9, _signsSealOwners.get(SEAL_GNOSIS));
			ps.setInt(10, _signsSealOwners.get(SEAL_STRIFE));
			ps.setInt(11, _signsDawnSealTotals.get(SEAL_AVARICE));
			ps.setInt(12, _signsDawnSealTotals.get(SEAL_GNOSIS));
			ps.setInt(13, _signsDawnSealTotals.get(SEAL_STRIFE));
			ps.setInt(14, _signsDuskSealTotals.get(SEAL_AVARICE));
			ps.setInt(15, _signsDuskSealTotals.get(SEAL_GNOSIS));
			ps.setInt(16, _signsDuskSealTotals.get(SEAL_STRIFE));
			ps.setInt(17, SevenSignsFestival.getInstance().getCurrentFestivalCycle());
			
			for (int i = 0; i < SevenSignsFestival.FESTIVAL_COUNT; i++)
			{
				ps.setInt(18 + i, SevenSignsFestival.getInstance().getAccumulatedBonus(i));
			}
			_lastSave = Calendar.getInstance();
			ps.setLong(18 + SevenSignsFestival.FESTIVAL_COUNT, _lastSave.getTimeInMillis());
			ps.execute();
		}
		catch (SQLException e)
		{
			_log.error("SevenSigns: Unable to save data to database!", e);
		}
	}
	
	/**
	 * Used to reset the cabal details of all players, and update the database.<BR>
	 * Primarily used when beginning a new cycle, and should otherwise never be called.
	 */
	protected void resetPlayerData()
	{
		// Reset each player's contribution data as well as seal and cabal.
		for (StatsSet sevenDat : _signsPlayerData.values())
		{
			// Reset the player's cabal and seal information
			sevenDat.set("cabal", "");
			sevenDat.set("seal", SEAL_NULL);
			sevenDat.set("contribution_score", 0);
		}
	}
	
	/**
	 * Used to specify cabal-related details for the specified player.<br>
	 * This method checks to see if the player has registered before and will update the database if necessary.
	 * @param objectId
	 * @param chosenCabal
	 * @param chosenSeal
	 * @return the cabal ID the player has joined.
	 */
	public int setPlayerInfo(int objectId, int chosenCabal, int chosenSeal)
	{
		StatsSet currPlayerData = _signsPlayerData.get(objectId);
		
		if (currPlayerData != null)
		{
			// If the seal validation period has passed,
			// cabal information was removed and so "re-register" player
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			
			_signsPlayerData.put(objectId, currPlayerData);
		}
		else
		{
			currPlayerData = new StatsSet();
			currPlayerData.set("charId", objectId);
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			currPlayerData.set("red_stones", 0);
			currPlayerData.set("green_stones", 0);
			currPlayerData.set("blue_stones", 0);
			currPlayerData.set("ancient_adena_amount", 0);
			currPlayerData.set("contribution_score", 0);
			
			_signsPlayerData.put(objectId, currPlayerData);
			
			// Update data in database, as we have a new player signing up.
			try (Connection con = ConnectionFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(INSERT_PLAYER))
			{
				ps.setInt(1, objectId);
				ps.setString(2, getCabalShortName(chosenCabal));
				ps.setInt(3, chosenSeal);
				ps.execute();
			}
			catch (SQLException e)
			{
				_log.error("SevenSigns: Failed to save data!", e);
			}
		}
		
		// Increasing Seal total score for the player chosen Seal.
		if ("dawn".equals(currPlayerData.getString("cabal")))
		{
			_signsDawnSealTotals.put(chosenSeal, _signsDawnSealTotals.get(chosenSeal) + 1);
		}
		else
		{
			_signsDuskSealTotals.put(chosenSeal, _signsDuskSealTotals.get(chosenSeal) + 1);
		}
		
		if (!Config.ALT_SEVENSIGNS_LAZY_UPDATE)
		{
			saveSevenSignsStatus();
		}
		
		return chosenCabal;
	}
	
	/**
	 * Returns the amount of ancient adena the specified player can claim, if any.<BR>
	 * If removeReward = True, all the ancient adena owed to them is removed, then DB is updated.
	 * @param objectId
	 * @param removeReward
	 * @return
	 */
	public int getAncientAdenaReward(int objectId, boolean removeReward)
	{
		StatsSet currPlayer = _signsPlayerData.get(objectId);
		int rewardAmount = currPlayer.getInt("ancient_adena_amount");
		
		currPlayer.set("red_stones", 0);
		currPlayer.set("green_stones", 0);
		currPlayer.set("blue_stones", 0);
		currPlayer.set("ancient_adena_amount", 0);
		
		if (removeReward)
		{
			_signsPlayerData.put(objectId, currPlayer);
			if (!Config.ALT_SEVENSIGNS_LAZY_UPDATE)
			{
				saveSevenSignsData(objectId);
				saveSevenSignsStatus();
			}
		}
		
		return rewardAmount;
	}
	
	/**
	 * Used to add the specified player's seal stone contribution points to the current total for their cabal.<br>
	 * Returns the point score the contribution was worth.<br>
	 * Each stone count <B>must be</B> broken down and specified by the stone's color.
	 * @param objectId
	 * @param blueCount
	 * @param greenCount
	 * @param redCount
	 * @return
	 */
	public long addPlayerStoneContrib(int objectId, long blueCount, long greenCount, long redCount)
	{
		StatsSet currPlayer = _signsPlayerData.get(objectId);
		
		long contribScore = calcContributionScore(blueCount, greenCount, redCount);
		long totalAncientAdena = currPlayer.getLong("ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
		long totalContribScore = currPlayer.getLong("contribution_score") + contribScore;
		
		if (totalContribScore > Config.ALT_MAXIMUM_PLAYER_CONTRIB)
		{
			return -1;
		}
		
		currPlayer.set("red_stones", currPlayer.getInt("red_stones") + redCount);
		currPlayer.set("green_stones", currPlayer.getInt("green_stones") + greenCount);
		currPlayer.set("blue_stones", currPlayer.getInt("blue_stones") + blueCount);
		currPlayer.set("ancient_adena_amount", totalAncientAdena);
		currPlayer.set("contribution_score", totalContribScore);
		_signsPlayerData.put(objectId, currPlayer);
		
		switch (getPlayerCabal(objectId))
		{
			case CABAL_DAWN:
				_dawnStoneScore += contribScore;
				break;
			case CABAL_DUSK:
				_duskStoneScore += contribScore;
				break;
		}
		
		if (!Config.ALT_SEVENSIGNS_LAZY_UPDATE)
		{
			saveSevenSignsData(objectId);
			saveSevenSignsStatus();
		}
		
		return contribScore;
	}
	
	/**
	 * Adds the specified number of festival points to the specified cabal.<br>
	 * Remember, the same number of points are <b>deducted from the rival cabal</b> to maintain proportionality.
	 * @param cabal
	 * @param amount
	 */
	public void addFestivalScore(int cabal, int amount)
	{
		if (cabal == CABAL_DUSK)
		{
			_duskFestivalScore += amount;
			
			// To prevent negative scores!
			if (_dawnFestivalScore >= amount)
			{
				_dawnFestivalScore -= amount;
			}
		}
		else
		{
			_dawnFestivalScore += amount;
			
			if (_duskFestivalScore >= amount)
			{
				_duskFestivalScore -= amount;
			}
		}
	}
	
	/**
	 * Send info on the current Seven Signs period to the specified player.
	 * @param player
	 */
	public void sendCurrentPeriodMsg(L2PcInstance player)
	{
		SystemMessage sm = null;
		
		switch (getCurrentPeriod())
		{
			case PERIOD_COMP_RECRUITING:
				sm = SystemMessage.getSystemMessage(SystemMessageId.PREPARATIONS_PERIOD_BEGUN);
				break;
			case PERIOD_COMPETITION:
				sm = SystemMessage.getSystemMessage(SystemMessageId.COMPETITION_PERIOD_BEGUN);
				break;
			case PERIOD_COMP_RESULTS:
				sm = SystemMessage.getSystemMessage(SystemMessageId.RESULTS_PERIOD_BEGUN);
				break;
			case PERIOD_SEAL_VALIDATION:
				sm = SystemMessage.getSystemMessage(SystemMessageId.VALIDATION_PERIOD_BEGUN);
				break;
		}
		
		player.sendPacket(sm);
	}
	
	/**
	 * Sends the built-in system message specified by sysMsgId to all online players.
	 * @param sysMsgId
	 */
	public void sendMessageToAll(SystemMessageId sysMsgId)
	{
		Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(sysMsgId));
	}
	
	/**
	 * Used to initialize the seals for each cabal.<bR>
	 * (Used at startup or at beginning of a new cycle).<br>
	 * This method should be called after <b>resetSeals()</b> and <b>calcNewSealOwners()</b> on a new cycle.
	 */
	protected void initializeSeals()
	{
		for (Entry<Integer, Integer> e : _signsSealOwners.entrySet())
		{
			if (e.getValue() != CABAL_NULL)
			{
				if (isSealValidationPeriod())
				{
					_log.info("SevenSigns: The {} have won the {}.", getCabalName(e.getValue()), getSealName(e.getKey(), false));
				}
				else
				{
					_log.info("SevenSigns: The {} is currently owned by {}.", getSealName(e.getKey(), false), getCabalName(e.getValue()));
				}
			}
			else
			{
				_log.info("SevenSigns: The {} remains unclaimed.", getSealName(e.getKey(), false));
			}
		}
	}
	
	/**
	 * Only really used at the beginning of a new cycle, this method resets all seal-related data.
	 */
	protected void resetSeals()
	{
		_signsDawnSealTotals.put(SEAL_AVARICE, 0);
		_signsDawnSealTotals.put(SEAL_GNOSIS, 0);
		_signsDawnSealTotals.put(SEAL_STRIFE, 0);
		_signsDuskSealTotals.put(SEAL_AVARICE, 0);
		_signsDuskSealTotals.put(SEAL_GNOSIS, 0);
		_signsDuskSealTotals.put(SEAL_STRIFE, 0);
	}
	
	/**
	 * Calculates the ownership of the three Seals of the Seven Signs, based on various criterion. <BR>
	 * <BR>
	 * Should only ever called at the beginning of a new cycle.
	 */
	protected void calcNewSealOwners()
	{
		for (Integer currSeal : _signsDawnSealTotals.keySet())
		{
			int prevSealOwner = _signsSealOwners.get(currSeal);
			int newSealOwner = CABAL_NULL;
			int dawnProportion = getSealProportion(currSeal, CABAL_DAWN);
			int totalDawnMembers = getTotalMembers(CABAL_DAWN) == 0 ? 1 : getTotalMembers(CABAL_DAWN);
			int dawnPercent = Math.round(((float) dawnProportion / (float) totalDawnMembers) * 100);
			int duskProportion = getSealProportion(currSeal, CABAL_DUSK);
			int totalDuskMembers = getTotalMembers(CABAL_DUSK) == 0 ? 1 : getTotalMembers(CABAL_DUSK);
			int duskPercent = Math.round(((float) duskProportion / (float) totalDuskMembers) * 100);
			
			/**
			 * If a Seal was already closed or owned by the opponent and the new winner wants to assume ownership of the Seal, 35% or more of the members of the Cabal must have chosen the Seal.<br>
			 * If they chose less than 35%, they cannot own the Seal.<br>
			 * If the Seal was owned by the winner in the previous Seven Signs, they can retain that seal if 10% or more members have chosen it.<br>
			 * If they want to possess a new Seal, at least 35% of the members of the Cabal must have chosen the new Seal.
			 */
			switch (prevSealOwner)
			{
				case CABAL_NULL:
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
							newSealOwner = CABAL_NULL;
							break;
						case CABAL_DAWN:
							if (dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if (duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
				case CABAL_DAWN:
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
							if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DAWN:
							if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if (duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
				case CABAL_DUSK:
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
							if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DAWN:
							if (dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
			}
			
			_signsSealOwners.put(currSeal, newSealOwner);
			
			// Alert all online players to new seal status.
			switch (currSeal)
			{
				case SEAL_AVARICE:
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_AVARICE);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_AVARICE);
					}
					break;
				case SEAL_GNOSIS:
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_GNOSIS);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_GNOSIS);
					}
					break;
				case SEAL_STRIFE:
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_STRIFE);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_STRIFE);
					}
					
					CastleManager.getInstance().validateTaxes(newSealOwner);
					break;
			}
		}
	}
	
	/**
	 * This method is called to remove all players from catacombs and necropolises, who belong to the losing cabal.<br>
	 * Should only ever called at the beginning of Seal Validation.
	 * @param compWinner
	 */
	protected void teleLosingCabalFromDungeons(String compWinner)
	{
		for (L2PcInstance player : L2World.getInstance().getPlayers())
		{
			StatsSet currPlayer = _signsPlayerData.get(player.getObjectId());
			
			if (isSealValidationPeriod() || isCompResultsPeriod())
			{
				if (!player.isGM() && player.isIn7sDungeon() && ((currPlayer == null) || !currPlayer.getString("cabal").equals(compWinner)))
				{
					player.teleToLocation(TeleportWhereType.TOWN);
					player.setIsIn7sDungeon(false);
					player.sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
				}
			}
			else
			{
				if (!player.isGM() && player.isIn7sDungeon() && ((currPlayer == null) || !currPlayer.getString("cabal").isEmpty()))
				{
					player.teleToLocation(TeleportWhereType.TOWN);
					player.setIsIn7sDungeon(false);
					player.sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
				}
			}
		}
	}
	
	/**
	 * The primary controller of period change of the Seven Signs system.<br>
	 * This runs all related tasks depending on the period that is about to begin.
	 * @author Tempy
	 */
	protected class SevenSignsPeriodChange implements Runnable
	{
		@Override
		public void run()
		{
			// Remember the period check here refers to the period just ENDED!
			final int periodEnded = getCurrentPeriod();
			_activePeriod++;
			
			switch (periodEnded)
			{
				case PERIOD_COMP_RECRUITING: // Initialization
					
					// Start the Festival of Darkness cycle.
					SevenSignsFestival.getInstance().startFestivalManager();
					
					// Send message that Competition has begun.
					sendMessageToAll(SystemMessageId.QUEST_EVENT_PERIOD_BEGUN);
					break;
				case PERIOD_COMPETITION: // Results Calculation
					
					// Send message that Competition has ended.
					sendMessageToAll(SystemMessageId.QUEST_EVENT_PERIOD_ENDED);
					
					int compWinner = getCabalHighestScore();
					
					// Schedule a stop of the festival engine and reward highest ranking members from cycle
					SevenSignsFestival.getInstance().getFestivalManagerSchedule().cancel(false);
					SevenSignsFestival.getInstance().rewardHighestRanked();
					
					calcNewSealOwners();
					
					switch (compWinner)
					{
						case CABAL_DAWN:
							sendMessageToAll(SystemMessageId.DAWN_WON);
							break;
						case CABAL_DUSK:
							sendMessageToAll(SystemMessageId.DUSK_WON);
							break;
					}
					
					_previousWinner = compWinner;
					// Reset Castle ticket buy count
					List<Castle> castles = CastleManager.getInstance().getCastles();
					for (Castle castle : castles)
					{
						castle.setTicketBuyCount(0);
					}
					break;
				case PERIOD_COMP_RESULTS: // Seal Validation
					
					// Perform initial Seal Validation set up.
					initializeSeals();
					
					// Buff/Debuff members of the event when Seal of Strife captured.
					giveCPMult(getSealOwner(SEAL_STRIFE));
					
					// Send message that Seal Validation has begun.
					sendMessageToAll(SystemMessageId.SEAL_VALIDATION_PERIOD_BEGUN);
					
					_log.info("SevenSigns: The {} have won the competition with {} points!", getCabalName(_previousWinner), getCurrentScore(_previousWinner));
					break;
				case PERIOD_SEAL_VALIDATION: // Reset for New Cycle
					
					// Ensure a cycle restart when this period ends.
					_activePeriod = PERIOD_COMP_RECRUITING;
					
					// Send message that Seal Validation has ended.
					sendMessageToAll(SystemMessageId.SEAL_VALIDATION_PERIOD_ENDED);
					// Clear Seal of Strife influence.
					removeCPMult();
					// Reset all data
					resetPlayerData();
					resetSeals();
					
					_currentCycle++;
					// Reset all Festival-related data and remove any unused blood offerings.
					// NOTE: A full update of Festival data in the database is also performed.
					SevenSignsFestival.getInstance().resetFestivalData(false);
					
					_dawnStoneScore = 0;
					_duskStoneScore = 0;
					
					_dawnFestivalScore = 0;
					_duskFestivalScore = 0;
					break;
			}
			
			// Make sure all Seven Signs data is saved for future use.
			saveSevenSignsData();
			saveSevenSignsStatus();
			
			teleLosingCabalFromDungeons(getCabalShortName(getCabalHighestScore()));
			
			SSQInfo ss = new SSQInfo();
			
			Broadcast.toAllOnlinePlayers(ss);
			spawnSevenSignsNPC();
			
			_log.info("SevenSigns: The {} period has begun!", getCurrentPeriodName());
			
			setCalendarForNextPeriodChange();
			
			SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
			ThreadPoolManager.getInstance().scheduleGeneral(sspc, getMilliToPeriodChange());
		}
	}
	
	public boolean checkIsDawnPostingTicket(int itemId)
	{
		// TODO: I think it should be some kind of a list in the datapack for compare.
		if ((itemId > 6114) && (itemId < 6175))
		{
			return true;
		}
		if ((itemId > 6801) && (itemId < 6812))
		{
			return true;
		}
		if ((itemId > 7997) && (itemId < 8008))
		{
			return true;
		}
		if ((itemId > 7940) && (itemId < 7951))
		{
			return true;
		}
		if ((itemId > 6294) && (itemId < 6307))
		{
			return true;
		}
		if ((itemId > 6831) && (itemId < 6834))
		{
			return true;
		}
		if ((itemId > 8027) && (itemId < 8030))
		{
			return true;
		}
		if ((itemId > 7970) && (itemId < 7973))
		{
			return true;
		}
		return false;
	}
	
	public boolean checkIsRookiePostingTicket(int itemId)
	{
		// TODO: I think it should be some kind of a list in the datapack for compare.
		if ((itemId > 6174) && (itemId < 6295))
		{
			return true;
		}
		if ((itemId > 6811) && (itemId < 6832))
		{
			return true;
		}
		if ((itemId > 7950) && (itemId < 7971))
		{
			return true;
		}
		if ((itemId > 8007) && (itemId < 8028))
		{
			return true;
		}
		return false;
	}
	
	public void giveCPMult(int strifeOwner)
	{
		for (L2PcInstance player : L2World.getInstance().getPlayers())
		{
			// Gives "Victor of War" passive skill to all online characters with Cabal, which controls Seal of Strife
			int cabal = getPlayerCabal(player.getObjectId());
			if (cabal != SevenSigns.CABAL_NULL)
			{
				if (cabal == strifeOwner)
				{
					player.addSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
				}
				else
				{
					// Gives "The Vanquished of War" passive skill to all online characters with Cabal, which does not control Seal of Strife
					player.addSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
				}
			}
		}
	}
	
	public void removeCPMult()
	{
		// Remove SevenSigns' buffs/debuffs.
		for (L2PcInstance player : L2World.getInstance().getPlayers())
		{
			player.removeSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
			player.removeSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
		}
	}
	
	public boolean checkSummonConditions(L2PcInstance activeChar)
	{
		if (activeChar == null)
		{
			return true;
		}
		// Golems cannot be summoned by Dusk when the Seal of Strife is controlled by the Dawn
		if (isSealValidationPeriod())
		{
			if (getSealOwner(SEAL_STRIFE) == CABAL_DAWN)
			{
				if (getPlayerCabal(activeChar.getObjectId()) == CABAL_DUSK)
				{
					activeChar.sendPacket(SystemMessageId.SEAL_OF_STRIFE_FORBIDS_SUMMONING);
					return true;
				}
			}
		}
		
		return false;
	}
	
	private static class SingletonHolder
	{
		protected static final SevenSigns _instance = new SevenSigns();
	}
}
