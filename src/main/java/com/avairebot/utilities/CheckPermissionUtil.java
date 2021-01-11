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

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.audio.AudioHandler;
import com.avairebot.audio.DJGuildLevel;
import com.avairebot.contracts.commands.CommandContext;
import com.avairebot.database.controllers.GuildController;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.permissions.Permissions;
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
        FACILITATOR(6, "Facilitator / Bot Admin / Global Admin"),
        PIA(5, "PIA / Global Moderator"),
        ADMIN(4, "Admin / Division Leader / Local Admin"),
        MANAGER(3, "Manager / Local Managment Mod"),
        MOD(2, "Mod / HR / Local Mod"),
        DJ(1, "DJ / Regular User With Music"),
        USER(0, "Regular User");

        private final int permissionLevel;
        private final String rankName;

        GuildPermissionCheckType(Integer pL, String rankName) {
            this.permissionLevel = pL;
            this.rankName = rankName;
        }

        public int getLevel() {
            return permissionLevel;
        }

        public String getRankName() {
            return rankName;
        }
    }

    public static GuildPermissionCheckType getPermissionLevel(GuildTransformer guildTransformer, Guild guild, Member member) {
        if (AvaIre.getInstance().getBotAdmins().getUserById(member.getUser().getIdLong(), true).isGlobalAdmin()) {
            return GuildPermissionCheckType.FACILITATOR;
        }

        if (Constants.bypass_users.contains(member.getUser().getId())) {
            return GuildPermissionCheckType.PIA;
        }

        if (guild == null) {
            return GuildPermissionCheckType.USER;
        }

        if (guildTransformer != null) {
            for (Long roleId : guildTransformer.getAdministratorRoles()) {
                Role r = guild.getRoleById(roleId);
                if (r != null) {
                    if (member.getRoles().contains(r)) {
                        return GuildPermissionCheckType.ADMIN;
                    }
                }
            }

            for (Long roleId : guildTransformer.getManagerRoles()) {
                Role r = guild.getRoleById(roleId);
                if (r != null) {
                    if (member.getRoles().contains(r)) {
                        return GuildPermissionCheckType.MANAGER;
                    }
                }
            }
            for (Long roleId : guildTransformer.getModeratorRoles()) {
                Role r = guild.getRoleById(roleId);
                if (r != null) {
                    if (member.getRoles().contains(r)) {
                        return GuildPermissionCheckType.MOD;
                    }
                }
            }


            DJGuildLevel guildLevel = guildTransformer.getDJLevel();
            if (guildLevel != null) {
                switch (guildLevel) {
                    case NORMAL:
                        return hasRole(guildTransformer, guild, member);

                    case ALL:
                        return GuildPermissionCheckType.DJ;

                    default:
                        return GuildPermissionCheckType.USER;
                }
            }

            return GuildPermissionCheckType.USER;
        }
        return GuildPermissionCheckType.USER;
    }

    private static GuildPermissionCheckType hasRole(GuildTransformer guildTransformer, Guild guild, Member member) {
        if (guildTransformer.getDjRole() != null) {
            Role role = guild.getRoleById(guildTransformer.getDjRole());
            if (role != null) {
                if (RoleUtil.hasRole(member, role)) {
                    return GuildPermissionCheckType.DJ;
                }
            }
        }

        for (Role role : member.getRoles()) {
            if (role.getName().equalsIgnoreCase("DJ")) {
                return GuildPermissionCheckType.DJ;
            }
        }
        return GuildPermissionCheckType.USER;
    }

    public static GuildPermissionCheckType getPermissionLevel(CommandContext context) {
        return getPermissionLevel(context.getGuildTransformer(), context.getGuild(), context.getMember());
    }
}

