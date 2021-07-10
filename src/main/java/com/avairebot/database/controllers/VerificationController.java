/*
 * Copyright (c) 2018.
 *
 * This file is part of AvaIre.
 *
 * AvaIre is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AvaIre is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AvaIre.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package com.avairebot.database.controllers;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.database.transformers.VerificationTransformer;
import com.avairebot.utilities.CacheUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VerificationController {

    public static final Cache<Long, VerificationTransformer> cache = CacheBuilder.newBuilder()
            .recordStats()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    private static final Logger log = LoggerFactory.getLogger(VerificationController.class);

    private static final String[] requiredVerificationColumns = new String[]{
            "verification.id", "verification.name", "verification.nickname_group",
            "verification.nickname_format", "verification.welcome_message",
            "verification.join_dm", "verification.nickname_users",
            "verification.unverified_role", "verification.verified_role", "verification.announce_channel",
            "verification.ranks"
    };

    /**
     * Fetches the guild transformer from the cache, if it doesn't exist in the
     * cache it will be loaded into the cache and then returned afterwords.
     *
     * @param avaire  The avaire instance, used to talking to the database.
     * @param message The JDA message instance for the current message.
     * @return Possibly null, the guild transformer instance for the current guild, or null.
     */
    @CheckReturnValue
    public static VerificationTransformer fetchGuild(AvaIre avaire, Message message) {
        if (!message.getChannelType().isGuild()) {
            return null;
        }
        return fetchVerificationFromGuild(avaire, message.getGuild());
    }

    /**
     * Fetches the guild transformer from the cache, if it doesn't exist in the
     * cache it will be loaded into the cache and then returned afterwords.
     *
     * @param avaire The avaire instance, used to talking to the database.
     * @param guild  The JDA guild instance for the current guild.
     * @return Possibly null, the guild transformer instance for the current guild, or null.
     */
    @CheckReturnValue
    public static VerificationTransformer fetchVerificationFromGuild(AvaIre avaire, Guild guild) {
        return (VerificationTransformer) CacheUtil.getUncheckedUnwrapped(cache, guild.getIdLong(), () -> loadGuildFromDatabase(avaire, guild));
    }

    public static String buildRoleData(List<Role> roles) {
        List<Map<String, Object>> rolesMap = new ArrayList<>();
        for (Role role : roles) {
            if (role.isPublicRole()) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();

            item.put("id", role.getId());
            item.put("name", role.getName());
            item.put("position", role.getPosition());
            item.put("permissions", role.getPermissionsRaw());

            item.put("color", role.getColor() == null ? null
                    : Integer.toHexString(role.getColor().getRGB()).substring(2)
            );

            rolesMap.add(item);
        }
        return AvaIre.gson.toJson(rolesMap);
    }

    public static void forgetCache(long guildId) {
        cache.invalidate(guildId);
    }

    private static VerificationTransformer loadGuildFromDatabase(AvaIre avaire, Guild guild) {
        if (log.isDebugEnabled()) {
            log.debug("Verification cache for " + guild.getId() + " was refreshed");
        }
        try {
            VerificationTransformer transformer = new VerificationTransformer(guild, avaire.getDatabase()
                    .newQueryBuilder(Constants.VERIFICATION_TABLE_NAME)
                    .select(requiredVerificationColumns)
                    .where("verification.id", guild.getId())
                    .get().first());

            if (!transformer.hasData()) {
                guild.retrieveOwner().queue(
                        member -> updateVerificationEntry(avaire, guild),
                        throwable -> updateVerificationEntry(avaire, guild)
                );

                return new VerificationTransformer(guild);
            }

            return transformer;
        } catch (Exception ex) {
            log.error("Failed to fetch guild transformer from the database, error: {}", ex.getMessage(), ex);

            return null;
        }
    }

    private static void updateVerificationEntry(AvaIre avaire, Guild guild) {
        try {
            avaire.getDatabase().newQueryBuilder(Constants.VERIFICATION_TABLE_NAME)
                    .insert(statement -> {
                        statement
                                .set("id", guild.getId())
                                .set("name", guild.getName(), true);
                    });
        } catch (Exception ex) {
            AvaIre.getLogger().error(ex.getMessage(), ex);
        }
    }
}
