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

import static org.apache.openmeetings.util.OpenmeetingsVariables.CONFIG_MAX_UPLOAD_SIZE_KEY;
import static org.apache.openmeetings.util.OpenmeetingsVariables.CONFIG_REDIRECT_URL_FOR_EXTERNAL_KEY;
import static org.apache.openmeetings.util.OpenmeetingsVariables.CONFIG_SIP_ENABLED;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.openmeetings.core.util.IClientUtil;
import org.apache.openmeetings.db.dao.basic.ConfigurationDao;
import org.apache.openmeetings.db.dao.calendar.AppointmentDao;
import org.apache.openmeetings.db.dao.room.RoomDao;
import org.apache.openmeetings.db.dao.server.ISessionManager;
import org.apache.openmeetings.db.dao.server.SessiondataDao;
import org.apache.openmeetings.db.dao.user.IUserManager;
import org.apache.openmeetings.db.dao.user.UserDao;
import org.apache.openmeetings.db.entity.basic.Configuration;
import org.apache.openmeetings.db.entity.calendar.Appointment;
import org.apache.openmeetings.db.entity.calendar.MeetingMember;
import org.apache.openmeetings.db.entity.room.Room;
import org.apache.openmeetings.db.entity.room.RoomGroup;
import org.apache.openmeetings.db.entity.room.StreamClient;
import org.apache.openmeetings.db.entity.server.Sessiondata;
import org.apache.openmeetings.db.entity.user.GroupUser;
import org.apache.openmeetings.db.entity.user.User;
import org.apache.openmeetings.db.entity.user.User.Right;
import org.apache.openmeetings.db.entity.user.Userdata;
import org.apache.openmeetings.db.util.AuthLevelUtil;
import org.apache.openmeetings.util.OpenmeetingsVariables;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author swagner
 *
 */
public class MainService implements IPendingServiceCallback {
	private static final Logger log = Red5LoggerFactory.getLogger(MainService.class, OpenmeetingsVariables.webAppRootKey);

	@Autowired
	private ISessionManager sessionManager;
	@Autowired
	private ScopeApplicationAdapter scopeApplicationAdapter;
	@Autowired
	private SessiondataDao sessionDao;
	@Autowired
	private ConfigurationDao configurationDao;
	@Autowired
	private IUserManager userManager;
	@Autowired
	private UserDao userDao;
	@Autowired
	private RoomDao roomDao;
	@Autowired
	private AppointmentDao appointmentDao;

	// External User Types
	public static final String EXTERNAL_USER_TYPE_LDAP = "LDAP";


	/**
	 * gets a user by its SID
	 *
	 * @param sid
	 * @param userId
	 * @return - user with SID given
	 */
	public User getUser(String sid, long userId) {
		User users = new User();
		Sessiondata sd = sessionDao.check(sid);
		Set<Right> rights = userDao.getRights(sd.getUserId());
		if (AuthLevelUtil.hasAdminLevel(rights) || AuthLevelUtil.hasWebServiceLevel(rights)) {
			users = userDao.get(userId);
		} else {
			users.setFirstname("No rights to do this");
		}
		return users;
	}

	public StreamClient getCurrentRoomClient(String SID) {
		try {
			IConnection current = Red5.getConnectionLocal();
			IClient client = current.getClient();

			log.debug("getCurrentRoomClient {}, {}", SID, client.getId());

			StreamClient currentClient = sessionManager.get(IClientUtil.getId(client));
			return currentClient;
		} catch (Exception err) {
			log.error("[getCurrentRoomClient]", err);
		}
		return null;
	}

	public boolean isRoomAllowedToUser(Room r, User u) {
		boolean allowed = false;
		if (r != null) {
			if (r.isAppointment()) {
				Appointment a = appointmentDao.getByRoom(r.getId());
				if (a != null && !a.isDeleted()) {
					allowed = a.getOwner().getId().equals(u.getId());
					log.debug("[loginWicket] appointed room, isOwner ? " + allowed);
					if (!allowed) {
						for (MeetingMember mm : a.getMeetingMembers()) {
							if (mm.getUser().getId().equals(u.getId())) {
								allowed = true;
								break;
							}
						}
					}
					/*
					TODO need to be reviewed
					Calendar c = WebSession.getCalendar();
					if (c.getTime().after(a.getStart()) && c.getTime().before(a.getEnd())) {
						allowed = true;
					} else {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm"); //FIXME format
						deniedMessage = Application.getString(1271) + String.format(" %s - %s", sdf.format(a.getStart()), sdf.format(a.getEnd()));
					}
					*/
				}
			} else {
				allowed = r.getIspublic() || (r.getOwnerId() != null && r.getOwnerId().equals(u.getId()));
				log.debug("[loginWicket] public ? " + r.getIspublic() + ", ownedId ? " + r.getOwnerId() + " " + allowed);
				if (!allowed && null != r.getRoomGroups()) {
					for (RoomGroup ro : r.getRoomGroups()) {
						for (GroupUser ou : u.getGroupUsers()) {
							if (ro.getGroup().getId().equals(ou.getGroup().getId())) {
								allowed = true;
								break;
							}
						}
						if (allowed) {
							break;
						}
					}
				}
			}
		}
		return allowed;
	}

	public List<Object> loginWicket(String wicketSID, Long wicketroomid) {
		log.debug("[loginWicket] wicketSID: '{}'; wicketroomid: '{}'", wicketSID, wicketroomid);
		Sessiondata sd = sessionDao.check(wicketSID);
		Long userId = sd.getUserId();
		User u = userId == null ? null : userDao.get(userId);
		Room r = roomDao.get(wicketroomid);
		if (u != null && r != null) {
			log.debug("[loginWicket] user and roomid are not empty: " + userId + ", " + wicketroomid);
			if (wicketroomid.equals(sd.getRoomId()) || isRoomAllowedToUser(r, u)) {
				IConnection current = Red5.getConnectionLocal();
				StreamClient currentClient = sessionManager.get(IClientUtil.getId(current.getClient()));

				if (User.Type.user != u.getType() || (User.Type.user == u.getType() && !u.getGroupUsers().isEmpty())) {
					u.setSessionData(sd);
					currentClient.setUserId(u.getId());
					currentClient.setRoomId(wicketroomid);

					currentClient.setUsername(u.getLogin());
					currentClient.setFirstname(u.getFirstname());
					currentClient.setLastname(u.getLastname());
					currentClient.setPicture_uri(u.getPictureuri());
					currentClient.setEmail(u.getAddress() == null ? null : u.getAddress().getEmail());
					sessionManager.update(currentClient);

					scopeApplicationAdapter.sendMessageToCurrentScope("roomConnect", currentClient, false);

					return Arrays.<Object>asList(u, r);
				}
			}
		}
		return null;
	}

	public List<Configuration> getGeneralOptions() {
		try {
			return configurationDao.get("exclusive.audio.keycode", CONFIG_SIP_ENABLED, CONFIG_MAX_UPLOAD_SIZE_KEY, "mute.keycode", CONFIG_REDIRECT_URL_FOR_EXTERNAL_KEY);
		} catch (Exception err) {
			log.error("[getGeneralOptions]",err);
		}
		return null;
	}

	public List<Userdata> getUserdata(String sid) {
		Sessiondata sd = sessionDao.check(sid);
		if (AuthLevelUtil.hasUserLevel(userDao.getRights(sd.getUserId()))) {
			return userManager.getUserdataDashBoard(sd.getUserId());
		}
		return null;
	}

	@Override
	public void resultReceived(IPendingServiceCall arg0) {
		log.debug("[resultReceived]" + arg0);
	}
}
