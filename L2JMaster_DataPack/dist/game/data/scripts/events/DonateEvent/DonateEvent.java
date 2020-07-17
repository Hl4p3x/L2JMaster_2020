/* Copyright (C) 2004-2020 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package events.DonateEvent;

import com.l2jserver.gameserver.data.xml.impl.NpcData;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.event.LongTimeEvent;

/**
 * @author MaGa
 */
public class DonateEvent extends LongTimeEvent
{
	public DonateEvent()
	{
		super(DonateEvent.class.getSimpleName(), "events");
		searchAllMonsters();
	}
	
	private static final int COIN = 23003;
	private static final int COIN_QTY = 1;
	private static final int REWARD = 3470;
	
	private void searchAllMonsters()
	{
		for (L2NpcTemplate npc : NpcData.getInstance().getAllNpcOfClassType("L2Monster"))
		{
//			if (!Config.SPAWN_RETURN_LIST_EXCLUDED.isEmpty() && Config.SPAWN_RETURN_LIST_EXCLUDED.contains(npc.getId()))
//			{
//				continue;
//			}
			setAttackableAggroRangeEnterId(event -> notifyAggroRangeEnter(event.getNpc(), event.getActiveChar(), event.isSummon()), npc.getId());
			addAttackId(npc.getId());
			addKillId(npc.getId());
		}
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		if (npc instanceof L2MonsterInstance)
		{

			if (killer.getInventory().getAllItemsByItemId(COIN).length >= COIN_QTY)
			{
				giveItems(killer, REWARD, 1);
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	public static void main(String[] args)
	{
		new DonateEvent();
	}
}