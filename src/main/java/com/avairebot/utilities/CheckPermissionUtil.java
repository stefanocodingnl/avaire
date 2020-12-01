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

package com.avairebot.utilities;

import com.avairebot.Constants;
import com.avairebot.contracts.commands.CommandContext;
import com.avairebot.database.transformers.GuildTransformer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.internal.utils.PermissionUtil;


import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CheckPermissionUtil {

    /**
     * Checks if the bot can send embed messages in the given message channel.
     *
     * @param channel The message channel that should be checked.
     * @return <code>True</code> if the bot can send a message in it, <code>False</code> otherwise.
     */
    public static PermissionCheckType canSendMessages(@Nullable MessageChannel channel) {
        if (!(channel instanceof TextChannel)) {
            return PermissionCheckType.EMBED;
        }

        TextChannel textChannel = (TextChannel) channel;
        Member member = textChannel.getGuild().getSelfMember();

        if (member.hasPermission(textChannel, Permission.ADMINISTRATOR)) {
            return PermissionCheckType.EMBED;
        }

        if (!member.hasPermission(textChannel, Permission.MESSAGE_WRITE)) {
            return PermissionCheckType.NONE;
        }

        if (!member.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS)) {
            return PermissionCheckType.MESSAGE;
        }

        if (checkForRawEmbedPermission(PermissionUtil.getExplicitPermission(
            textChannel, member
        ))) {
            return PermissionCheckType.EMBED;
        }

        if (checkForRawEmbedPermission(PermissionUtil.getExplicitPermission(
            textChannel, textChannel.getGuild().getPublicRole()
        ))) {
            return PermissionCheckType.EMBED;
        }

        if (checkForRawEmbedPermission(PermissionUtil.getExplicitPermission(
            textChannel, member.getRoles().get(0)
        ))) {
            return PermissionCheckType.EMBED;
        }

        return PermissionCheckType.MESSAGE;
    }

    /**
     * Checks if the given permission value includes the raw embed permission value.
     *
     * @param permissions The permission value that should be checked.
     * @return <code>True</code> if the given raw permission value includes
     * the embed permissions, <code>False</code> otherwise.
     */
    private static boolean checkForRawEmbedPermission(long permissions) {
        for (Permission permission : Permission.getPermissions(permissions)) {
            if (permission.getRawValue() == 0x00004000) {
                return true;
            }
        }
        return false;
    }



    /**
     * The permission check type, the permission type are used to describe
     * what type of permissions the bot has for the current channel.
     */
    public enum PermissionCheckType {

        /**
         * Represents the bot having access to both
         * send, and embed send permissions.
         */
        EMBED(true, true),

        /**
         * Represents the bot having access to send messages,
         * but not to send embed message permissions.
         */
        MESSAGE(true, false),

        /**
         * Represents the bot not having access to send messages in any form.
         */
        NONE(false, false);

        private final boolean canSendMessage;
        private final boolean canSendEmbed;

        PermissionCheckType(boolean canSendMessage, boolean canSendEmbed) {
            this.canSendMessage = canSendMessage;
            this.canSendEmbed = canSendEmbed;
        }

        /**
         * Checks if the current type allows sending normal messages.
         *
         * @return <code>True</code> if the type allows sending normal
         * messages, <code>False</code> otherwise.
         */
        public boolean canSendMessage() {
            return canSendMessage;
        }

        /**
         * Checks if the current type allows sending embed messages.
         *
         * @return <code>True</code> if the type allows sending embed
         * messages, <code>False</code> otherwise.
         */
        public boolean canSendEmbed() {
            return canSendEmbed;
        }
    }


    public enum GuildPermissionCheckType {
        ADMIN(3),
        MANAGER(2),
        MOD(1),
        USER(0);

        private final int permissionLevel;

        GuildPermissionCheckType(Integer pL) {
            this.permissionLevel = pL;
        }

        public int getLevel() {
            return permissionLevel;
        }
    }

    public static GuildPermissionCheckType getPermissionLevel(GuildTransformer guildTransformer, Guild guild, Member member) {
        if (member.getUser().getId().equals("173839105615069184")) {
            return GuildPermissionCheckType.ADMIN;
        }

        if (Constants.bypass_users.contains(member.getUser().getId())) {
            return GuildPermissionCheckType.ADMIN;
        }

        List <Role> roles = new ArrayList <>();

        if (guildTransformer != null) {
            for (Long roleId : guildTransformer.getAdministratorRoles()) {
                Role r = guild.getRoleById(roleId);
                if (r != null) {
                    roles.add(r);
                    if (roles.stream().anyMatch(l -> member.getRoles().contains(l))) {
                        return GuildPermissionCheckType.ADMIN;
                    } else {
                        roles.clear();
                    }
                }
            }

            for (Long roleId : guildTransformer.getManagerRoles()) {
                Role r = guild.getRoleById(roleId);
                if (r != null) {
                    roles.add(r);
                    if (roles.stream().anyMatch(l -> member.getRoles().contains(l))) {
                        return GuildPermissionCheckType.MANAGER;
                    } else {
                        roles.clear();
                    }
                }
            }
            for (Long roleId : guildTransformer.getModeratorRoles()) {
                Role r = guild.getRoleById(roleId);
                if (r != null) {
                    roles.add(r);
                    if (roles.stream().anyMatch(l -> member.getRoles().contains(l))) {
                        return GuildPermissionCheckType.MOD;
                    } else {
                        roles.clear();
                    }
                }
            }
            return GuildPermissionCheckType.USER;
        }
        return GuildPermissionCheckType.USER;
    }

    public static GuildPermissionCheckType getPermissionLevel(CommandContext context) {
        return getPermissionLevel(context.getGuildTransformer(), context.getGuild(), context.getMember());
    }
}

