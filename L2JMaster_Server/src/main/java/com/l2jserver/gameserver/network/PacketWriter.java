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
package com.l2jserver.gameserver.network;

import io.netty.buffer.ByteBuf;

public final class PacketWriter
{
	private final ByteBuf _buf;
	
	public PacketWriter(ByteBuf buf)
	{
		this._buf = buf;
	}
	
	public int getWritableBytes()
	{
		return this._buf.writableBytes();
	}
	
	public void writeC(int value)
	{
		this._buf.writeByte(value);
	}
	
	public void writeH(int value)
	{
		this._buf.writeShortLE(value);
	}
	
	public void writeD(int value)
	{
		this._buf.writeIntLE(value);
	}
	
	public void writeQ(long value)
	{
		this._buf.writeLongLE(value);
	}
	
	public void writeE(float value)
	{
		this._buf.writeIntLE(Float.floatToIntBits(value));
	}
	
	public void writeF(double value)
	{
		this._buf.writeLongLE(Double.doubleToLongBits(value));
	}
	
	public void writeS(String value)
	{
		if (value != null)
		{
			for (int i = 0; i < value.length(); i++)
			{
				this._buf.writeChar(Character.reverseBytes(value.charAt(i)));
			}
		}
		this._buf.writeChar(0);
	}
	
	public void writeString(String value)
	{
		if (value != null)
		{
			this._buf.writeShortLE(value.length());
			for (int i = 0; i < value.length(); i++)
			{
				this._buf.writeChar(Character.reverseBytes(value.charAt(i)));
			}
		}
		else
		{
			this._buf.writeShort(0);
		}
	}
	
	public void writeB(byte[] bytes)
	{
		this._buf.writeBytes(bytes);
	}
}