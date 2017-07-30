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
package org.apache.openmeetings.core.remote;

import static org.apache.openmeetings.util.OpenmeetingsVariables.webAppRootKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.openmeetings.core.data.whiteboard.WhiteboardManager;
import org.apache.openmeetings.core.documents.LibraryChartLoader;
import org.apache.openmeetings.core.documents.LibraryDocumentConverter;
import org.apache.openmeetings.core.documents.LibraryWmlLoader;
import org.apache.openmeetings.db.dao.file.FileExplorerItemDao;
import org.apache.openmeetings.db.dao.server.ISessionManager;
import org.apache.openmeetings.db.dao.server.SessiondataDao;
import org.apache.openmeetings.db.dao.user.UserDao;
import org.apache.openmeetings.db.entity.file.FileExplorerItem;
import org.apache.openmeetings.db.entity.file.FileItem;
import org.apache.openmeetings.db.entity.file.FileItem.Type;
import org.apache.openmeetings.db.entity.room.StreamClient;
import org.apache.openmeetings.db.entity.server.Sessiondata;
import org.apache.openmeetings.db.util.AuthLevelUtil;
import org.apache.openmeetings.util.OmFileHelper;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author swagner
 *
 */
public class ConferenceLibrary implements IPendingServiceCallback {
	private static final Logger log = Red5LoggerFactory.getLogger(ConferenceLibrary.class, webAppRootKey);

	@Autowired
	private ISessionManager sessionManager;
	@Autowired
	private SessiondataDao sessionDao;
	@Autowired
	private UserDao userDao;
	@Autowired
	private FileExplorerItemDao fileDao;
	@Autowired
	private WhiteboardManager whiteboardManager;
	@Autowired
	private ScopeApplicationAdapter scopeAdapter;

	/**
	 *
	 * Save an Object to the library and returns the file-explorer Id
	 *
	 * @param sid
	 * @param roomId
	 * @param fileName
	 * @param tObjectRef
	 * @return - file-explorer Id in case of success, -1 otherwise
	 */
	public Long saveAsObject(String sid, Long roomId, String fileName, Object tObjectRef) {
		try {
			Sessiondata sd = sessionDao.check(sid);
			if (AuthLevelUtil.hasUserLevel(userDao.getRights(sd.getUserId()))) {
				// LinkedHashMap tObject = (LinkedHashMap)t;
				// ArrayList tObject = (ArrayList)t;

				log.debug("saveAsObject :1: " + tObjectRef);
				log.debug("saveAsObject :2: " + tObjectRef.getClass().getName());

				@SuppressWarnings("rawtypes")
				ArrayList tObject = (ArrayList) tObjectRef;

				log.debug("saveAsObject" + tObject.size());

				FileExplorerItem file = fileDao.add(fileName, null, null, roomId, sd.getUserId(), Type.WmlFile, "", "");
				LibraryDocumentConverter.writeToLocalFolder(file.getHash(), tObject);

				return file.getId();
			}
		} catch (Exception err) {
			log.error("[saveAsObject] ", err);
		}
		return -1L;
	}

	/**
	 * Loads a Object from the library into the whiteboard of all participant of
	 * the current room
	 *
	 * @param uid - uid of the client performing operation
	 * @param wbId - id of whiteboard
	 * @param fi - FileItem of the Wml being loaded
	 */
	public void sendToWhiteboard(String uid, Long wbId, FileItem fi) {
		StreamClient client = sessionManager.get(uid);
		if (client == null) {
			log.warn("No client was found to send Wml:: {}", uid);
			return;
		}

		List<?> roomItems = LibraryWmlLoader.loadWmlFile(fi.getHash());

		Map<Integer, Object> wbClear = new HashMap<>();
		wbClear.put(2, "clear");
		wbClear.put(3, null);

		Long roomId = client.getRoomId();
		whiteboardManager.add(roomId, wbClear, wbId);

		for (int k = 0; k < roomItems.size(); k++) {
			List<?> actionObject = (List<?>)roomItems.get(k);

			Map<Integer, Object> whiteboardObj = new HashMap<>();
			whiteboardObj.put(2, "draw");
			whiteboardObj.put(3, actionObject);

			whiteboardManager.add(roomId, whiteboardObj, wbId);
		}

		Map<String, Object> sendObject = new HashMap<>();
		sendObject.put("id", wbId);
		sendObject.put("roomitems", roomItems);

		scopeAdapter.sendToScope(roomId, "loadWmlToWhiteboardById", sendObject);
	}

	/**
	 *
	 * Loads a chart object
	 *
	 * @param sid
	 * @param room_id
	 * @param fileName
	 * @return - chart object
	 */
	@SuppressWarnings("rawtypes")
	public ArrayList loadChartObject(String sid, Long room_id, String fileName) {
		try {
			Sessiondata sd = sessionDao.check(sid);
			if (AuthLevelUtil.hasUserLevel(userDao.getRights(sd.getUserId()))) {
				return LibraryChartLoader.getInstance().loadChart(OmFileHelper.getUploadRoomDir(room_id.toString()), fileName);
			}
		} catch (Exception err) {
			log.error("[loadChartObject] ", err);
		}
		return null;
	}

	@Override
	public void resultReceived(IPendingServiceCall arg0) {
		// TODO Auto-generated method stub
	}
}
