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
package com.l2jserver.tools.images;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

/**
 * Usage of this class causes images to be loaded and kept in memory, and therefore should only be used by helper applications.<br>
 * Some icons from famfamfam (http://www.famfamfam.com/) credit *MUST* be given.
 * @author KenM
 */
public class ImagesTable
{
	private static final Map<String, ImageIcon> IMAGES = new HashMap<>();
	
	public static final String IMAGES_DIRECTORY = "../images/";
	
	public static ImageIcon getImage(String name)
	{
		if (!IMAGES.containsKey(name))
		{
			IMAGES.put(name, new ImageIcon(IMAGES_DIRECTORY + name));
		}
		return IMAGES.get(name);
	}
}