/*
 * Copyright (c) 2019.
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

package com.avairebot.commands.system;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.CommandPriority;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.contracts.commands.SystemCommand;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.utilities.ComparatorUtil;
import com.avairebot.utilities.MentionableUtil;
import com.avairebot.utilities.NumberUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RoleSettingsCommand extends SystemCommand {

    private static final Logger log = LoggerFactory.getLogger(RoleSettingsCommand.class);

    public RoleSettingsCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Role Management Command";
    }

    @Override
    public String getDescription() {
        return "This command is used to modify the manager, mod and admin roles in the official pinewood discords.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Arrays.asList(
            "`:command <role> [status]` - Toggles the locking feature on/off."
        );
    }

    @Override
    public List <String> getExampleUsage() {
        return Arrays.asList(
            "`:command` - Lists all the roles that currently has their XP status disabled."
        );
    }

    @Override
    public List <String> getTriggers() {
        return Arrays.asList("rmanage");
    }

    @Override
    public CommandPriority getCommandPriority() {
        return CommandPriority.SYSTEM;
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.MISCELLANEOUS);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        GuildTransformer guildTransformer = context.getGuildTransformer();
        if (args.length == 0 || NumberUtil.parseInt(args[0], -1) > 0) {
            return sendEnabledRoles(context, guildTransformer);
        }
        if (args[0].equals("setup-basic-roles")) {
            return handleFirstSetupRoles(context, guildTransformer);
        }

        Role role = MentionableUtil.getRole(context.getMessage(), new String[]{args[0]});
        if (role == null) {
            return sendErrorMessage(context, context.i18n("invalidRole", args[0]));
        }

        if (args.length > 1) {
            if (args[1].equals("mod") || args[1].equals("admin") || args[1].equals("manager")) {
                if (args.length > 2) {
                    return handleToggleRole(context, role, args[1], ComparatorUtil.getFuzzyType(args[2]));
                }
                return handleToggleRole(context, role, args[1], ComparatorUtil.getFuzzyType(args[1]));
            } else {
                context.makeError("Invalid role given to manage.").queue();
                return false;
            }

        }
        return handleToggleRole(context, role, "mod", ComparatorUtil.ComparatorType.UNKNOWN);
    }

    private boolean handleFirstSetupRoles(CommandMessage context, GuildTransformer transformer) {
        Set <Long> admins = transformer.getAdministratorRoles();
        Set <Long> mods = transformer.getModeratorRoles();
        Set <Long> managers = transformer.getManagerRoles();

        admins.clear();
        mods.clear();
        managers.clear();

        for (Role r : context.guild.getRoles()) {
            if (r.isManaged()) {
                continue;
            }
            if (r.hasPermission(Permission.ADMINISTRATOR)) {
                admins.add(r.getIdLong());
            }
            if (r.hasPermission(Permission.MANAGE_SERVER) && !r.hasPermission(Permission.ADMINISTRATOR)) {
                managers.add(r.getIdLong());
            }
            if (r.hasPermission(Permission.MESSAGE_MANAGE) && !r.hasPermission(Permission.ADMINISTRATOR)) {
                mods.add(r.getIdLong());
            }
        }
        try {
            avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
                .where("id", context.getGuild().getId())
                .update(statement -> {
                    statement.set("admin_roles", AvaIre.gson.toJson(
                        admins
                    ), true);
                    statement.set("manager_roles", AvaIre.gson.toJson(
                        managers
                    ), true);
                    statement.set("moderator_roles", AvaIre.gson.toJson(
                        mods
                    ), true);
                });
            StringBuilder sb = new StringBuilder();

            runAdminRolesCheck(context, admins.size() > 0, sb, admins);
            runManagerRolesCheck(context, managers.size() > 0, sb, managers);
            runModRolesCheck(context, mods.size() > 0, sb, mods);

            context.makeSuccess(sb.toString() + "\n\nHave been added to the database!").queue();
            return true;
        } catch (SQLException throwables) {
            context.makeError("Something went wrong when saving the roles to the database.").queue();
            return false;
        }

    }

    private void runModRolesCheck(CommandMessage context, boolean b, StringBuilder sb, Set <Long> mods) {
        if (b) {
            sb.append("\n\n**Moderator roles**:");
            for (Long s : mods) {
                Role r = context.getGuild().getRoleById(s);
                if (r != null) {
                    sb.append("\n - ").append(r.getAsMention());
                }
            }
        } else {
            sb.append("\n\n**Moderator roles**:\n" + "" + "No roles have been found!");
        }
    }

    private void runManagerRolesCheck(CommandMessage context, boolean b, StringBuilder sb, Set <Long> managers) {
        if (b) {
            sb.append("\n\n**Manager roles**:");
            for (Long s : managers) {
                Role r = context.getGuild().getRoleById(s);
                if (r != null) {
                    sb.append("\n - ").append(r.getAsMention());
                }
            }
        } else {
            sb.append("\n\n**Manager roles**:\n" + "" + "No roles have been found!");
        }
    }

    private void runAdminRolesCheck(CommandMessage context, boolean b, StringBuilder sb, Set <Long> admins) {
        if (b) {
            sb.append("\n\n**Admin roles**:");
            for (Long s : admins) {
                Role r = context.getGuild().getRoleById(s);
                if (r != null) {
                    sb.append("\n - ").append(r.getAsMention());
                }
            }
        } else {
            sb.append("\n\n**Admin roles**:\n" + "" + "No roles have been found!");
        }
    }

    private boolean sendEnabledRoles(CommandMessage context, GuildTransformer transformer) {
        if (transformer.getAdministratorRoles().isEmpty() && transformer.getModeratorRoles().isEmpty() && transformer.getManagerRoles().isEmpty()) {
            return sendErrorMessage(context, "Sorry, but there are no manager, admin or mod roles on the discord configured.");
        }

        Set <Long> mod = transformer.getModeratorRoles();
        Set <Long> manager = transformer.getManagerRoles();
        Set <Long> admins = transformer.getAdministratorRoles();
        StringBuilder sb = new StringBuilder();
        runAdminRolesCheck(context, admins.size() > 0, sb, admins);
        runManagerRolesCheck(context, manager.size() > 0, sb, manager);
        runModRolesCheck(context, mod.size() > 0, sb, mod);

        context.makeInfo(context.i18n("listRoles"))
            .set("roles", sb.toString())
            .setTitle(context.i18n("listRolesTitle",
                transformer.getModeratorRoles().size() + transformer.getManagerRoles().size() + transformer.getAdministratorRoles().size()
            ))
            .queue();


        return true;
    }


    @SuppressWarnings("ConstantConditions")
    private boolean handleToggleRole(CommandMessage context, Role role, String rank, ComparatorUtil.ComparatorType value) {
        GuildTransformer guildTransformer = context.getGuildTransformer();

        switch (value) {
            case FALSE:
                if (rank.equals("admin")) {
                    guildTransformer.getAdministratorRoles().remove(role.getIdLong());
                }
                if (rank.equals("manager")) {
                    guildTransformer.getManagerRoles().remove(role.getIdLong());
                }
                if (rank.equals("mod")) {
                    guildTransformer.getModeratorRoles().remove(role.getIdLong());
                }
                break;

            case TRUE:
                if (rank.equals("admin")) {
                    guildTransformer.getAdministratorRoles().add(role.getIdLong());
                }
                if (rank.equals("manager")) {
                    guildTransformer.getManagerRoles().add(role.getIdLong());
                }
                if (rank.equals("mod")) {
                    guildTransformer.getModeratorRoles().add(role.getIdLong());
                }

                break;

            case UNKNOWN:
                if (rank.equals("admin")) {
                    if (guildTransformer.getAdministratorRoles().contains(role.getIdLong())) {
                        guildTransformer.getAdministratorRoles().remove(role.getIdLong());
                    } else {
                        guildTransformer.getAdministratorRoles().add(role.getIdLong());
                    }
                    break;
                }

                if (rank.equals("manager")) {
                    if (guildTransformer.getManagerRoles().contains(role.getIdLong())) {
                        guildTransformer.getManagerRoles().remove(role.getIdLong());
                    } else {
                        guildTransformer.getManagerRoles().add(role.getIdLong());
                    }
                    break;
                }
                if (rank.equals("mod")) {
                    if (guildTransformer.getModeratorRoles().contains(role.getIdLong())) {
                        guildTransformer.getModeratorRoles().remove(role.getIdLong());
                    } else {
                        guildTransformer.getModeratorRoles().add(role.getIdLong());
                    }
                    break;
                }
        }

        boolean isEnabled = guildTransformer.getModeratorRoles().contains(role.getIdLong()) ||
            guildTransformer.getAdministratorRoles().contains(role.getIdLong()) ||
            guildTransformer.getManagerRoles().contains(role.getIdLong());

        try {
            if (rank.equals("admin")) {
                avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
                    .where("id", context.getGuild().getId())
                    .update(statement -> {
                        statement.set("admin_roles", AvaIre.gson.toJson(
                            guildTransformer.getAdministratorRoles()
                        ), true);
                    });
            }
            if (rank.equals("manager")) {
                avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
                    .where("id", context.getGuild().getId())
                    .update(statement -> {
                        statement.set("manager_roles", AvaIre.gson.toJson(
                            guildTransformer.getManagerRoles()
                        ), true);
                    });
            }
            if (rank.equals("mod")) {
                avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
                    .where("id", context.getGuild().getId())
                    .update(statement -> {
                        statement.set("moderator_roles", AvaIre.gson.toJson(
                            guildTransformer.getModeratorRoles()
                        ), true);
                    });
            }

            context.makeSuccess(context.i18n("success"))
                .set("role", role.getAsMention())
                .set("status", context.i18n(isEnabled ? "status.enabled" : "status.disabled"))
                .set("rank", rank)
                .queue();

            return true;
        } catch (SQLException e) {
            log.error("Failed to save the level exempt roles to the database for guild {}, error: {}",
                context.getGuild().getId(), e.getMessage(), e
            );

            context.makeError("Failed to save the changes to the database, please try again. If the issue persists, please contact one of my developers.").queue();

            return false;
        }
    }
}
