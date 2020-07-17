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
package com.l2jserver.gameserver.communitybbs.Manager;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.Config;
import com.l2jserver.commons.database.pool.impl.ConnectionFactory;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.communitybbs.CommunityBoard;
import com.l2jserver.gameserver.data.sql.impl.ClanTable;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

public class TopBBSManager extends BaseBBSManager
{
	private static final Logger LOG = LoggerFactory.getLogger(TopBBSManager.class);
	// SQL Queries
	private static final String COUNT_FAVORITES = "SELECT COUNT(*) AS favorites FROM `bbs_favorites` WHERE `playerId`=?";
	
	TopBBSManager()
	{
	}
	
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		String path = "data/html/CommunityBoard/custom/";
		String filepath = "";
		String content = "";
		if (command.equals("_bbshome"))
		{
			CommunityBoard.separateAndSend(HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/custom/index.htm"), activeChar);
		}
		else if (command.equals("_bbstoprank"))
		{
			filepath = path + "index.htm";
			content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), filepath);
			separateAndSend(content, activeChar);
		}
		else if (command.startsWith("_bbstop;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();// _bbstop skip
			sendHtm(activeChar, "data/html/CommunityBoard/custom/" + st.nextToken() + ".htm");
		}
		else if (command.startsWith("_bbs_buff"))
		{
			CommunityBoard.getInstance().handleCommands(activeChar.getClient(), "_bbs_buff");
			return;
		}
		else if (command.startsWith("_bbstoprank;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			String file = st.nextToken();
			filepath = path + file + ".htm";
			File filecom = new File(filepath);
			
			if (!(filecom.exists()))
			{
				content = "<html><body><br><br><center>The command " + command + " points to file(" + filepath + ") that NOT exists.</center></body></html>";
				separateAndSend(content, activeChar);
				return;
			}
			
			content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), filepath);
			
			if (content.isEmpty())
			{
				content = "<html><body><br><br><center>Content Empty: The command " + command + " points to an invalid or empty html file(" + filepath + ").</center></body></html>";
			}
			
			separateAndSend(content, activeChar);
		}
		
		if (command.equals("_bbshome") && (!Config.ZEUS_ACTIVE))
		{
			CommunityBoard.getInstance().addBypass(activeChar, "Home", command);
			
			String html = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/home.html");
			html = html.replaceAll("%fav_count%", Integer.toString(getFavoriteCount(activeChar)));
			html = html.replaceAll("%region_count%", Integer.toString(getRegionCount(activeChar)));
			html = html.replaceAll("%clan_count%", Integer.toString(ClanTable.getInstance().getClanCount()));
			CommunityBoard.separateAndSend(html, activeChar);
		}
		else if (command.startsWith("_bbstop;") && (!Config.ZEUS_ACTIVE))
		{
			if ((path.length() > 0) && path.endsWith(".html"))
			{
				final String html = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/CommunityBoard/" + path);
				CommunityBoard.separateAndSend(html, activeChar);
			}
		}
		return;
	}
	
	/**
	 * Gets the Favorite links for the given player.
	 * @param player the player
	 * @return the favorite links count
	 */
	private static int getFavoriteCount(L2PcInstance player)
	{
		int count = 0;
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(COUNT_FAVORITES))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					count = rs.getInt("favorites");
				}
			}
		}
		catch (Exception e)
		{
			LOG.warn(FavoriteBBSManager.class.getSimpleName() + ": Coudn't load favorites count for player " + player.getName());
		}
		return count;
	}
	
	/**
	 * Gets the registered regions count for the given player.
	 * @param player the player
	 * @return the registered regions count
	 */
	private static int getRegionCount(L2PcInstance player)
	{
		return 0; // TODO: Implement.
	}
	
	private boolean sendHtm(L2PcInstance player, String path)
	{
		String oriPath = path;
		if ((player.getLang() != null) && !player.getLang().equalsIgnoreCase("en"))
		{
			if (path.contains("html/"))
			{
				path = path.replace("html/", "html-" + player.getLang() + "/");
			}
		}
		String content = HtmCache.getInstance().getHtm(path);
		if ((content == null) && !oriPath.equals(path))
		{
			content = HtmCache.getInstance().getHtm(oriPath);
		}
		if (content == null)
		{
			return false;
		}
		
		separateAndSend(content, player);
		return true;
	}
	
	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
	}
	
	public static TopBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final TopBBSManager _instance = new TopBBSManager();
	}
}