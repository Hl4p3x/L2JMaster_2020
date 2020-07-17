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

import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.Config;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.data.sql.impl.TeleportLocationTable;
import com.l2jserver.gameserver.datatables.SkillData;
import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.model.ClanPrivilege;
import com.l2jserver.gameserver.model.L2TeleportLocation;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.effects.L2EffectType;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SortedWareHouseWithdrawalList;
import com.l2jserver.gameserver.network.serverpackets.SortedWareHouseWithdrawalList.WarehouseListType;
import com.l2jserver.gameserver.network.serverpackets.WareHouseDepositList;
import com.l2jserver.gameserver.network.serverpackets.WareHouseWithdrawalList;

/**
 * Fortress Foreman implementation used for: Area Teleports, Support Magic, Clan Warehouse, Exp Loss Reduction
 */
public class L2FortManagerInstance extends L2MerchantInstance
{
	private static final Logger LOG = LoggerFactory.getLogger(L2FortManagerInstance.class);
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;
	
	/**
	 * Creates a fort manager.
	 * @param template the fort manager NPC template
	 */
	public L2FortManagerInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2FortManagerInstance);
	}
	
	@Override
	public boolean isWarehouse()
	{
		return true;
	}
	
	private void sendHtmlMessage(L2PcInstance player, NpcHtmlMessage html)
	{
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getId()));
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// BypassValidation Exploit plug.
		if (player.getLastFolkNPC().getObjectId() != getObjectId())
		{
			return;
		}
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
		{
			return;
		}
		else if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			return;
		}
		else if (condition == COND_OWNER)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command
			
			String val = "";
			if (st.countTokens() >= 1)
			{
				val = st.nextToken();
			}
			if (actualCommand.equalsIgnoreCase("expel"))
			{
				if (player.hasClanPrivilege(ClanPrivilege.CS_DISMISS))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-expel.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("banish_foreigner"))
			{
				if (player.hasClanPrivilege(ClanPrivilege.CS_DISMISS))
				{
					getFort().banishForeigners(); // Move non-clan members off fortress area
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-expeled.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("receive_report"))
			{
				if (getFort().getFortState() < 2)
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-report.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					if (Config.FS_MAX_OWN_TIME > 0)
					{
						int hour = (int) Math.floor(getFort().getTimeTillRebelArmy() / 3600);
						int minutes = (int) (Math.floor(getFort().getTimeTillRebelArmy() - (hour * 3600)) / 60);
						html.replace("%hr%", String.valueOf(hour));
						html.replace("%min%", String.valueOf(minutes));
					}
					else
					{
						int hour = (int) Math.floor(getFort().getOwnedTime() / 3600);
						int minutes = (int) (Math.floor(getFort().getOwnedTime() - (hour * 3600)) / 60);
						html.replace("%hr%", String.valueOf(hour));
						html.replace("%min%", String.valueOf(minutes));
					}
					player.sendPacket(html);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-castlereport.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					int hour, minutes;
					if (Config.FS_MAX_OWN_TIME > 0)
					{
						hour = (int) Math.floor(getFort().getTimeTillRebelArmy() / 3600);
						minutes = (int) (Math.floor(getFort().getTimeTillRebelArmy() - (hour * 3600)) / 60);
						html.replace("%hr%", String.valueOf(hour));
						html.replace("%min%", String.valueOf(minutes));
					}
					else
					{
						hour = (int) Math.floor(getFort().getOwnedTime() / 3600);
						minutes = (int) (Math.floor(getFort().getOwnedTime() - (hour * 3600)) / 60);
						html.replace("%hr%", String.valueOf(hour));
						html.replace("%min%", String.valueOf(minutes));
					}
					hour = (int) Math.floor(getFort().getTimeTillNextFortUpdate() / 3600);
					minutes = (int) (Math.floor(getFort().getTimeTillNextFortUpdate() - (hour * 3600)) / 60);
					html.replace("%castle%", getFort().getContractedCastle().getName());
					html.replace("%hr2%", String.valueOf(hour));
					html.replace("%min2%", String.valueOf(minutes));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("operate_door")) // door
			// control
			{
				if (player.hasClanPrivilege(ClanPrivilege.CS_OPEN_DOOR))
				{
					if (!val.isEmpty())
					{
						boolean open = (Integer.parseInt(val) == 1);
						while (st.hasMoreTokens())
						{
							getFort().openCloseDoor(player, Integer.parseInt(st.nextToken()), open);
						}
						if (open)
						{
							final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
							html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-opened.htm");
							html.replace("%objectId%", String.valueOf(getObjectId()));
							player.sendPacket(html);
						}
						else
						{
							final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
							html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-closed.htm");
							html.replace("%objectId%", String.valueOf(getObjectId()));
							player.sendPacket(html);
						}
					}
					else
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getHtmlPrefix(), "data/html/fortress/" + getTemplate().getId() + "-d.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						html.replace("%npcname%", getName());
						player.sendPacket(html);
					}
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-noprivs.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manage_vault"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (player.hasClanPrivilege(ClanPrivilege.CL_VIEW_WAREHOUSE))
				{
					if (val.equalsIgnoreCase("deposit"))
					{
						showVaultWindowDeposit(player);
					}
					else if (val.equalsIgnoreCase("withdraw"))
					{
						if (Config.L2JMOD_ENABLE_WAREHOUSESORTING_CLAN)
						{
							String htmFile = "data/html/mods/WhSortedC.htm";
							String htmContent = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), htmFile);
							if (htmContent != null)
							{
								final NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
								npcHtmlMessage.setHtml(htmContent);
								npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
								player.sendPacket(npcHtmlMessage);
							}
							else
							{
								LOG.warn("Missing htm: {} !", htmFile);
							}
						}
						else
						{
							showVaultWindowWithdraw(player, null, (byte) 0);
						}
					}
					else
					{
						html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-vault.htm");
						sendHtmlMessage(player, html);
					}
				}
				else
				{
					html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-noprivs.htm");
					sendHtmlMessage(player, html);
				}
				return;
			}
			else if (actualCommand.startsWith("WithdrawSortedC"))
			{
				String param[] = command.split("_");
				if (param.length > 2)
				{
					showVaultWindowWithdraw(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
				}
				else if (param.length > 1)
				{
					showVaultWindowWithdraw(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
				}
				else
				{
					showVaultWindowWithdraw(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("functions"))
			{
				if (val.equalsIgnoreCase("tele"))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					if (getFort().getFunction(Fort.FUNC_TELEPORT) == null)
					{
						html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-nac.htm");
					}
					else
					{
						html.setFile(player.getHtmlPrefix(), "data/html/fortress/" + getId() + "-t" + getFort().getFunction(Fort.FUNC_TELEPORT).getLvl() + ".htm");
					}
					sendHtmlMessage(player, html);
				}
				else if (val.equalsIgnoreCase("support"))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					if (getFort().getFunction(Fort.FUNC_SUPPORT) == null)
					{
						html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-nac.htm");
					}
					else
					{
						html.setFile(player.getHtmlPrefix(), "data/html/fortress/support" + getFort().getFunction(Fort.FUNC_SUPPORT).getLvl() + ".htm");
						html.replace("%mp%", String.valueOf((int) getCurrentMp()));
					}
					sendHtmlMessage(player, html);
				}
				else if (val.equalsIgnoreCase("back"))
				{
					showChatWindow(player);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-functions.htm");
					if (getFort().getFunction(Fort.FUNC_RESTORE_EXP) != null)
					{
						html.replace("%xp_regen%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_EXP).getLvl()));
					}
					else
					{
						html.replace("%xp_regen%", "0");
					}
					if (getFort().getFunction(Fort.FUNC_RESTORE_HP) != null)
					{
						html.replace("%hp_regen%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_HP).getLvl()));
					}
					else
					{
						html.replace("%hp_regen%", "0");
					}
					if (getFort().getFunction(Fort.FUNC_RESTORE_MP) != null)
					{
						html.replace("%mp_regen%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_MP).getLvl()));
					}
					else
					{
						html.replace("%mp_regen%", "0");
					}
					sendHtmlMessage(player, html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("manage"))
			{
				if (player.hasClanPrivilege(ClanPrivilege.CS_SET_FUNCTIONS))
				{
					if (val.equalsIgnoreCase("recovery"))
					{
						if (st.countTokens() >= 1)
						{
							if (getFort().getOwnerClan() == null)
							{
								player.sendMessage("This fortress has no owner, you cannot change the configuration.");
								return;
							}
							val = st.nextToken();
							if (val.equalsIgnoreCase("hp_cancel"))
							{
								final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-cancel.htm");
								html.replace("%apply%", "recovery hp 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("mp_cancel"))
							{
								final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-cancel.htm");
								html.replace("%apply%", "recovery mp 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("exp_cancel"))
							{
								final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-cancel.htm");
								html.replace("%apply%", "recovery exp 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_hp"))
							{
								val = st.nextToken();
								final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-apply.htm");
								html.replace("%name%", "(HP Recovery Device)");
								int percent = Integer.parseInt(val);
								int cost;
								switch (percent)
								{
									case 300:
										cost = Config.FS_HPREG1_FEE;
										break;
									default: // 400
										cost = Config.FS_HPREG2_FEE;
										break;
								}
								
								html.replace("%cost%", String.valueOf(cost) + "</font>Adena /" + String.valueOf(Config.FS_HPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Day</font>)");
								html.replace("%use%", "Provides additional HP recovery for clan members in the fortress.<font color=\"00FFFF\">" + String.valueOf(percent) + "%</font>");
								html.replace("%apply%", "recovery hp " + String.valueOf(percent));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_mp"))
							{
								val = st.nextToken();
								final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-apply.htm");
								html.replace("%name%", "(MP Recovery)");
								int percent = Integer.parseInt(val);
								int cost;
								switch (percent)
								{
									case 40:
										cost = Config.FS_MPREG1_FEE;
										break;
									default: // 50
										cost = Config.FS_MPREG2_FEE;
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Adena /" + String.valueOf(Config.FS_MPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Day</font>)");
								html.replace("%use%", "Provides additional MP recovery for clan members in the fortress.<font color=\"00FFFF\">" + String.valueOf(percent) + "%</font>");
								html.replace("%apply%", "recovery mp " + String.valueOf(percent));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_exp"))
							{
								val = st.nextToken();
								final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-apply.htm");
								html.replace("%name%", "(EXP Recovery Device)");
								int percent = Integer.parseInt(val);
								int cost;
								switch (percent)
								{
									case 45:
										cost = Config.FS_EXPREG1_FEE;
										break;
									default: // 50
										cost = Config.FS_EXPREG2_FEE;
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Adena /" + String.valueOf(Config.FS_EXPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Day</font>)");
								html.replace("%use%", "Restores the Exp of any clan member who is resurrected in the fortress.<font color=\"00FFFF\">" + String.valueOf(percent) + "%</font>");
								html.replace("%apply%", "recovery exp " + String.valueOf(percent));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("hp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										LOG.debug("Mp editing invoked");
									}
									val = st.nextToken();
									final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
									html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-apply_confirmed.htm");
									if (getFort().getFunction(Fort.FUNC_RESTORE_HP) != null)
									{
										if (getFort().getFunction(Fort.FUNC_RESTORE_HP).getLvl() == Integer.parseInt(val))
										{
											html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-used.htm");
											html.replace("%val%", String.valueOf(val) + "%");
											sendHtmlMessage(player, html);
											return;
										}
									}
									int percent = Integer.parseInt(val);
									switch (percent)
									{
										case 0:
											fee = 0;
											html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-cancel_confirmed.htm");
											break;
										case 300:
											fee = Config.FS_HPREG1_FEE;
											break;
										default: // 400
											fee = Config.FS_HPREG2_FEE;
											break;
									}
									if (!getFort().updateFunctions(player, Fort.FUNC_RESTORE_HP, percent, fee, Config.FS_HPREG_FEE_RATIO, (getFort().getFunction(Fort.FUNC_RESTORE_HP) == null)))
									{
										html.setFile(player.getHtmlPrefix(), "data/html/fortress/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
							else if (val.equalsIgnoreCase("mp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										LOG.debug("Mp editing invoked");
									}
									val = st.nextToken();
									final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
									html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-apply_confirmed.htm");
									if (getFort().getFunction(Fort.FUNC_RESTORE_MP) != null)
									{
										if (getFort().getFunction(Fort.FUNC_RESTORE_MP).getLvl() == Integer.parseInt(val))
										{
											html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-used.htm");
											html.replace("%val%", String.valueOf(val) + "%");
											sendHtmlMessage(player, html);
											return;
										}
									}
									int percent = Integer.parseInt(val);
									switch (percent)
									{
										case 0:
											fee = 0;
											html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-cancel_confirmed.htm");
											break;
										case 40:
											fee = Config.FS_MPREG1_FEE;
											break;
										default: // 50
											fee = Config.FS_MPREG2_FEE;
											break;
									}
									if (!getFort().updateFunctions(player, Fort.FUNC_RESTORE_MP, percent, fee, Config.FS_MPREG_FEE_RATIO, (getFort().getFunction(Fort.FUNC_RESTORE_MP) == null)))
									{
										html.setFile(player.getHtmlPrefix(), "data/html/fortress/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
							else if (val.equalsIgnoreCase("exp"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										LOG.debug("Exp editing invoked");
									}
									val = st.nextToken();
									final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
									html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-apply_confirmed.htm");
									if (getFort().getFunction(Fort.FUNC_RESTORE_EXP) != null)
									{
										if (getFort().getFunction(Fort.FUNC_RESTORE_EXP).getLvl() == Integer.parseInt(val))
										{
											html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-used.htm");
											html.replace("%val%", String.valueOf(val) + "%");
											sendHtmlMessage(player, html);
											return;
										}
									}
									int percent = Integer.parseInt(val);
									switch (percent)
									{
										case 0:
											fee = 0;
											html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-cancel_confirmed.htm");
											break;
										case 45:
											fee = Config.FS_EXPREG1_FEE;
											break;
										default: // 50
											fee = Config.FS_EXPREG2_FEE;
											break;
									}
									if (!getFort().updateFunctions(player, Fort.FUNC_RESTORE_EXP, percent, fee, Config.FS_EXPREG_FEE_RATIO, (getFort().getFunction(Fort.FUNC_RESTORE_EXP) == null)))
									{
										html.setFile(player.getHtmlPrefix(), "data/html/fortress/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
						}
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getHtmlPrefix(), "data/html/fortress/edit_recovery.htm");
						String hp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 400\">400%</a>]";
						String exp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 45\">45%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]";
						String mp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 50\">50%</a>]";
						if (getFort().getFunction(Fort.FUNC_RESTORE_HP) != null)
						{
							html.replace("%hp_recovery%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_HP).getLvl()) + "%</font> (<font color=\"FFAABB\">" + String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_HP).getLease()) + "</font>Adena /"
								+ String.valueOf(Config.FS_HPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Day)");
							html.replace("%hp_period%", "Withdraw the fee for the next time at " + format.format(getFort().getFunction(Fort.FUNC_RESTORE_HP).getEndTime()));
							html.replace("%change_hp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Deactivate</a>]" + hp);
						}
						else
						{
							html.replace("%hp_recovery%", "none");
							html.replace("%hp_period%", "none");
							html.replace("%change_hp%", hp);
						}
						if (getFort().getFunction(Fort.FUNC_RESTORE_EXP) != null)
						{
							html.replace("%exp_recovery%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_EXP).getLvl()) + "%</font> (<font color=\"FFAABB\">" + String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_EXP).getLease()) + "</font>Adena /"
								+ String.valueOf(Config.FS_EXPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Day)");
							html.replace("%exp_period%", "Withdraw the fee for the next time at " + format.format(getFort().getFunction(Fort.FUNC_RESTORE_EXP).getEndTime()));
							html.replace("%change_exp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Deactivate</a>]" + exp);
						}
						else
						{
							html.replace("%exp_recovery%", "none");
							html.replace("%exp_period%", "none");
							html.replace("%change_exp%", exp);
						}
						if (getFort().getFunction(Fort.FUNC_RESTORE_MP) != null)
						{
							html.replace("%mp_recovery%", String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_MP).getLvl()) + "%</font> (<font color=\"FFAABB\">" + String.valueOf(getFort().getFunction(Fort.FUNC_RESTORE_MP).getLease()) + "</font>Adena /"
								+ String.valueOf(Config.FS_MPREG_FEE_RATIO / 1000 / 60 / 60 / 24) + " Day)");
							html.replace("%mp_period%", "Withdraw the fee for the next time at " + format.format(getFort().getFunction(Fort.FUNC_RESTORE_MP).getEndTime()));
							html.replace("%change_mp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Deactivate</a>]" + mp);
						}
						else
						{
							html.replace("%mp_recovery%", "none");
							html.replace("%mp_period%", "none");
							html.replace("%change_mp%", mp);
						}
						sendHtmlMessage(player, html);
					}
					else if (val.equalsIgnoreCase("other"))
					{
						if (st.countTokens() >= 1)
						{
							if (getFort().getOwnerClan() == null)
							{
								player.sendMessage("This fortress has no owner, you cannot change the configuration.");
								return;
							}
							val = st.nextToken();
							if (val.equalsIgnoreCase("tele_cancel"))
							{
								final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-cancel.htm");
								html.replace("%apply%", "other tele 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("support_cancel"))
							{
								final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-cancel.htm");
								html.replace("%apply%", "other support 0");
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_support"))
							{
								val = st.nextToken();
								final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-apply.htm");
								html.replace("%name%", "Insignia (Supplementary Magic)");
								int stage = Integer.parseInt(val);
								int cost;
								switch (stage)
								{
									case 1:
										cost = Config.FS_SUPPORT1_FEE;
										break;
									default:
										cost = Config.FS_SUPPORT2_FEE;
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Adena /" + String.valueOf(Config.FS_SUPPORT_FEE_RATIO / 1000 / 60 / 60 / 24) + " Day</font>)");
								html.replace("%use%", "Enables the use of supplementary magic.");
								html.replace("%apply%", "other support " + String.valueOf(stage));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("edit_tele"))
							{
								val = st.nextToken();
								final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-apply.htm");
								html.replace("%name%", "Mirror (Teleportation Device)");
								int stage = Integer.parseInt(val);
								int cost;
								switch (stage)
								{
									case 1:
										cost = Config.FS_TELE1_FEE;
										break;
									default:
										cost = Config.FS_TELE2_FEE;
										break;
								}
								html.replace("%cost%", String.valueOf(cost) + "</font>Adena /" + String.valueOf(Config.FS_TELE_FEE_RATIO / 1000 / 60 / 60 / 24) + " Day</font>)");
								html.replace("%use%", "Teleports clan members in a fort to the target <font color=\"00FFFF\">Stage " + String.valueOf(stage) + "</font> staging area");
								html.replace("%apply%", "other tele " + String.valueOf(stage));
								sendHtmlMessage(player, html);
								return;
							}
							else if (val.equalsIgnoreCase("tele"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										LOG.debug("Tele editing invoked");
									}
									val = st.nextToken();
									final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
									html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-apply_confirmed.htm");
									if (getFort().getFunction(Fort.FUNC_TELEPORT) != null)
									{
										if (getFort().getFunction(Fort.FUNC_TELEPORT).getLvl() == Integer.parseInt(val))
										{
											html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-used.htm");
											html.replace("%val%", "Stage " + String.valueOf(val));
											sendHtmlMessage(player, html);
											return;
										}
									}
									int lvl = Integer.parseInt(val);
									switch (lvl)
									{
										case 0:
											fee = 0;
											html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-cancel_confirmed.htm");
											break;
										case 1:
											fee = Config.FS_TELE1_FEE;
											break;
										default:
											fee = Config.FS_TELE2_FEE;
											break;
									}
									if (!getFort().updateFunctions(player, Fort.FUNC_TELEPORT, lvl, fee, Config.FS_TELE_FEE_RATIO, (getFort().getFunction(Fort.FUNC_TELEPORT) == null)))
									{
										html.setFile(player.getHtmlPrefix(), "data/html/fortress/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									sendHtmlMessage(player, html);
								}
								return;
							}
							else if (val.equalsIgnoreCase("support"))
							{
								if (st.countTokens() >= 1)
								{
									int fee;
									if (Config.DEBUG)
									{
										LOG.debug("Support editing invoked");
									}
									val = st.nextToken();
									final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
									html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-apply_confirmed.htm");
									if (getFort().getFunction(Fort.FUNC_SUPPORT) != null)
									{
										if (getFort().getFunction(Fort.FUNC_SUPPORT).getLvl() == Integer.parseInt(val))
										{
											html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-used.htm");
											html.replace("%val%", "Stage " + String.valueOf(val));
											sendHtmlMessage(player, html);
											return;
										}
									}
									int lvl = Integer.parseInt(val);
									switch (lvl)
									{
										case 0:
											fee = 0;
											html.setFile(player.getHtmlPrefix(), "data/html/fortress/functions-cancel_confirmed.htm");
											break;
										case 1:
											fee = Config.FS_SUPPORT1_FEE;
											break;
										default:
											fee = Config.FS_SUPPORT2_FEE;
											break;
									}
									if (!getFort().updateFunctions(player, Fort.FUNC_SUPPORT, lvl, fee, Config.FS_SUPPORT_FEE_RATIO, (getFort().getFunction(Fort.FUNC_SUPPORT) == null)))
									{
										html.setFile(player.getHtmlPrefix(), "data/html/fortress/low_adena.htm");
										sendHtmlMessage(player, html);
									}
									else
									{
										sendHtmlMessage(player, html);
									}
								}
								return;
							}
						}
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getHtmlPrefix(), "data/html/fortress/edit_other.htm");
						String tele = "[<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">Level 2</a>]";
						String support = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>]";
						if (getFort().getFunction(Fort.FUNC_TELEPORT) != null)
						{
							html.replace("%tele%", "Stage " + String.valueOf(getFort().getFunction(Fort.FUNC_TELEPORT).getLvl()) + "</font> (<font color=\"FFAABB\">" + String.valueOf(getFort().getFunction(Fort.FUNC_TELEPORT).getLease()) + "</font>Adena /"
								+ String.valueOf(Config.FS_TELE_FEE_RATIO / 1000 / 60 / 60 / 24) + " Day)");
							html.replace("%tele_period%", "Withdraw the fee for the next time at " + format.format(getFort().getFunction(Fort.FUNC_TELEPORT).getEndTime()));
							html.replace("%change_tele%", "[<a action=\"bypass -h npc_%objectId%_manage other tele_cancel\">Deactivate</a>]" + tele);
						}
						else
						{
							html.replace("%tele%", "none");
							html.replace("%tele_period%", "none");
							html.replace("%change_tele%", tele);
						}
						if (getFort().getFunction(Fort.FUNC_SUPPORT) != null)
						{
							html.replace("%support%", "Stage " + String.valueOf(getFort().getFunction(Fort.FUNC_SUPPORT).getLvl()) + "</font> (<font color=\"FFAABB\">" + String.valueOf(getFort().getFunction(Fort.FUNC_SUPPORT).getLease()) + "</font>Adena /"
								+ String.valueOf(Config.FS_SUPPORT_FEE_RATIO / 1000 / 60 / 60 / 24) + " Day)");
							html.replace("%support_period%", "Withdraw the fee for the next time at " + format.format(getFort().getFunction(Fort.FUNC_SUPPORT).getEndTime()));
							html.replace("%change_support%", "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Deactivate</a>]" + support);
						}
						else
						{
							html.replace("%support%", "none");
							html.replace("%support_period%", "none");
							html.replace("%change_support%", support);
						}
						sendHtmlMessage(player, html);
					}
					else if (val.equalsIgnoreCase("back"))
					{
						showChatWindow(player);
					}
					else
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player.getHtmlPrefix(), "data/html/fortress/manage.htm");
						sendHtmlMessage(player, html);
					}
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-noprivs.htm");
					sendHtmlMessage(player, html);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("support"))
			{
				setTarget(player);
				Skill skill;
				if (val.isEmpty())
				{
					return;
				}
				
				try
				{
					int skill_id = Integer.parseInt(val);
					try
					{
						if (getFort().getFunction(Fort.FUNC_SUPPORT) == null)
						{
							return;
						}
						if (getFort().getFunction(Fort.FUNC_SUPPORT).getLvl() == 0)
						{
							return;
						}
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						int skill_lvl = 0;
						if (st.countTokens() >= 1)
						{
							skill_lvl = Integer.parseInt(st.nextToken());
						}
						skill = SkillData.getInstance().getSkill(skill_id, skill_lvl);
						if (skill.hasEffectType(L2EffectType.SUMMON))
						{
							player.doCast(skill);
						}
						else
						{
							if (!((skill.getMpConsume1() + skill.getMpConsume2()) > getCurrentMp()) || Config.FORTRESS_BUFF_FREE)
							{
								this.doCast(skill);
							}
							else
							{
								html.setFile(player.getHtmlPrefix(), "data/html/fortress/support-no_mana.htm");
								html.replace("%mp%", String.valueOf((int) getCurrentMp()));
								sendHtmlMessage(player, html);
								return;
							}
						}
						html.setFile(player.getHtmlPrefix(), "data/html/fortress/support-done.htm");
						html.replace("%mp%", String.valueOf((int) getCurrentMp()));
						sendHtmlMessage(player, html);
					}
					catch (Exception e)
					{
						player.sendMessage("Invalid skill level, contact your admin!");
					}
				}
				catch (Exception e)
				{
					player.sendMessage("Invalid skill level, contact your admin!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("support_back"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (getFort().getFunction(Fort.FUNC_SUPPORT).getLvl() == 0)
				{
					return;
				}
				html.setFile(player.getHtmlPrefix(), "data/html/fortress/support" + getFort().getFunction(Fort.FUNC_SUPPORT).getLvl() + ".htm");
				html.replace("%mp%", String.valueOf((int) getStatus().getCurrentMp()));
				sendHtmlMessage(player, html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("goto"))
			{
				int whereTo = Integer.parseInt(val);
				doTeleport(player, whereTo);
				return;
			}
			super.onBypassFeedback(player, command);
		}
	}
	
	@Override
	public void showChatWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/fortress/foreman-no.htm";
		
		int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			{
				filename = "data/html/fortress/foreman-busy.htm"; // Busy because of siege
			}
			else if (condition == COND_OWNER)
			{
				filename = "data/html/fortress/foreman.htm"; // Owner message window
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getHtmlPrefix(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	private void doTeleport(L2PcInstance player, int val)
	{
		if (Config.DEBUG)
		{
			LOG.debug("doTeleport(L2PcInstance player, int val) is called");
		}
		L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
		if (list != null)
		{
			if (player.destroyItemByItemId("Teleport", list.getItemId(), list.getPrice(), this, true))
			{
				if (Config.DEBUG)
				{
					LOG.debug("Teleporting player " + player.getName() + " for Fortress to new location: " + list.getLocX() + ":" + list.getLocY() + ":" + list.getLocZ());
				}
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ());
			}
		}
		else
		{
			LOG.debug("No teleport destination with id:" + val);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	protected int validateCondition(L2PcInstance player)
	{
		if ((getFort() != null) && (getFort().getResidenceId() > 0))
		{
			if (player.getClan() != null)
			{
				if (getFort().getZone().isActive())
				{
					return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
				}
				else if ((getFort().getOwnerClan() != null) && (getFort().getOwnerClan().getId() == player.getClanId()))
				{
					return COND_OWNER; // Owner
				}
			}
		}
		return COND_ALL_FALSE;
	}
	
	private void showVaultWindowDeposit(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getClan().getWarehouse());
		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.CLAN));
	}
	
	private void showVaultWindowWithdraw(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
		if (player.isClanLeader() || player.hasClanPrivilege(ClanPrivilege.CL_VIEW_WAREHOUSE))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.setActiveWarehouse(player.getClan().getWarehouse());
			if (itemtype != null)
			{
				player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN, itemtype, sortorder));
			}
			else
			{
				player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
			}
		}
		else
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getHtmlPrefix(), "data/html/fortress/foreman-noprivs.htm");
			sendHtmlMessage(player, html);
		}
	}
}