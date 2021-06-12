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

package com.avairebot.commands.administration;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.utilities.CheckPermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

public class LockChannelsCommand extends Command {

    private static final Logger log = LoggerFactory.getLogger(LockChannelsCommand.class);

    public LockChannelsCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Lock Channel(s) Command";
    }

    @Override
    public String getDescription() {
        return "Lock one, or all channels.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Arrays.asList(
            "`:command (all)` - Lock the channel you executed the command in."
        );
    }

    @Override
    public List <String> getExampleUsage() {
        return Arrays.asList(
            "`:command #spam off` - Disables lock in the #spam channel.",
            "`:command #sandbox` - Toggles the lock on/off for the #sandbox channel.",
            "`:command` - Lists all the channels that currently have their lock-ability enabled."
        );
    }

    @Override
    public List <Class <? extends Command>> getRelations() {
        return Arrays.asList(
            ModifyRoleChannelLockCommand.class,
            ModifyLockChannelCommand.class
        );
    }

    @Override
    public List <String> getTriggers() {
        return Arrays.asList("lock", "lc");
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "isModOrHigher",
            "throttle:user,1,5"
        );
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.CHANNEL_LOCK);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public boolean onCommand(CommandMessage context, String[] args) {
        List <Role> r = new ArrayList <>();
        context.getGuildTransformer().getLockableChannelRoles().forEach(v -> {
            Role role = context.getGuild().getRoleById(v);
            if (role != null) {
                r.add(role);
            }
        });

        if (args.length > 0 && context.getMentionedChannels().size() < 1) {
            if (args[0].equals("all")) {
                if (!(CheckPermissionUtil.getPermissionLevel(context).getLevel() >= CheckPermissionUtil.GuildPermissionCheckType.ADMIN.getLevel())) {
                    context.makeError("Sorry, but you have to be a PIA member, or a Admin+ to use this command!").queue();
                    return false;
                }
                List <TextChannel> c = new ArrayList <>();
                context.getGuildTransformer().getLockableChannels().forEach(v -> {
                    TextChannel channel = context.getGuild().getTextChannelById(v);
                    if (channel != null) {
                        c.add(channel);
                    }
                });
                return handleGlobalChannelLock(context, r, c);
            }
        }
        return handleChannelLock(context, r);
    }

    private boolean handleGlobalChannelLock(CommandMessage context, List <Role> r, List <TextChannel> c) {
        StringBuilder sb = new StringBuilder();
        for (TextChannel tc : c) {
            changePermissions(r, sb, tc);
        }
        context.makeSuccess("Succesfully modified the current channels!\n" + sb.toString()).queue();
        return true;
    }

    private boolean handleChannelLock(CommandMessage context, List <Role> r) {
        StringBuilder sb = new StringBuilder();
        TextChannel tc = context.getMentionedChannels().size() == 1 ? context.getMentionedChannels().get(0) : context.channel;
        changePermissions(r, sb, tc);
        context.makeSuccess("Succesfully modified the current channel!\n" + sb.toString()).queue();
        return true;
    }

    EnumSet <Permission> allow_see = EnumSet.of(Permission.MESSAGE_READ);
    EnumSet <Permission> deny_write = EnumSet.of(Permission.MESSAGE_WRITE);

    private void changePermissions(List <Role> r, StringBuilder sb, TextChannel tc) {
        for (Role role : r) {
            PermissionOverride permissionOverride = tc.getPermissionOverride(role);
            if (permissionOverride != null) {
                if (permissionOverride.getRole().hasPermission(tc, Permission.MESSAGE_WRITE)) {
                    permissionOverride.getManager().setPermissions(allow_see, deny_write).queue();
                    sb.append(":x: ").append(tc.getAsMention()).append(": ").append(role.getAsMention()).append("\n");
                } else {
                    permissionOverride.getManager().clear(Permission.MESSAGE_WRITE).setAllow(Permission.MESSAGE_READ).queue();
                    sb.append(":white_check_mark: ").append(tc.getAsMention()).append(": ").append(role.getAsMention()).append("\n");
                }
            }
        }
    }

}

