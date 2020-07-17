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
package com.l2jserver.gameserver.model.actor.instance;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.l2jserver.Config;
import com.l2jserver.gameserver.data.sql.impl.ClanTable;
import com.l2jserver.gameserver.data.xml.impl.ClassListData;
import com.l2jserver.gameserver.data.xml.impl.SkillTreesData;
import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.enums.Race;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.FortSiegeManager;
import com.l2jserver.gameserver.instancemanager.SiegeManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2Clan.SubPledge;
import com.l2jserver.gameserver.model.L2ClanMember;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.base.AcquireSkillType;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.base.PlayerClass;
import com.l2jserver.gameserver.model.base.SubClass;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillLaunched;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.StringUtil;

/**
 * This class ...
 * @version $Revision: 1.4.2.3.2.8 $ $Date: 2005/03/29 23:15:15 $
 */
public class L2VillageMasterInstance extends L2NpcInstance
{
	private static Logger _log = Logger.getLogger(L2VillageMasterInstance.class.getName());
	
	/**
	 * Creates a village master.
	 * @param template the village master NPC template
	 */
	public L2VillageMasterInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2VillageMasterInstance);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/villagemaster/" + pom + ".htm";
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		
		switch (st.nextToken())
		{
			case "create_clan":
			{
				if (st.hasMoreTokens())
				{
					final String clanName = st.nextToken();
					
					if (st.hasMoreTokens() ||  (!isValidName(clanName)))
					{
						player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
						return;
					}
					ClanTable.getInstance().createClan(player, clanName);
				}
				break;
			}
			case "create_academy":
			{
				if (st.hasMoreTokens())
				{
					createSubPledge(player, st.nextToken(), null, L2Clan.SUBUNIT_ACADEMY, 5);
				}
				break;
			}
			case "rename_pledge":
			{
				if (st.countTokens() > 1)
				{
					renameSubPledge(player, Integer.parseInt(st.nextToken()), st.nextToken());
				}
				break;
			}
			case "create_royal":
			{
				if (st.countTokens() > 1)
				{
					createSubPledge(player, st.nextToken(), st.nextToken(), L2Clan.SUBUNIT_ROYAL1, 6);
				}
				break;
			}
			case "create_knight":
			{
				if (st.countTokens() > 1)
				{
					createSubPledge(player, st.nextToken(), st.nextToken(), L2Clan.SUBUNIT_KNIGHT1, 7);
				}
				break;
			}
			case "assign_subpl_leader":
			{
				if (st.countTokens() > 1)
				{
					assignSubPledgeLeader(player, st.nextToken(), st.nextToken());
				}
				break;
			}
			case "create_ally":
			{
				if (!player.isClanLeader())
				{
					player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
				}
				else
				{
					if (st.hasMoreTokens())
					{
						player.getClan().createAlly(player, st.nextToken());
					}
				}
				break;
			}
			case "dissolve_ally":
			{
				player.getClan().dissolveAlly(player);
				break;
			}
			case "dissolve_clan":
			{
				dissolveClan(player, player.getClanId());
				break;
			}
			case "change_clan_leader":
			{
				if (!player.isClanLeader())
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
					return;
				}
				
				if (!st.hasMoreTokens())
				{
					break;
				}
				
				final String newLeaderName = st.nextToken();
				
				if (player.getName().equalsIgnoreCase(newLeaderName))
				{
					return;
				}
				
				final L2Clan clan = player.getClan();
				final L2ClanMember member = clan.getClanMember(newLeaderName);
				if (member == null)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
					sm.addString(newLeaderName);
					player.sendPacket(sm);
					return;
				}
				
				if (!member.isOnline())
				{
					player.sendPacket(SystemMessageId.INVITED_USER_NOT_ONLINE);
					return;
				}
				
				// To avoid clans with null clan leader, academy members shouldn't be eligible for clan leader.
				if (member.getPlayerInstance().isAcademyMember())
				{
					player.sendPacket(SystemMessageId.RIGHT_CANT_TRANSFERRED_TO_ACADEMY_MEMBER);
					return;
				}
				
				if (Config.ALT_CLAN_LEADER_INSTANT_ACTIVATION)
				{
					clan.setNewLeader(member);
				}
				else
				{
					final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
					if (clan.getNewLeaderId() == 0)
					{
						clan.setNewLeaderId(member.getObjectId(), true);
						msg.setFile(player.getHtmlPrefix(), "data/scripts/village_master/Clan/9000-07-success.htm");
					}
					else
					{
						msg.setFile(player.getHtmlPrefix(), "data/scripts/village_master/Clan/9000-07-in-progress.htm");
					}
					player.sendPacket(msg);
				}
				break;
			}
			case "cancel_clan_leader_change":
			{
				if (!player.isClanLeader())
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
					return;
				}
				
				final L2Clan clan = player.getClan();
				final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
				if (clan.getNewLeaderId() != 0)
				{
					clan.setNewLeaderId(0, true);
					msg.setFile(player.getHtmlPrefix(), "data/scripts/village_master/Clan/9000-07-canceled.htm");
				}
				else
				{
					msg.setHtml("<html><body>You don't have clan leader delegation applications submitted yet!</body></html>");
				}
				
				player.sendPacket(msg);
				break;
			}
			case "recover_clan":
			{
				recoverClan(player, player.getClanId());
				break;
			}
			case "increase_clan_level":
			{
				if (player.getClan().levelUpClan(player))
				{
					player.broadcastPacket(new MagicSkillUse(player, 5103, 1, 0, 0));
					player.broadcastPacket(new MagicSkillLaunched(player, 5103, 1));
				}
				break;
			}
			case "learn_clan_skills":
			{
				showPledgeSkillList(player);
				break;
			}
			case "Subclass":
			{
				// Subclasses may not be changed while a skill is in use.
				if (player.isCastingNow() || player.isAllSkillsDisabled())
				{
					player.sendPacket(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE);
					return;
				}
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				// Subclasses may not be changed while a transformated state.
				if (player.getTransformation() != null)
				{
					html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_NoTransformed.htm");
					player.sendPacket(html);
					return;
				}
				// Subclasses may not be changed while a summon is active.
				if (player.hasSummon())
				{
					html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_NoSummon.htm");
					player.sendPacket(html);
					return;
				}
				// Subclasses may not be changed while you have exceeded your inventory limit.
				if (!player.isInventoryUnder90(true))
				{
					player.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_INVENTORY_FULL);
					return;
				}
				// Subclasses may not be changed while a you are over your weight limit.
				if (player.getWeightPenalty() >= 2)
				{
					player.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_OVERWEIGHT);
					return;
				}
				
				int cmdChoice = 0;
				int paramOne = 0;
				int paramTwo = 0;
				try
				{
					cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
					
					int endIndex = command.indexOf(' ', 11);
					if (endIndex == -1)
					{
						endIndex = command.length();
					}
					
					if (command.length() > 11)
					{
						paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
						if (command.length() > endIndex)
						{
							paramTwo = Integer.parseInt(command.substring(endIndex).trim());
						}
					}
				}
				catch (Exception NumberFormatException)
				{
					_log.warning(L2VillageMasterInstance.class.getName() + ": Wrong numeric values for command " + command);
				}
				
				Set<PlayerClass> subsAvailable = null;
				switch (cmdChoice)
				{
					case 0: // Subclass change menu
						html.setFile(player.getHtmlPrefix(), getSubClassMenu(player.getRace()));
						break;
					case 1: // Add Subclass - Initial
						// Avoid giving player an option to add a new sub class, if they have max sub-classes already.
						if (player.getTotalSubClasses() >= Config.MAX_SUBCLASS)
						{
							html.setFile(player.getHtmlPrefix(), getSubClassFail());
							break;
						}
						
						subsAvailable = getAvailableSubClasses(player);
						if ((subsAvailable != null) && !subsAvailable.isEmpty())
						{
							html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_Add.htm");
							final StringBuilder content1 = StringUtil.startAppend(200);
							for (PlayerClass subClass : subsAvailable)
							{
								StringUtil.append(content1, "<a action=\"bypass -h npc_%objectId%_Subclass 4 ", String.valueOf(subClass.ordinal()), "\" msg=\"1268;", ClassListData.getInstance().getClass(subClass.ordinal()).getClassName(), "\">", ClassListData.getInstance().getClass(subClass.ordinal()).getClientCode(), "</a><br>");
							}
							html.replace("%list%", content1.toString());
						}
						else
						{
							if ((player.getRace() == Race.ELF) || (player.getRace() == Race.DARK_ELF))
							{
								html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_Fail_Elves.htm");
								player.sendPacket(html);
							}
							else if (player.getRace() == Race.KAMAEL)
							{
								html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_Fail_Kamael.htm");
								player.sendPacket(html);
							}
							else
							{
								// TODO: Retail message
								player.sendMessage("There are no sub classes available at this time.");
							}
							return;
						}
						break;
					case 2: // Change Class - Initial
						if (player.getSubClasses().isEmpty())
						{
							html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_ChangeNo.htm");
						}
						else
						{
							final StringBuilder content2 = StringUtil.startAppend(200);
							if (checkVillageMaster(player.getBaseClass()))
							{
								StringUtil.append(content2, "<a action=\"bypass -h npc_%objectId%_Subclass 5 0\">", ClassListData.getInstance().getClass(player.getBaseClass()).getClientCode(), "</a><br>");
							}
							
							for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
							{
								SubClass subClass = subList.next();
								if (checkVillageMaster(subClass.getClassDefinition()))
								{
									StringUtil.append(content2, "<a action=\"bypass -h npc_%objectId%_Subclass 5 ", String.valueOf(subClass.getClassIndex()), "\">", ClassListData.getInstance().getClass(subClass.getClassId()).getClientCode(), "</a><br>");
								}
							}
							
							if (content2.length() > 0)
							{
								html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_Change.htm");
								html.replace("%list%", content2.toString());
							}
							else
							{
								html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_ChangeNotFound.htm");
							}
						}
						break;
					case 3: // Change/Cancel Subclass - Initial
						if ((player.getSubClasses() == null) || player.getSubClasses().isEmpty())
						{
							html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_ModifyEmpty.htm");
							break;
						}
						
						// custom value
						if (player.getTotalSubClasses() > 3)
						{
							html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_ModifyCustom.htm");
							final StringBuilder content3 = StringUtil.startAppend(200);
							int classIndex = 1;
							
							for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
							{
								SubClass subClass = subList.next();
								
								StringUtil.append(content3, "Sub-class ", String.valueOf(classIndex++), "<br>", "<a action=\"bypass -h npc_%objectId%_Subclass 6 ", String.valueOf(subClass.getClassIndex()), "\">", ClassListData.getInstance().getClass(subClass.getClassId()).getClientCode(), "</a><br>");
							}
							html.replace("%list%", content3.toString());
						}
						else
						{
							// retail html contain only 3 subclasses
							html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_Modify.htm");
							if (player.getSubClasses().containsKey(1))
							{
								html.replace("%sub1%", ClassListData.getInstance().getClass(player.getSubClasses().get(1).getClassId()).getClientCode());
							}
							else
							{
								html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 1\">%sub1%</a><br>", "");
							}
							
							if (player.getSubClasses().containsKey(2))
							{
								html.replace("%sub2%", ClassListData.getInstance().getClass(player.getSubClasses().get(2).getClassId()).getClientCode());
							}
							else
							{
								html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 2\">%sub2%</a><br>", "");
							}
							
							if (player.getSubClasses().containsKey(3))
							{
								html.replace("%sub3%", ClassListData.getInstance().getClass(player.getSubClasses().get(3).getClassId()).getClientCode());
							}
							else
							{
								html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 6 3\">%sub3%</a><br>", "");
							}
						}
						break;
					case 4: // Add Subclass - Action (Subclass 4 x[x])
						/**
						 * If the character is less than level 75 on any of their previously chosen classes then disallow them to change to their most recently added sub-class choice.
						 */
						if (!player.getFloodProtectors().getSubclass().tryPerformAction("add subclass"))
						{
							_log.warning(L2VillageMasterInstance.class.getName() + ": Player " + player.getName() + " has performed a subclass change too fast");
							return;
						}
						
						boolean allowAddition = true;
						
						if (player.getTotalSubClasses() >= Config.MAX_SUBCLASS)
						{
							allowAddition = false;
						}
						
						if (player.getLevel() < 75)
						{
							allowAddition = false;
						}
						
						if (allowAddition)
						{
							if (!player.getSubClasses().isEmpty())
							{
								for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
								{
									SubClass subClass = subList.next();
									
									if (subClass.getLevel() < 75)
									{
										allowAddition = false;
										break;
									}
								}
							}
						}
						
						/**
						 * If quest checking is enabled, verify if the character has completed the Mimir's Elixir (Path to Subclass) and Fate's Whisper (A Grade Weapon) quests by checking for instances of their unique reward items. If they both exist, remove both unique items and continue with
						 * adding the sub-class.
						 */
						if (allowAddition && !Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS)
						{
							allowAddition = checkQuests(player);
						}
						
						if (allowAddition && isValidNewSubClass(player, paramOne))
						{
							if (!player.addSubClass(paramOne, player.getTotalSubClasses() + 1))
							{
								return;
							}
							
							player.setActiveClass(player.getTotalSubClasses());
							
							html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_AddOk.htm");
							
							player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS); // Subclass added.
						}
						else
						{
							html.setFile(player.getHtmlPrefix(), getSubClassFail());
						}
						break;
					case 5: // Change Class - Action
						/**
						 * If the character is less than level 75 on any of their previously chosen classes then disallow them to change to their most recently added sub-class choice. Note: paramOne = classIndex
						 */
						if (!player.getFloodProtectors().getSubclass().tryPerformAction("change class"))
						{
							_log.warning(L2VillageMasterInstance.class.getName() + ": Player " + player.getName() + " has performed a subclass change too fast");
							return;
						}
						
						if (player.getClassIndex() == paramOne)
						{
							html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_Current.htm");
							break;
						}
						
						if (paramOne == 0)
						{
							if (!checkVillageMaster(player.getBaseClass()))
							{
								return;
							}
						}
						else
						{
							try
							{
								if (!checkVillageMaster(player.getSubClasses().get(paramOne).getClassDefinition()))
								{
									return;
								}
							}
							catch (NullPointerException e)
							{
								return;
							}
						}
						
						player.setActiveClass(paramOne);
						player.sendPacket(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED); // Transfer completed.
						return;
					case 6: // Change/Cancel Subclass - Choice
						// validity check
						if ((paramOne < 1) || (paramOne > Config.MAX_SUBCLASS))
						{
							return;
						}
						
						subsAvailable = getAvailableSubClasses(player);
						// another validity check
						if ((subsAvailable == null) || subsAvailable.isEmpty())
						{
							// TODO: Retail message
							player.sendMessage("There are no sub classes available at this time.");
							return;
						}
						
						final StringBuilder content6 = StringUtil.startAppend(200);
						for (PlayerClass subClass : subsAvailable)
						{
							StringUtil.append(content6, "<a action=\"bypass -h npc_%objectId%_Subclass 7 ", String.valueOf(paramOne), " ", String.valueOf(subClass.ordinal()), "\" msg=\"1445;", "\">", ClassListData.getInstance().getClass(subClass.ordinal()).getClientCode(), "</a><br>");
						}
						
						switch (paramOne)
						{
							case 1:
								html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_ModifyChoice1.htm");
								break;
							case 2:
								html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_ModifyChoice2.htm");
								break;
							case 3:
								html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_ModifyChoice3.htm");
								break;
							default:
								html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_ModifyChoice.htm");
						}
						html.replace("%list%", content6.toString());
						break;
					case 7: // Change Subclass - Action
						/**
						 * Warning: the information about this subclass will be removed from the subclass list even if false!
						 */
						if (!player.getFloodProtectors().getSubclass().tryPerformAction("change class"))
						{
							_log.warning(L2VillageMasterInstance.class.getName() + ": Player " + player.getName() + " has performed a subclass change too fast");
							return;
						}
						
						if (!isValidNewSubClass(player, paramTwo))
						{
							return;
						}
						
						if (player.modifySubClass(paramOne, paramTwo))
						{
							player.abortCast();
							player.stopAllEffectsExceptThoseThatLastThroughDeath(); // all effects from old subclass stopped!
							player.stopAllEffectsNotStayOnSubclassChange();
							player.stopCubics();
							player.setActiveClass(paramOne);
							
							html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/SubClass_ModifyOk.htm");
							html.replace("%name%", ClassListData.getInstance().getClass(paramTwo).getClientCode());
							
							player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS); // Subclass added.
						}
						else
						{
							/**
							 * This isn't good! modifySubClass() removed subclass from memory we must update _classIndex! Else IndexOutOfBoundsException can turn up some place down the line along with other seemingly unrelated problems.
							 */
							player.setActiveClass(0); // Also updates _classIndex plus switching _classid to baseclass.
							
							player.sendMessage("The sub class could not be added, you have been reverted to your base class.");
							return;
						}
						break;
				}
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				break;
			}
			default:
			{
				super.onBypassFeedback(player, command);
			}
		}
	}
	
	protected String getSubClassMenu(Race race)
	{
		if (Config.ALT_GAME_SUBCLASS_EVERYWHERE || (race != Race.KAMAEL))
		{
			return "data/html/villagemaster/SubClass.htm";
		}
		
		return "data/html/villagemaster/SubClass_NoOther.htm";
	}
	
	protected String getSubClassFail()
	{
		return "data/html/villagemaster/SubClass_Fail.htm";
	}
	
	protected boolean checkQuests(L2PcInstance player)
	{
		return player.isNoble() || player.hasQuestCompleted("Q00234_FatesWhisper") || player.hasQuestCompleted("Q00235_MimirsElixir");
	}
	
	/**
	 * Returns list of available subclasses Base class and already used subclasses removed
	 * @param player
	 * @return
	 */
	private final Set<PlayerClass> getAvailableSubClasses(L2PcInstance player)
	{
		// get player base class
		final int currentBaseId = player.getBaseClass();
		final ClassId baseCID = ClassId.getClassId(currentBaseId);
		
		// we need 2nd occupation ID
		final int baseClassId;
		if (baseCID.level() > 2)
		{
			baseClassId = baseCID.getParent().ordinal();
		}
		else
		{
			baseClassId = currentBaseId;
		}
		
		/**
		 * If the race of your main class is Elf or Dark Elf, you may not select each class as a subclass to the other class. If the race of your main class is Kamael, you may not subclass any other race If the race of your main class is NOT Kamael, you may not subclass any Kamael class You may not
		 * select Overlord and Warsmith class as a subclass. You may not select a similar class as the subclass. The occupations classified as similar classes are as follows: Treasure Hunter, Plainswalker and Abyss Walker Hawkeye, Silver Ranger and Phantom Ranger Paladin, Dark Avenger, Temple Knight
		 * and Shillien Knight Warlocks, Elemental Summoner and Phantom Summoner Elder and Shillien Elder Swordsinger and Bladedancer Sorcerer, Spellsinger and Spellhowler Also, Kamael have a special, hidden 4 subclass, the inspector, which can only be taken if you have already completed the other
		 * two Kamael subclasses
		 */
		Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		
		if ((availSubs != null) && !availSubs.isEmpty())
		{
			for (Iterator<PlayerClass> availSub = availSubs.iterator(); availSub.hasNext();)
			{
				PlayerClass pclass = availSub.next();
				
				// check for the village master
				if (!checkVillageMaster(pclass))
				{
					availSub.remove();
					continue;
				}
				
				// scan for already used subclasses
				int availClassId = pclass.ordinal();
				ClassId cid = ClassId.getClassId(availClassId);
				SubClass prevSubClass;
				ClassId subClassId;
				for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					prevSubClass = subList.next();
					subClassId = ClassId.getClassId(prevSubClass.getClassId());
					
					if (subClassId.equalsOrChildOf(cid))
					{
						availSub.remove();
						break;
					}
				}
			}
		}
		
		return availSubs;
	}
	
	/**
	 * Check new subclass classId for validity (villagemaster race/type, is not contains in previous subclasses, is contains in allowed subclasses) Base class not added into allowed subclasses.
	 * @param player
	 * @param classId
	 * @return
	 */
	private final boolean isValidNewSubClass(L2PcInstance player, int classId)
	{
		if (!checkVillageMaster(classId))
		{
			return false;
		}
		
		final ClassId cid = ClassId.values()[classId];
		SubClass sub;
		ClassId subClassId;
		for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
		{
			sub = subList.next();
			subClassId = ClassId.values()[sub.getClassId()];
			
			if (subClassId.equalsOrChildOf(cid))
			{
				return false;
			}
		}
		
		// get player base class
		final int currentBaseId = player.getBaseClass();
		final ClassId baseCID = ClassId.getClassId(currentBaseId);
		
		// we need 2nd occupation ID
		final int baseClassId;
		if (baseCID.level() > 2)
		{
			baseClassId = baseCID.getParent().ordinal();
		}
		else
		{
			baseClassId = currentBaseId;
		}
		
		Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		if ((availSubs == null) || availSubs.isEmpty())
		{
			return false;
		}
		
		boolean found = false;
		for (PlayerClass pclass : availSubs)
		{
			if (pclass.ordinal() == classId)
			{
				found = true;
				break;
			}
		}
		return found;
	}
	
	protected boolean checkVillageMasterRace(PlayerClass pclass)
	{
		return true;
	}
	
	protected boolean checkVillageMasterTeachType(PlayerClass pclass)
	{
		return true;
	}
	
	/**
	 * Returns true if this classId allowed for master
	 * @param classId
	 * @return
	 */
	public final boolean checkVillageMaster(int classId)
	{
		return checkVillageMaster(PlayerClass.values()[classId]);
	}
	
	/**
	 * Returns true if this PlayerClass is allowed for master
	 * @param pclass
	 * @return
	 */
	public final boolean checkVillageMaster(PlayerClass pclass)
	{
		if (Config.ALT_GAME_SUBCLASS_EVERYWHERE)
		{
			return true;
		}
		
		return checkVillageMasterRace(pclass) && checkVillageMasterTeachType(pclass);
	}
	
	private static final Iterator<SubClass> iterSubClasses(L2PcInstance player)
	{
		return player.getSubClasses().values().iterator();
	}
	
	private static final void dissolveClan(L2PcInstance player, int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final L2Clan clan = player.getClan();
		if (clan.getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.CANNOT_DISPERSE_THE_CLANS_IN_ALLY);
			return;
		}
		if (clan.isAtWar())
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_WAR);
			return;
		}
		if ((clan.getCastleId() != 0) || (clan.getHideoutId() != 0) || (clan.getFortId() != 0))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_OWNING_CLAN_HALL_OR_CASTLE);
			return;
		}
		
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, castle.getResidenceId()))
			{
				player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE);
				return;
			}
		}
		for (Fort fort : FortManager.getInstance().getForts())
		{
			if (FortSiegeManager.getInstance().checkIsRegistered(clan, fort.getResidenceId()))
			{
				player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE);
				return;
			}
		}
		
		if (player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE);
			return;
		}
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.DISSOLUTION_IN_PROGRESS);
			return;
		}
		
		clan.setDissolvingExpiryTime(System.currentTimeMillis() + (Config.ALT_CLAN_DISSOLVE_DAYS * 86400000L)); // 24*60*60*1000 = 86400000
		clan.updateClanInDB();
		
		// The clan leader should take the XP penalty of a full death.
		player.calculateDeathExpPenalty(null, false);
		ClanTable.getInstance().scheduleRemoveClan(clan.getId());
	}
	
	private static final void recoverClan(L2PcInstance player, int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final L2Clan clan = player.getClan();
		clan.setDissolvingExpiryTime(0);
		clan.updateClanInDB();
	}
	
	private static final void createSubPledge(L2PcInstance player, String clanName, String leaderName, int pledgeType, int minClanLvl)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final L2Clan clan = player.getClan();
		if (clan.getLevel() < minClanLvl)
		{
			if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN_ACADEMY);
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT);
			}
			
			return;
		}
		if (!Util.isAlphaNumeric(clanName) || !isValidName(clanName) || (2 > clanName.length()))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		if (clanName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_TOO_LONG);
			return;
		}
		
		for (L2Clan tempClan : ClanTable.getInstance().getClans())
		{
			if (tempClan.getSubPledge(clanName) != null)
			{
				if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
					sm.addString(clanName);
					player.sendPacket(sm);
				}
				else
				{
					player.sendPacket(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME);
				}
				
				return;
			}
		}
		
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			if ((clan.getClanMember(leaderName) == null) || (clan.getClanMember(leaderName).getPledgeType() != 0))
			{
				if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
				{
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
				}
				else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
				{
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
				}
				
				return;
			}
		}
		
		final int leaderId = pledgeType != L2Clan.SUBUNIT_ACADEMY ? clan.getClanMember(leaderName).getObjectId() : 0;
		
		if (clan.createSubPledge(player, pledgeType, leaderId, clanName) == null)
		{
			return;
		}
		
		SystemMessage sm;
		if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_S1S_CLAN_ACADEMY_HAS_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_CREATED);
		}
		player.sendPacket(sm);
		
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			final L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
			final L2PcInstance leaderPlayer = leaderSubPledge.getPlayerInstance();
			if (leaderPlayer != null)
			{
				leaderPlayer.setPledgeClass(L2ClanMember.calculatePledgeClass(leaderPlayer));
				leaderPlayer.sendPacket(new UserInfo(leaderPlayer));
				leaderPlayer.sendPacket(new ExBrExtraUserInfo(leaderPlayer));
			}
		}
	}
	
	private static final void renameSubPledge(L2PcInstance player, int pledgeType, String pledgeName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final L2Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(pledgeType);
		
		if (subPledge == null)
		{
			player.sendMessage("Pledge don't exists.");
			return;
		}
		if (!Util.isAlphaNumeric(pledgeName) || !isValidName(pledgeName) || (2 > pledgeName.length()))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		if (pledgeName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_TOO_LONG);
			return;
		}
		
		subPledge.setName(pledgeName);
		clan.updateSubPledgeInDB(subPledge.getId());
		clan.broadcastClanStatus();
		player.sendMessage("Pledge name changed.");
	}
	
	private static final void assignSubPledgeLeader(L2PcInstance player, String clanName, String leaderName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		if (leaderName.length() > 16)
		{
			player.sendPacket(SystemMessageId.NAMING_CHARNAME_UP_TO_16CHARS);
			return;
		}
		if (player.getName().equals(leaderName))
		{
			player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			return;
		}
		
		final L2Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(clanName);
		
		if ((null == subPledge) || (subPledge.getId() == L2Clan.SUBUNIT_ACADEMY))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		if ((clan.getClanMember(leaderName) == null) || (clan.getClanMember(leaderName).getPledgeType() != 0))
		{
			if (subPledge.getId() >= L2Clan.SUBUNIT_KNIGHT1)
			{
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
			}
			else if (subPledge.getId() >= L2Clan.SUBUNIT_ROYAL1)
			{
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			}
			
			return;
		}
		
		subPledge.setLeaderId(clan.getClanMember(leaderName).getObjectId());
		clan.updateSubPledgeInDB(subPledge.getId());
		
		final L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
		final L2PcInstance leaderPlayer = leaderSubPledge.getPlayerInstance();
		if (leaderPlayer != null)
		{
			leaderPlayer.setPledgeClass(L2ClanMember.calculatePledgeClass(leaderPlayer));
			leaderPlayer.sendPacket(new UserInfo(leaderPlayer));
			leaderPlayer.sendPacket(new ExBrExtraUserInfo(leaderPlayer));
		}
		
		clan.broadcastClanStatus();
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_BEEN_SELECTED_AS_CAPTAIN_OF_S2);
		sm.addString(leaderName);
		sm.addString(clanName);
		clan.broadcastToOnlineMembers(sm);
	}
	
	/**
	 * this displays PledgeSkillList to the player.
	 * @param player
	 */
	public static final void showPledgeSkillList(L2PcInstance player)
	{
		if (!player.isClanLeader())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/NotClanLeader.htm");
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final List<L2SkillLearn> skills = SkillTreesData.getInstance().getAvailablePledgeSkills(player.getClan());
		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.PLEDGE);
		int counts = 0;
		
		for (L2SkillLearn s : skills)
		{
			asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getLevelUpSp(), s.getSocialClass().ordinal());
			counts++;
		}
		
		if (counts == 0)
		{
			if (player.getClan().getLevel() < 8)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
				if (player.getClan().getLevel() < 5)
				{
					sm.addInt(5);
				}
				else
				{
					sm.addInt(player.getClan().getLevel() + 1);
				}
				player.sendPacket(sm);
			}
			else
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				html.setFile(player.getHtmlPrefix(), "data/html/villagemaster/NoMoreSkills.htm");
				player.sendPacket(html);
			}
		}
		else
		{
			player.sendPacket(asl);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private static boolean isValidName(String name)
	{
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.CLAN_NAME_TEMPLATE);
		}
		catch (PatternSyntaxException e)
		{
			_log.warning("ERROR: Wrong pattern for clan name!");
			pattern = Pattern.compile(".*");
		}
		return pattern.matcher(name).matches();
	}
}
