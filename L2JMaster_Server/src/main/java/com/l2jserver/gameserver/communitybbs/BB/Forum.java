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
package com.l2jserver.gameserver.communitybbs.BB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.commons.database.pool.impl.ConnectionFactory;
import com.l2jserver.gameserver.communitybbs.Manager.ForumsBBSManager;
import com.l2jserver.gameserver.communitybbs.Manager.MemoBBSManager;

public class Forum
{
	// type
	public static final int ROOT = 0;
	public static final int NORMAL = 1;
	public static final int CLAN = 2;
	public static final int MEMO = 3;
	public static final int MAIL = 4;
	// perm
	public static final int INVISIBLE = 0;
	public static final int ALL = 1;
	public static final int CLANMEMBERONLY = 2;
	public static final int OWNERONLY = 3;
	
	private static Logger _log = LoggerFactory.getLogger(Forum.class);
	private final List<Forum> _children = new ArrayList<>();
	private final Map<Integer, Topic> _topic = new ConcurrentHashMap<>();
	private final int _forumId;
	private String _forumName;
	private int _forumType;
	private int _forumPost;
	private int _forumPerm;
	private final Forum _fParent;
	private int _ownerID;
	private boolean _loaded = false;
	
	/**
	 * Creates new instance of Forum. When you create new forum, use {@link com.l2jserver.gameserver.communitybbs.Manager.ForumsBBSManager# addForum(com.l2jserver.gameserver.communitybbs.BB.Forum)} to add forum to the forums manager.
	 * @param Forumid
	 * @param FParent
	 */
	public Forum(int Forumid, Forum FParent)
	{
		_forumId = Forumid;
		_fParent = FParent;
	}
	
	/**
	 * @param name
	 * @param parent
	 * @param type
	 * @param perm
	 * @param OwnerID
	 */
	public Forum(String name, Forum parent, int type, int perm, int OwnerID)
	{
		_forumName = name;
		_forumId = ForumsBBSManager.getInstance().getANewID();
		_forumType = type;
		_forumPost = 0;
		_forumPerm = perm;
		_fParent = parent;
		_ownerID = OwnerID;
		parent._children.add(this);
		ForumsBBSManager.getInstance().addForum(this);
		_loaded = true;
	}
	
	private void load()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM forums WHERE forum_id=?"))
		{
			ps.setInt(1, _forumId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					_forumName = rs.getString("forum_name");
					_forumPost = rs.getInt("forum_post");
					_forumType = rs.getInt("forum_type");
					_forumPerm = rs.getInt("forum_perm");
					_ownerID = rs.getInt("forum_owner_id");
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Data error on Forum " + _forumId + " : " + e.getMessage(), e);
		}
		
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM topic WHERE topic_forum_id=? ORDER BY topic_id DESC"))
		{
			ps.setInt(1, _forumId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					Topic t = new Topic(Topic.ConstructorType.RESTORE, rs.getInt("topic_id"), rs.getInt("topic_forum_id"), rs.getString("topic_name"), rs.getLong("topic_date"), rs.getString("topic_ownername"), rs.getInt("topic_ownerid"), rs.getInt("topic_type"), rs.getInt("topic_reply"));
					_topic.put(t.getID(), t);
					if (t.getID() > MemoBBSManager.getInstance().getMaxID(this))
					{
						MemoBBSManager.getInstance().setMaxID(t.getID(), this);
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Data error on Forum " + _forumId + " : " + e.getMessage(), e);
		}
	}
	
	private void getChildren()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT forum_id FROM forums WHERE forum_parent=?"))
		{
			ps.setInt(1, _forumId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Forum f = new Forum(rs.getInt("forum_id"), this);
					_children.add(f);
					ForumsBBSManager.getInstance().addForum(f);
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Data error on Forum (children): " + e.getMessage(), e);
		}
	}
	
	public int getTopicSize()
	{
		vload();
		return _topic.size();
	}
	
	public Topic getTopic(int j)
	{
		vload();
		return _topic.get(j);
	}
	
	public void addTopic(Topic t)
	{
		vload();
		_topic.put(t.getID(), t);
	}
	
	/**
	 * @return the forum Id
	 */
	public int getID()
	{
		return _forumId;
	}
	
	public String getName()
	{
		vload();
		return _forumName;
	}
	
	public int getType()
	{
		vload();
		return _forumType;
	}
	
	/**
	 * @param name the forum name
	 * @return the forum for the given name
	 */
	public Forum getChildByName(String name)
	{
		vload();
		return _children.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
	}
	
	/**
	 * @param id
	 */
	public void rmTopicByID(int id)
	{
		_topic.remove(id);
	}
	
	public void insertIntoDb()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO forums (forum_id,forum_name,forum_parent,forum_post,forum_type,forum_perm,forum_owner_id) VALUES (?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, _forumId);
			ps.setString(2, _forumName);
			ps.setInt(3, _fParent.getID());
			ps.setInt(4, _forumPost);
			ps.setInt(5, _forumType);
			ps.setInt(6, _forumPerm);
			ps.setInt(7, _ownerID);
			ps.execute();
		}
		catch (Exception e)
		{
			// _log.warn("Error while saving new Forum to db " + e.getMessage(), e);
		}
	}
	
	public void vload()
	{
		if (!_loaded)
		{
			load();
			getChildren();
			_loaded = true;
		}
	}
}