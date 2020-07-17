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
package com.l2jserver.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.Config;
import com.l2jserver.commons.database.pool.impl.ConnectionFactory;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.util.Rnd;

/**
 * @author n0nam3
 */
public class FishingChampionshipManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(FishingChampionshipManager.class.getName());
	
	private static final String SELECT = "SELECT `player_name`, `fish_length`, `rewarded` FROM fishing_championship";
	private static final String INSERT = "INSERT INTO fishing_championship(player_name,fish_length,rewarded) VALUES (?,?,?)";
	private static final String DELETE = "DELETE FROM fishing_championship";
	
	private static final FishingChampionshipManager _instance = new FishingChampionshipManager();
	
	protected long _endDate = 0;
	
	protected final List<String> _playersName = new ArrayList<>();
	protected final List<String> _fishLength = new ArrayList<>();
	protected final List<String> _winPlayersName = new ArrayList<>();
	protected final List<String> _winFishLength = new ArrayList<>();
	protected final List<Fisher> _tmpPlayers = new ArrayList<>();
	protected final List<Fisher> _winPlayers = new ArrayList<>();
	protected double _minFishLength = 0;
	protected boolean _needRefresh = true;
	
	private FishingChampionshipManager()
	{
		restoreData();
		refreshWinResult();
		recalculateMinLength();
		
		if (_endDate <= System.currentTimeMillis())
		{
			_endDate = System.currentTimeMillis();
			new FinishChamp().run();
		}
		else
		{
			ThreadPoolManager.getInstance().scheduleEvent(new FinishChamp(), _endDate - System.currentTimeMillis());
		}
	}
	
	public static FishingChampionshipManager getInstance()
	{
		return _instance;
	}
	
	public String getCurrentFishLength(int par)
	{
		if (_fishLength.size() >= par)
		{
			return _fishLength.get(par - 1);
		}
		return "0";
	}
	
	public String getCurrentName(int par)
	{
		if (_playersName.size() >= par)
		{
			return _playersName.get(par - 1);
		}
		return "None";
	}
	
	public String getFishLength(int par)
	{
		if (_winFishLength.size() >= par)
		{
			return _winFishLength.get(par - 1);
		}
		return "0";
	}
	
	public void getReward(L2PcInstance player)
	{
		for (Fisher fisher : _winPlayers)
		{
			if (fisher.getName().equalsIgnoreCase(player.getName()))
			{
				if (fisher.getRewardType() != 2)
				{
					int rewardCount = 0;
					for (int i = 0; i < _winPlayersName.size(); i++)
					{
						if (_winPlayersName.get(i).equalsIgnoreCase(player.getName()))
						{
							switch (i)
							{
								case 0:
									rewardCount = Config.ALT_FISH_CHAMPIONSHIP_REWARD_1;
									break;
								case 1:
									rewardCount = Config.ALT_FISH_CHAMPIONSHIP_REWARD_2;
									break;
								case 2:
									rewardCount = Config.ALT_FISH_CHAMPIONSHIP_REWARD_3;
									break;
								case 3:
									rewardCount = Config.ALT_FISH_CHAMPIONSHIP_REWARD_4;
									break;
								case 4:
									rewardCount = Config.ALT_FISH_CHAMPIONSHIP_REWARD_5;
									break;
							}
						}
					}
					fisher.setRewardType(2);
					if (rewardCount > 0)
					{
						player.addItem("fishing_reward", Config.ALT_FISH_CHAMPIONSHIP_REWARD_ITEM, rewardCount, null, true);
						final NpcHtmlMessage html = new NpcHtmlMessage();
						html.setFile(player.getHtmlPrefix(), "data/scripts/ai/npc/Fisherman/fish_event_reward001.htm");
						player.sendPacket(html);
					}
				}
			}
		}
	}
	
	public String getTimeRemaining()
	{
		return formatTime(_endDate - System.currentTimeMillis());
	}
	
	public String getWinnerName(int par)
	{
		if (_winPlayersName.size() >= par)
		{
			return _winPlayersName.get(par - 1);
		}
		return "None";
	}
	
	public boolean isWinner(String playerName)
	{
		for (String name : _winPlayersName)
		{
			if (name.equals(playerName))
			{
				return true;
			}
		}
		return false;
	}
	
	public synchronized void newFish(L2PcInstance player, int lureId)
	{
		if (!Config.ALT_FISH_CHAMPIONSHIP_ENABLED)
		{
			return;
		}
		
		double length = Rnd.get(60, 89) + (Rnd.get(0, 1000) / 1000.);
		if ((lureId >= 8484) && (lureId <= 8486))
		{
			length += Rnd.get(0, 3000) / 1000.;
		}
		
		player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CAUGHT_FISH_S1_LENGTH).addString(String.valueOf(length)));
		
		if (_tmpPlayers.size() < 5)
		{
			for (Fisher fisher : _tmpPlayers)
			{
				if (fisher.getName().equalsIgnoreCase(player.getName()))
				{
					if (fisher.getLength() < length)
					{
						fisher.setLength(length);
						player.sendPacket(SystemMessageId.REGISTERED_IN_FISH_SIZE_RANKING);
						recalculateMinLength();
					}
					return;
				}
			}
			_tmpPlayers.add(new Fisher(player.getName(), length, 0));
			player.sendPacket(SystemMessageId.REGISTERED_IN_FISH_SIZE_RANKING);
			recalculateMinLength();
		}
		else if (_minFishLength < length)
		{
			for (Fisher fisher : _tmpPlayers)
			{
				if (fisher.getName().equalsIgnoreCase(player.getName()))
				{
					if (fisher.getLength() < length)
					{
						fisher.setLength(length);
						player.sendPacket(SystemMessageId.REGISTERED_IN_FISH_SIZE_RANKING);
						recalculateMinLength();
					}
					return;
				}
			}
			
			Fisher minFisher = null;
			double minLength = 99999.;
			for (Fisher fisher : _tmpPlayers)
			{
				if (fisher.getLength() < minLength)
				{
					minFisher = fisher;
					minLength = minFisher.getLength();
				}
			}
			_tmpPlayers.remove(minFisher);
			_tmpPlayers.add(new Fisher(player.getName(), length, 0));
			player.sendPacket(SystemMessageId.REGISTERED_IN_FISH_SIZE_RANKING);
			recalculateMinLength();
		}
	}
	
	public void showChampScreen(L2PcInstance player, L2NpcInstance npc)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player.getHtmlPrefix(), "data/scripts/ai/npc/Fisherman/fish_event001.htm");
		
		String string = null;
		for (int x = 1; x <= 5; x++)
		{
			string += "<tr><td width=70 align=center>" + x + "</td>";
			string += "<td width=110 align=center>" + getWinnerName(x) + "</td>";
			string += "<td width=80 align=center>" + getFishLength(x) + "</td></tr>";
		}
		html.replace("%TABLE%", string);
		html.replace("%prizeItem%", ItemTable.getInstance().getTemplate(Config.ALT_FISH_CHAMPIONSHIP_REWARD_ITEM).getName());
		html.replace("%prizeFirst%", String.valueOf(Config.ALT_FISH_CHAMPIONSHIP_REWARD_1));
		html.replace("%prizeTwo%", String.valueOf(Config.ALT_FISH_CHAMPIONSHIP_REWARD_2));
		html.replace("%prizeThree%", String.valueOf(Config.ALT_FISH_CHAMPIONSHIP_REWARD_3));
		html.replace("%prizeFour%", String.valueOf(Config.ALT_FISH_CHAMPIONSHIP_REWARD_4));
		html.replace("%prizeFive%", String.valueOf(Config.ALT_FISH_CHAMPIONSHIP_REWARD_5));
		html.replace("%refresh%", getTimeRemaining());
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		player.sendPacket(html);
	}
	
	public void showMidResult(L2PcInstance player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		
		if (_needRefresh)
		{
			html.setFile(player.getHtmlPrefix(), "data/scripts/ai/npc/Fisherman/fish_event003.htm");
			player.sendPacket(html);
			refreshResult();
			ThreadPoolManager.getInstance().scheduleEvent(new NeedRefresh(), 60000);
			return;
		}
		html.setFile(player.getHtmlPrefix(), "data/scripts/ai/npc/Fisherman/fish_event002.htm");
		
		String string = null;
		for (int i = 1; i <= 5; i++)
		{
			string += "<tr><td width=70 align=center>" + i + "</td>";
			string += "<td width=110 align=center>" + getCurrentName(i) + "</td>";
			string += "<td width=80 align=center>" + getCurrentFishLength(i) + "</td></tr>";
		}
		html.replace("%TABLE%", string);
		html.replace("%prizeItem%", ItemTable.getInstance().getTemplate(Config.ALT_FISH_CHAMPIONSHIP_REWARD_ITEM).getName());
		html.replace("%prizeFirst%", String.valueOf(Config.ALT_FISH_CHAMPIONSHIP_REWARD_1));
		html.replace("%prizeTwo%", String.valueOf(Config.ALT_FISH_CHAMPIONSHIP_REWARD_2));
		html.replace("%prizeThree%", String.valueOf(Config.ALT_FISH_CHAMPIONSHIP_REWARD_3));
		html.replace("%prizeFour%", String.valueOf(Config.ALT_FISH_CHAMPIONSHIP_REWARD_4));
		html.replace("%prizeFive%", String.valueOf(Config.ALT_FISH_CHAMPIONSHIP_REWARD_5));
		player.sendPacket(html);
	}
	
	public void shutdown()
	{
		GlobalVariablesManager.getInstance().set("fishChampionshipEnd", _endDate);
		
		try (Connection con = ConnectionFactory.getInstance().getConnection())
		{
			PreparedStatement ps;
			try
			{
				ps = con.prepareStatement(DELETE);
				ps.execute();
				ps.close();
			}
			catch (SQLException e)
			{
				LOG.warn("{}: can't delete informations: {} {}", FishingChampionshipManager.class.getSimpleName(), e.getMessage(), e);
			}
			
			for (Fisher fisher : _winPlayers)
			{
				ps = con.prepareStatement(INSERT);
				ps.setString(1, fisher.getName());
				ps.setDouble(2, fisher.getLength());
				ps.setInt(3, fisher.getRewardType());
				ps.execute();
				ps.close();
			}
			for (Fisher fisher : _tmpPlayers)
			{
				ps = con.prepareStatement(INSERT);
				ps.setString(1, fisher.getName());
				ps.setDouble(2, fisher.getLength());
				ps.setInt(3, 0);
				ps.execute();
				ps.close();
			}
		}
		catch (SQLException e)
		{
			LOG.warn("{}: can't update informations: {} {}", FishingChampionshipManager.class.getSimpleName(), e.getMessage(), e);
		}
	}
	
	protected void refreshWinResult()
	{
		_winPlayersName.clear();
		_winFishLength.clear();
		
		Fisher fisher1;
		Fisher fisher2;
		
		for (int i = 0; i <= (_winPlayers.size() - 1); i++)
		{
			for (int j = 0; j <= (_winPlayers.size() - 2); j++)
			{
				fisher1 = _winPlayers.get(j);
				fisher2 = _winPlayers.get(j + 1);
				if (fisher1.getLength() < fisher2.getLength())
				{
					_winPlayers.set(j, fisher2);
					_winPlayers.set(j + 1, fisher1);
				}
			}
		}
		
		for (int i = 0; i <= (_winPlayers.size() - 1); i++)
		{
			_winPlayersName.add(_winPlayers.get(i).getName());
			_winFishLength.add(String.valueOf(_winPlayers.get(i).getLength()));
		}
	}
	
	protected void setEndOfChamp()
	{
		final Calendar finishTime = Calendar.getInstance();
		finishTime.setTimeInMillis(_endDate);
		finishTime.set(Calendar.MINUTE, 0);
		finishTime.set(Calendar.SECOND, 0);
		finishTime.add(Calendar.DAY_OF_MONTH, 6);
		finishTime.set(Calendar.DAY_OF_WEEK, 3);
		finishTime.set(Calendar.HOUR_OF_DAY, 19);
		_endDate = finishTime.getTimeInMillis();
	}
	
	private String formatTime(long time)
	{
		if (time == 0)
		{
			return "now";
		}
		time = Math.abs(time);
		String ret = "";
		final long numDays = time / 86400000;
		time -= numDays * 86400000;
		final long numHours = time / 3600000;
		time -= numHours * 3600000;
		final long numMins = time / 60000;
		time -= numMins * 60000;
		final long numSeconds = time / 1000;
		if (numDays > 0)
		{
			ret += numDays + "d ";
		}
		if (numHours > 0)
		{
			ret += numHours + "h ";
		}
		if (numMins > 0)
		{
			ret += numMins + "m ";
		}
		if (numSeconds > 0)
		{
			ret += numSeconds + "s";
		}
		return ret.trim();
	}
	
	private void recalculateMinLength()
	{
		double minLen = 99999.;
		for (Fisher fisher : _tmpPlayers)
		{
			if (fisher.getLength() < minLen)
			{
				minLen = fisher.getLength();
			}
		}
		_minFishLength = minLen;
	}
	
	private synchronized void refreshResult()
	{
		_needRefresh = false;
		
		_playersName.clear();
		_fishLength.clear();
		
		Fisher fisher1;
		Fisher fisher2;
		
		for (int i = 0; i <= (_tmpPlayers.size() - 1); i++)
		{
			for (int j = 0; j <= (_tmpPlayers.size() - 2); j++)
			{
				fisher1 = _tmpPlayers.get(j);
				fisher2 = _tmpPlayers.get(j + 1);
				if (fisher1.getLength() < fisher2.getLength())
				{
					_tmpPlayers.set(j, fisher2);
					_tmpPlayers.set(j + 1, fisher1);
				}
			}
		}
		
		for (int i = 0; i <= (_tmpPlayers.size() - 1); i++)
		{
			_playersName.add(_tmpPlayers.get(i).getName());
			_fishLength.add(String.valueOf(_tmpPlayers.get(i).getLength()));
		}
	}
	
	private void restoreData()
	{
		_endDate = GlobalVariablesManager.getInstance().getLong("fishChampionshipEnd", 0);
		
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final int rewarded = rs.getInt("rewarded");
				if (rewarded == 0)
				{
					_tmpPlayers.add(new Fisher(rs.getString("player_name"), rs.getDouble("fish_length"), 0));
				}
				else if (rewarded > 0)
				{
					_winPlayers.add(new Fisher(rs.getString("player_name"), rs.getDouble("fish_length"), rewarded));
				}
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warn("{}: can't restore fishing championship info: {} {}", FishingChampionshipManager.class.getSimpleName(), e.getMessage(), e);
		}
	}
	
	private class FinishChamp implements Runnable
	{
		protected FinishChamp()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			_winPlayers.clear();
			for (Fisher fisher : _tmpPlayers)
			{
				fisher.setRewardType(1);
				_winPlayers.add(fisher);
			}
			_tmpPlayers.clear();
			
			refreshWinResult();
			setEndOfChamp();
			shutdown();
			
			LOG.info("{}: New event period start", FishingChampionshipManager.class.getSimpleName());
			ThreadPoolManager.getInstance().scheduleEvent(new FinishChamp(), _endDate - System.currentTimeMillis());
		}
	}
	
	private class Fisher
	{
		private double _length;
		private final String _name;
		private int _reward;
		
		public Fisher(String name, double length, int rewardType)
		{
			_name = name;
			_length = length;
			_reward = rewardType;
		}
		
		public double getLength()
		{
			return _length;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int getRewardType()
		{
			return _reward;
		}
		
		public void setLength(double value)
		{
			_length = value;
		}
		
		public void setRewardType(int value)
		{
			_reward = value;
		}
	}
	
	private class NeedRefresh implements Runnable
	{
		protected NeedRefresh()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			_needRefresh = true;
		}
	}
}