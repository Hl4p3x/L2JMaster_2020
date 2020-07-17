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

import java.util.StringTokenizer;

import com.l2jserver.gameserver.data.sql.impl.ClanTable;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.util.StringUtil;

public class ClanBBSManager extends BaseBBSManager
{
	ClanBBSManager()
	{
	}
	
	public static ClanBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		if (command.equals("_bbsclan"))
		{
			if ((activeChar.getClan() == null) || (activeChar.getClan().getLevel() < 2))
			{
				clanlist(activeChar, 1);
			}
			else
			{
				clanhome(activeChar);
			}
		}
		else if (command.startsWith("_bbsclan_clanlist"))
		{
			if (command.equals("_bbsclan_clanlist"))
			{
				clanlist(activeChar, 1);
			}
			else if (command.startsWith("_bbsclan_clanlist;"))
			{
				StringTokenizer st = new StringTokenizer(command, ";");
				st.nextToken();
				int index = Integer.parseInt(st.nextToken());
				clanlist(activeChar, index);
			}
		}
		else if (command.startsWith("_bbsclan_clanhome"))
		{
			if (command.equals("_bbsclan_clanhome"))
			{
				clanhome(activeChar);
			}
			else if (command.startsWith("_bbsclan_clanhome;"))
			{
				StringTokenizer st = new StringTokenizer(command, ";");
				st.nextToken();
				int index = Integer.parseInt(st.nextToken());
				clanhome(activeChar, index);
			}
		}
		else if (command.startsWith("_bbsclan_clannotice_edit;"))
		{
			clanNotice(activeChar, activeChar.getClanId());
		}
		else if (command.startsWith("_bbsclan_clannotice_enable"))
		{
			if (activeChar.getClan() != null)
			{
				activeChar.getClan().setNoticeEnabled(true);
			}
			clanNotice(activeChar, activeChar.getClanId());
		}
		else if (command.startsWith("_bbsclan_clannotice_disable"))
		{
			if (activeChar.getClan() != null)
			{
				activeChar.getClan().setNoticeEnabled(false);
			}
			clanNotice(activeChar, activeChar.getClanId());
		}
		else
		{
			separateAndSend("<html><body><br><br><center>Command : " + command + " needs core development</center><br><br></body></html>", activeChar);
		}
	}
	
	private void clanNotice(L2PcInstance activeChar, int clanId)
	{
		final L2Clan cl = ClanTable.getInstance().getClan(clanId);
		if (cl != null)
		{
			if (cl.getLevel() < 2)
			{
				activeChar.sendPacket(SystemMessageId.NO_CB_IN_MY_CLAN);
				parsecmd("_bbsclan_clanlist", activeChar);
			}
			else
			{
				final StringBuilder html = StringUtil.startAppend(2000, "<html><body><br><br>" + "<table border=0 width=755><tr><td width=10></td><td width=600 align=left>" + "<a action=\"bypass _bbshome\">Home</a> &gt; " + "<a action=\"bypass _bbsclan_clanlist\"> Clan Community </a>  &gt; "
					+ "<a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), "\"> &amp;$802; </a>" + "</td></tr>" + "</table>");
				if (activeChar.isClanLeader())
				{
					StringUtil.append(html, "<br><br><center>" + "<table width=755 border=0 cellspacing=0 cellpadding=0>"
						+ "<tr><td fixwidth=610><font color=\"AAAAAA\">The Clan Notice function allows the clan leader to send messages through a pop-up window to clan members at login.</font> </td></tr>" + "<tr><td height=20></td></tr>");
					
					if (activeChar.getClan().isNoticeEnabled())
					{
						StringUtil.append(html, "<tr><td fixwidth=610> Clan Notice Function:&nbsp;&nbsp;&nbsp;on&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_disable\">off</a>");
					}
					else
					{
						StringUtil.append(html, "<tr><td fixwidth=610> Clan Notice Function:&nbsp;&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_enable\">on</a>&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;off");
					}
					
					StringUtil.append(html, "</td></tr>" + "</table>" + "<img src=\"L2UI.Squaregray\" width=\"610\" height=\"1\">" + "<br> <br>" + "<table width=610 border=0 cellspacing=2 cellpadding=0>" + "<tr><td>Edit Notice: </td></tr>" + "<tr><td height=5></td></tr>" + "<tr><td>"
						+ "<MultiEdit var =\"Content\" width=610 height=100>" + "</td></tr>" + "</table>" + "<br>" + "<table width=610 border=0 cellspacing=0 cellpadding=0>" + "<tr><td height=5></td></tr>" + "<tr>"
						+ "<td align=center FIXWIDTH=65><button value=\"&$140;\" action=\"Write Notice Set _ Content Content Content\" back=\"l2ui_ct1.button.button_df_small_down\" width=65 height=20 fore=\"l2ui_ct1.button.button_df_small\" ></td>" + "<td align=center FIXWIDTH=45></td>"
						+ "<td align=center FIXWIDTH=500></td>" + "</tr>" + "</table>" + "</center>" + "</body>" + "</html>");
					send1001(html.toString(), activeChar);
					send1002(activeChar, activeChar.getClan().getNotice(), " ", "0");
				}
				else
				{
					StringUtil.append(html, "<img src=\"L2UI.squareblank\" width=\"1\" height=\"10\">" + "<center>" + "<table border=0 cellspacing=0 cellpadding=0><tr>" + "<td>You are not your clan's leader, and therefore cannot change the clan notice</td>" + "</tr></table>");
					if (activeChar.getClan().isNoticeEnabled())
					{
						StringUtil.append(html, "<table border=0 cellspacing=0 cellpadding=0>" + "<tr>" + "<td>The current clan notice:</td>" + "</tr>" + "<tr><td fixwidth=5></td>" + "<td FIXWIDTH=600 align=left>" + activeChar.getClan().getNotice() + "</td>" + "<td fixqqwidth=5></td>" + "</tr>"
							+ "</table>");
					}
					StringUtil.append(html, "</center>" + "</body>" + "</html>");
					separateAndSend(html.toString(), activeChar);
				}
			}
		}
	}
	
	private void clanlist(L2PcInstance activeChar, int index)
	{
		if (index < 1)
		{
			index = 1;
		}
		
		final StringBuilder html = StringUtil.startAppend(2000, "<html><body><br><br><center>" + "<br1><br1><table border=0 cellspacing=0 cellpadding=0>" + "<tr><td FIXWIDTH=15>&nbsp;</td>" + "<td width=610 height=30 align=left>" + "<a action=\"bypass _bbsclan_clanlist\"> Clan Community </a>"
			+ "</td></tr></table>" + "<table border=0 cellspacing=0 cellpadding=0 width=755 bgcolor=A7A19A>" + "<tr><td height=10></td></tr>" + "<tr>" + "<td fixWIDTH=5></td>" + "<td fixWIDTH=600>"
			+ "<a action=\"bypass _bbsclan_clanhome;", String.valueOf((activeChar.getClan() != null) ? activeChar.getClan().getId() : 0), "\">[Go to my clan]</a>&nbsp;&nbsp;" + "</td>" + "<td fixWIDTH=5></td>" + "</tr>" + "<tr><td height=10></td></tr>" + "</table>" + "<br>"
				+ "<table border=0 cellspacing=0 cellpadding=2 bgcolor=A7A19A width=755>" + "<tr>" + "<td FIXWIDTH=5></td>" + "<td FIXWIDTH=200 align=center>Clan Name</td>" + "<td FIXWIDTH=200 align=center>Clan Leader</td>" + "<td FIXWIDTH=100 align=center>Clan Level</td>"
				+ "<td FIXWIDTH=150 align=center>Number of Clan Members</td>" + "<td FIXWIDTH=5></td>" + "</tr>" + "</table>" + "<img src=\"L2UI.Squareblank\" width=\"1\" height=\"5\">");
		
		int i = 0;
		for (L2Clan cl : ClanTable.getInstance().getClans())
		{
			if (i > ((index + 1) * 7))
			{
				break;
			}
			
			if (i++ >= ((index - 1) * 7))
			{
				StringUtil.append(html, "<img src=\"L2UI.SquareBlank\" width=\"755\" height=\"3\">" + "<table border=0 cellspacing=0 cellpadding=0 width=755>" + "<tr> " + "<td FIXWIDTH=5></td>"
					+ "<td FIXWIDTH=200 align=center><a action=\"bypass _bbsclan_clanhome;", String.valueOf(cl.getId()), "\">", cl.getName(), "</a></td>" + "<td FIXWIDTH=200 align=center>", cl.getLeaderName(), "</td>"
						+ "<td FIXWIDTH=100 align=center>", String.valueOf(cl.getLevel()), "</td>" + "<td FIXWIDTH=150 align=center>", String.valueOf(cl.getMembersCount()), "</td>" + "<td FIXWIDTH=5></td>" + "</tr>" + "<tr><td height=5></td></tr>" + "</table>"
							+ "<img src=\"L2UI.SquareBlank\" width=\"755\" height=\"3\">" + "<img src=\"L2UI.SquareGray\" width=\"755\" height=\"1\">");
			}
		}
		
		html.append("<img src=\"L2UI.SquareBlank\" width=\"755\" height=\"2\">" + "<table cellpadding=0 cellspacing=2 border=0><tr>");
		
		if (index == 1)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		else
		{
			StringUtil.append(html, "<td><button action=\"_bbsclan_clanlist;", String.valueOf(index - 1), "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		
		i = 0;
		int nbp;
		nbp = ClanTable.getInstance().getClans().size() / 8;
		if ((nbp * 8) != ClanTable.getInstance().getClans().size())
		{
			nbp++;
		}
		for (i = 1; i <= nbp; i++)
		{
			if (i == index)
			{
				StringUtil.append(html, "<td> ", String.valueOf(i), " </td>");
			}
			else
			{
				StringUtil.append(html, "<td><a action=\"bypass _bbsclan_clanlist;", String.valueOf(i), "\"> ", String.valueOf(i), " </a></td>");
			}
			
		}
		if (index == nbp)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		else
		{
			StringUtil.append(html, "<td><button action=\"bypass _bbsclan_clanlist;", String.valueOf(index + 1), "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		html.append("</tr></table>" + "<table border=0 cellspacing=0 cellpadding=0>" + "<tr><td width=755><img src=\"sek.cbui141\" width=\"755\" height=\"1\"></td></tr>" + "</table>"
			+ "<table border=0><tr><td><combobox width=65 var=keyword list=\"Name;Ruler\"></td><td><edit var = \"Search\" width=130 height=15 length=\"16\"></td>"
			+ "<td><button value=\"&$420;\" action=\"Write 5 -1 0 Search keyword keyword\" back=\"L2UI_ct1.button_df\" width=65 height=25 fore=\"L2UI_ct1.button_df\"> </td> </tr></table>" + "<br>" + "<br>" + "</center>" + "</body>" + "</html>");
		separateAndSend(html.toString(), activeChar);
	}
	
	private void clanhome(L2PcInstance activeChar)
	{
		clanhome(activeChar, activeChar.getClan().getId());
	}
	
	private static class SingletonHolder
	{
		protected static final ClanBBSManager _instance = new ClanBBSManager();
	}
	
	private void clanhome(L2PcInstance activeChar, int clanId)
	{
		L2Clan cl = ClanTable.getInstance().getClan(clanId);
		if (cl != null)
		{
			if (cl.getLevel() < 2)
			{
				activeChar.sendPacket(SystemMessageId.NO_CB_IN_MY_CLAN);
				parsecmd("_bbsclan_clanlist", activeChar);
			}
			else
			{
				final String html = StringUtil.concat("<html><body><center><br><br>" + "<br1><br1><table border=0 cellspacing=0 cellpadding=0>" + "<tr><td FIXWIDTH=15>&nbsp;</td>" + "<td width=755 height=30 align=left>"
					+ "<a action=\"bypass _bbshome\">HOME</a> &gt; <a action=\"bypass _bbsclan_clanlist\"> Clan Community </a>  &gt; <a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), "\"> &amp;$802; </a>" + "</td></tr></table>"
						+ "<table border=0 cellspacing=0 cellpadding=0 width=755 bgcolor=A7A19A>" + "<tr><td height=10></td></tr>" + "<tr>" + "<td fixWIDTH=5></td>" + "<td fixwidth=600>"
						+ "<a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), ";announce\">[Clan Announcement]</a> <a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), ";cbb\">[Clan Bulletin Board]</a>"
							+ "<a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), ";cmail\">[Clan Mail]</a>&nbsp;&nbsp;" + "<a action=\"bypass _bbsclan_clannotice_edit;", String.valueOf(clanId), ";cnotice\">[Clan Notice]</a>&nbsp;&nbsp;" + "</td>" + "<td fixWIDTH=5></td>" + "</tr>"
								+ "<tr><td height=10></td></tr>" + "</table>" + "<table border=0 cellspacing=0 cellpadding=0 width=755>" + "<tr><td height=10></td></tr>" + "<tr><td fixWIDTH=5></td>" + "<td fixwidth=290 valign=top>" + "</td>" + "<td fixWIDTH=5></td>"
								+ "<td fixWIDTH=5 align=center valign=top><img src=\"l2ui.squaregray\" width=2  height=128></td>" + "<td fixWIDTH=5></td>" + "<td fixwidth=295>" + "<table border=0 cellspacing=0 cellpadding=0 width=295>" + "<tr>" + "<td fixWIDTH=100 align=left>Clan Name</td>"
								+ "<td fixWIDTH=195 align=left>", cl.getName(), "</td>" + "</tr>" + "<tr><td height=7></td></tr><br1>" + "<tr>" + "<td fixWIDTH=100 align=left>Clan Level</td>" + "<td fixWIDTH=195 align=left height=16>", String.valueOf(cl.getLevel()), "</td>" + "</tr>"
									+ "<tr><td height=7></td></tr><br1>" + "<tr>" + "<td fixWIDTH=100 align=left>Clan Members</td>" + "<td fixWIDTH=195 align=left height=16>", String.valueOf(cl.getMembersCount()), "</td>" + "</tr>" + "<tr><td height=7></td></tr><br1>" + "<tr>"
										+ "<td fixWIDTH=100 align=left>Clan Leader</td>" + "<td fixWIDTH=195 align=left height=16>", cl.getLeaderName(), "</td>" + "</tr>" + "<tr><td height=7></td></tr><br1>" + "<tr>" + "<td fixWIDTH=100 align=left>Administrator</td>"
											+ "<td fixWIDTH=195 align=left height=16>", cl.getLeaderName(), "</td>" + "</tr>" + "<tr><td height=7></td></tr>" + "<tr><td height=7></td></tr><br1>" + "<tr>" + "<td fixWIDTH=100 align=left>Alliance</td>"
												+ "<td fixWIDTH=195 align=left height=16>", (cl.getAllyName() != null) ? cl.getAllyName() : "", "</td>" + "</tr>" + "</table>" + "</td>" + "<td fixWIDTH=5></td>" + "</tr>" + "<tr><td height=10></td></tr>" + "</table>"
													+ "<img src=\"L2UI.squareblank\" width=\"1\" height=\"5\">" + "<img src=\"L2UI.squaregray\" width=\"755\" height=\"13\">" + "<br>" + "</center>" + "<br> <br>" + "</body>" + "</html>");
				separateAndSend(html, activeChar);
			}
		}
	}
	
	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		if (ar1.equals("Set"))
		{
			activeChar.getClan().setNotice(ar4);
			parsecmd("_bbsclan_clanhome;" + activeChar.getClan().getId(), activeChar);
		}
	}
}