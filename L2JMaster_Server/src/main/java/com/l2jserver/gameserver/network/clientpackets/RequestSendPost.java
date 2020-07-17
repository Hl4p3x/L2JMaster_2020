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
package com.l2jserver.gameserver.network.clientpackets;

import static com.l2jserver.gameserver.model.itemcontainer.Inventory.ADENA_ID;
import static com.l2jserver.gameserver.model.itemcontainer.Inventory.MAX_ADENA;

import com.l2jserver.Config;
import com.l2jserver.gameserver.data.sql.impl.CharNameTable;
import com.l2jserver.gameserver.data.xml.impl.AdminData;
import com.l2jserver.gameserver.enums.PrivateStoreType;
import com.l2jserver.gameserver.instancemanager.MailManager;
import com.l2jserver.gameserver.model.BlockList;
import com.l2jserver.gameserver.model.L2AccessLevel;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Message;
import com.l2jserver.gameserver.model.itemcontainer.Mail;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExNoticePostSent;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.ItemList;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.util.StringUtil;

/**
 * @author Migi, DS
 */
public final class RequestSendPost extends L2GameClientPacket
{
	private static final String _C__D0_66_REQUESTSENDPOST = "[C] D0:66 RequestSendPost";
	
	private static final int BATCH_LENGTH = 12; // length of the one item
	
	private static final int MAX_RECV_LENGTH = 16;
	private static final int MAX_SUBJ_LENGTH = 128;
	private static final int MAX_TEXT_LENGTH = 512;
	private static final int MAX_ATTACHMENTS = 8;
	private static final int INBOX_SIZE = 240;
	private static final int OUTBOX_SIZE = 240;
	
	private static final int MESSAGE_FEE = 100;
	private static final int MESSAGE_FEE_PER_SLOT = 1000; // 100 adena message fee + 1000 per each item slot
	
	private String _receiver;
	private boolean _isCod;
	private String _subject;
	private String _text;
	private AttachmentItem _items[] = null;
	private long _reqAdena;
	
	public RequestSendPost()
	{
	}
	
	@Override
	protected void readImpl()
	{
		_receiver = readS();
		_isCod = readD() == 0 ? false : true;
		_subject = readS();
		_text = readS();
		
		int attachCount = readD();
		if ((attachCount < 0) || (attachCount > Config.MAX_ITEM_IN_PACKET) || (((attachCount * BATCH_LENGTH) + 8) != _buf.remaining()))
		{
			return;
		}
		
		if (attachCount > 0)
		{
			_items = new AttachmentItem[attachCount];
			for (int i = 0; i < attachCount; i++)
			{
				int objectId = readD();
				long count = readQ();
				if ((objectId < 1) || (count < 0))
				{
					_items = null;
					return;
				}
				_items[i] = new AttachmentItem(objectId, count);
			}
		}
		
		_reqAdena = readQ();
	}
	
