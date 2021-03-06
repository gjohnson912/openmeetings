/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.db.dto.file;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.openmeetings.db.entity.file.FileExplorerItem;

/**
 * @author sebastianwagner
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FileExplorerObject implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private List<FileExplorerItemDTO> userHome;
	private List<FileExplorerItemDTO> roomHome;
	private Long userHomeSize;
	private Long roomHomeSize;

	public FileExplorerObject() {}

	public List<FileExplorerItemDTO> getUserHome() {
		return userHome;
	}

	public void setUserHome(List<FileExplorerItemDTO> userHome) {
		this.userHome = userHome;
	}

	public void setUser(List<FileExplorerItem> list, long size) {
		this.userHome = FileExplorerItemDTO.list(list);
		this.userHomeSize = size;
	}

	public List<FileExplorerItemDTO> getRoomHome() {
		return roomHome;
	}

	public void setRoomHome(List<FileExplorerItemDTO> roomHome) {
		this.roomHome = roomHome;
	}

	public void setRoom(List<FileExplorerItem> list, long size) {
		this.roomHome = FileExplorerItemDTO.list(list);
		this.roomHomeSize = size;
	}

	public Long getUserHomeSize() {
		return userHomeSize;
	}

	public void setUserHomeSize(Long userHomeSize) {
		this.userHomeSize = userHomeSize;
	}

	public Long getRoomHomeSize() {
		return roomHomeSize;
	}

	public void setRoomHomeSize(Long roomHomeSize) {
		this.roomHomeSize = roomHomeSize;
	}
}
