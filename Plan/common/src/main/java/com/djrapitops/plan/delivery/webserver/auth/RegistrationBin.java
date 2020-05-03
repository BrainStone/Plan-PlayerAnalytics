/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.delivery.webserver.auth;

import com.djrapitops.plan.delivery.domain.auth.User;
import com.djrapitops.plan.utilities.PassEncryptUtil;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.*;

/**
 * Holds registrations of users before they are confirmed.
 *
 * @author Rsl1122
 */
public class RegistrationBin {

    private static final Map<String, AwaitingForRegistration> REGISTRATION_BIN = new HashMap<>();

    public static String addInfoForRegistration(String username, String password) {
        String hash = PassEncryptUtil.createHash(password);
        String code = DigestUtils.sha256Hex(username + password + System.currentTimeMillis()).substring(0, 12);
        REGISTRATION_BIN.put(code, new AwaitingForRegistration(username, hash));
        return code;
    }

    public static Optional<User> register(String code, UUID linkedToUUID) {
        AwaitingForRegistration found = REGISTRATION_BIN.get(code);
        if (found == null) return Optional.empty();
        REGISTRATION_BIN.remove(code);
        return Optional.of(found.toUser(linkedToUUID));
    }

    public static boolean contains(String code) {
        return REGISTRATION_BIN.containsKey(code);
    }

    private static class AwaitingForRegistration {
        private final String username;
        private final String passwordHash;

        public AwaitingForRegistration(String username, String passwordHash) {
            this.username = username;
            this.passwordHash = passwordHash;
        }

        public User toUser(UUID linkedToUUID) {
            return new User(username, null, linkedToUUID, passwordHash, Collections.emptyList());
        }
    }
}