	@Override
	public void runImpl()
	{
		if (!Config.ALLOW_MAIL)
		{
			return;
		}
		
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (!Config.ALLOW_ATTACHMENTS)
		{
			_items = null;
			_isCod = false;
			_reqAdena = 0;
		}
		
		if (!activeChar.getAccessLevel().allowTransaction())
		{
			activeChar.sendMessage("Transactions are disabled for your Access Level.");
			return;
		}
		
		if (!activeChar.isInsideZone(ZoneId.PEACE) && (_items != null))
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_NOT_IN_PEACE_ZONE);
			return;
		}
		
		if (activeChar.getActiveTradeList() != null)
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_DURING_EXCHANGE);
			return;
		}
		
		if (activeChar.isEnchanting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_DURING_ENCHANT);
			return;
		}
		
		if (activeChar.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_PRIVATE_STORE);
			return;
		}
		
		if (_receiver.length() > MAX_RECV_LENGTH)
		{
			activeChar.sendPacket(SystemMessageId.ALLOWED_LENGTH_FOR_RECIPIENT_EXCEEDED);
			return;
		}
		
		if (_subject.length() > MAX_SUBJ_LENGTH)
		{
			activeChar.sendPacket(SystemMessageId.ALLOWED_LENGTH_FOR_TITLE_EXCEEDED);
			return;
		}
		
		if (_text.length() > MAX_TEXT_LENGTH)
		{
			// not found message for this
			activeChar.sendPacket(SystemMessageId.ALLOWED_LENGTH_FOR_TITLE_EXCEEDED);
			return;
		}
		
		if ((_items != null) && (_items.length > MAX_ATTACHMENTS))
		{
			activeChar.sendPacket(SystemMessageId.ITEM_SELECTION_POSSIBLE_UP_TO_8);
			return;
		}
		
		if ((_reqAdena < 0) || (_reqAdena > MAX_ADENA))
		{
			return;
		}
		
		if (_isCod)
		{
			if (_reqAdena == 0)
			{
				activeChar.sendPacket(SystemMessageId.PAYMENT_AMOUNT_NOT_ENTERED);
				return;
			}
			if ((_items == null) || (_items.length == 0))
			{
				activeChar.sendPacket(SystemMessageId.PAYMENT_REQUEST_NO_ITEM);
				return;
			}
		}
		
		final int receiverId = CharNameTable.getInstance().getIdByName(_receiver);
		if (receiverId <= 0)
		{
			activeChar.sendPacket(SystemMessageId.RECIPIENT_NOT_EXIST);
			return;
		}
		
		if (receiverId == activeChar.getObjectId())
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANT_SEND_MAIL_TO_YOURSELF);
			return;
		}
		
		final int level = CharNameTable.getInstance().getAccessLevelById(receiverId);
		final L2AccessLevel accessLevel = AdminData.getInstance().getAccessLevel(level);
		
		if (accessLevel.isGm() && !activeChar.getAccessLevel().isGm())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CANNOT_MAIL_GM_C1);
			sm.addString(_receiver);
			activeChar.sendPacket(sm);
			return;
		}
		
		if (activeChar.isJailed() && ((Config.JAIL_DISABLE_TRANSACTION && (_items != null)) || Config.JAIL_DISABLE_CHAT))
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_NOT_IN_PEACE_ZONE);
			return;
		}
		
		if (BlockList.isInBlockList(receiverId, activeChar.getObjectId()))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_BLOCKED_YOU_CANNOT_MAIL);
			sm.addString(_receiver);
			activeChar.sendPacket(sm);
			return;
		}
		
		if (MailManager.getInstance().getOutboxSize(activeChar.getObjectId()) >= OUTBOX_SIZE)
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_MAIL_LIMIT_EXCEEDED);
			return;
		}
		
		if (MailManager.getInstance().getInboxSize(receiverId) >= INBOX_SIZE)
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_MAIL_LIMIT_EXCEEDED);
			return;
		}
		
		if (!getClient().getFloodProtectors().getSendMail().tryPerformAction("sendmail"))
		{
			activeChar.sendPacket(SystemMessageId.CANT_FORWARD_LESS_THAN_MINUTE);
			return;
		}
		
		Message msg = new Message(activeChar.getObjectId(), receiverId, _isCod, _subject, _text, _reqAdena);
		if (removeItems(activeChar, msg))
		{
			MailManager.getInstance().sendMessage(msg);
			activeChar.sendPacket(ExNoticePostSent.valueOf(true));
			activeChar.sendPacket(SystemMessageId.MAIL_SUCCESSFULLY_SENT);
		}
	}
	
	private final boolean removeItems(L2PcInstance player, Message msg)
	{
		long currentAdena = player.getAdena();
		long fee = MESSAGE_FEE;
		
		if (_items != null)
		{
			for (AttachmentItem i : _items)
			{
				// Check validity of requested item
				L2ItemInstance item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "attach");
				if ((item == null) || !item.isTradeable() || item.isEquipped())
				{
					player.sendPacket(SystemMessageId.CANT_FORWARD_BAD_ITEM);
					return false;
				}
				
				fee += MESSAGE_FEE_PER_SLOT;
				
				if (item.getId() == ADENA_ID)
				{
					currentAdena -= i.getCount();
				}
			}
		}
		
		// Check if enough adena and charge the fee
		if ((currentAdena < fee) || !player.reduceAdena("MailFee", fee, null, false))
		{
			player.sendPacket(SystemMessageId.CANT_FORWARD_NO_ADENA);
			return false;
		}
		
		if (_items == null)
		{
			return true;
		}
		
		Mail attachments = msg.createAttachments();
		
		// message already has attachments ? oO
		if (attachments == null)
		{
			return false;
		}
		
		final StringBuilder recv = new StringBuilder(32);
		StringUtil.append(recv, msg.getReceiverName(), "[", String.valueOf(msg.getReceiverId()), "]");
		final String receiver = recv.toString();
		
		// Proceed to the transfer
		InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (AttachmentItem i : _items)
		{
			// Check validity of requested item
			L2ItemInstance oldItem = player.checkItemManipulation(i.getObjectId(), i.getCount(), "attach");
			if ((oldItem == null) || !oldItem.isTradeable() || oldItem.isEquipped())
			{
				_log.warning("Error adding attachment for char " + player.getName() + " (olditem == null)");
				return false;
			}
			
			final L2ItemInstance newItem = player.getInventory().transferItem("SendMail", i.getObjectId(), i.getCount(), attachments, player, receiver);
			if (newItem == null)
			{
				_log.warning("Error adding attachment for char " + player.getName() + " (newitem == null)");
				continue;
			}
			newItem.setItemLocation(newItem.getItemLocation(), msg.getId());
			
			if (playerIU != null)
			{
				if ((oldItem.getCount() > 0) && (oldItem != newItem))
				{
					playerIU.addModifiedItem(oldItem);
				}
				else
				{
					playerIU.addRemovedItem(oldItem);
				}
			}
		}
		
		// Send updated item list to the player
		if (playerIU != null)
		{
			player.sendPacket(playerIU);
		}
		else
		{
			player.sendPacket(new ItemList(player, false));
		}
		
		// Update current load status on player
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		
		return true;
	}
	
	private static class AttachmentItem
	{
		private final int _objectId;
		private final long _count;
		
		public AttachmentItem(int id, long num)
		{
			_objectId = id;
			_count = num;
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public long getCount()
		{
			return _count;
		}
	}
	
	@Override
	public String getType()
	{
		return _C__D0_66_REQUESTSENDPOST;
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}
