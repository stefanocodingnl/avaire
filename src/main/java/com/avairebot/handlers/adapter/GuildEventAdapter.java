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

package com.avairebot.handlers.adapter;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.changelog.ChangelogHandler;
import com.avairebot.changelog.ChangelogMessage;
import com.avairebot.contracts.handlers.EventAdapter;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.query.QueryBuilder;
import com.avairebot.factories.MessageFactory;
import com.avairebot.utilities.CheckPermissionUtil;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogOption;
import net.dv8tion.jda.api.audit.TargetType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuildEventAdapter extends EventAdapter {

    public GuildEventAdapter(AvaIre avaire) {
        super(avaire);
    }


    public void onGuildPIAMemberBanEvent(GuildUnbanEvent e) {
        try {
            QueryBuilder qb = avaire.getDatabase().newQueryBuilder(Constants.ANTI_UNBAN_TABLE_NAME).where("userId", e.getUser().getId());
            Collection unbanCollection = qb.get();
            if (unbanCollection.size() > 0) {
                List <AuditLogEntry> logs = e.getGuild().retrieveAuditLogs().stream().filter(d -> d.getType().equals(ActionType.UNBAN) && d.getTargetType().equals(TargetType.MEMBER) && d.getTargetId().equals(e.getUser().getId())).collect(Collectors.toList());
                MessageChannel tc = avaire.getShardManager().getTextChannelById("788316320747094046");

                if (logs.size() < 1) {
                    if (tc != null) {
                        tc.sendMessage(MessageFactory.makeEmbeddedMessage(tc).setDescription(e.getUser().getAsTag() + " has been unbanned from ``" + e.getGuild() + "``, however, I could not find the user responsible for the unban. Please check the audit logs in the responsible server for more information. (User has been re-banned)").buildEmbed()).queue();
                    }
                    e.getGuild().ban(e.getUser().getId(), 0, "User was unbanned, user has been re-banned due to permban system in Xeus. Original ban reason (Do not unban without PIA permission): " + unbanCollection.get(0).getString("reason")).reason("PIA BAN: " + unbanCollection.get(0).getString("reason")).queue();
                } else {
                    if (tc != null) {
                        if (Constants.bypass_users.contains(logs.get(0).getUser().getId())) {
                            tc.sendMessage(MessageFactory.makeEmbeddedMessage(tc).setDescription("``" + e.getUser().getName() + e.getUser().getDiscriminator() + "``" + " has been unbanned from ``" + e.getGuild().getName() + "``\nIssued by PIA Member: " + logs.get(0).getUser().getName() + "#" + logs.get(0).getUser().getDiscriminator() + "\nWith reason: " + (logs.get(0).getReason() != null ? logs.get(0).getReason() : "No reason given")).buildEmbed()).queue();
                            logs.get(0).getUser().openPrivateChannel().queue(o -> {
                                if (o.getUser().isBot()) return;
                                o.sendMessage(MessageFactory.makeEmbeddedMessage(tc).setDescription("You have curr").buildEmbed()).queue();
                            });
                        } else {
                            tc.sendMessage(MessageFactory.makeEmbeddedMessage(tc).setDescription("``" + e.getUser().getName() + e.getUser().getDiscriminator() + "`` has been unbanned from ``" + e.getGuild().getName() + "``\nIssued by Guild Member: " + logs.get(0).getUser().getName() + "#" + logs.get(0).getUser().getDiscriminator() + " (User has been re-banned)").buildEmbed()).queue();
                            e.getGuild().ban(e.getUser().getId(), 0, "User was unbanned, user has been re-banned due to permban system in Xeus. Original ban reason (Do not unban without PIA permission): " + unbanCollection.get(0).getString("reason")).reason("PIA BAN: " + unbanCollection.get(0).getString("reason")).queue();
                            logs.get(0).getUser().openPrivateChannel().queue(o -> {
                                if (o.getUser().isBot()) return;
                                o.sendMessage(MessageFactory.makeEmbeddedMessage(tc).setDescription("Sorry, but this user ``:bannedUser`` was permbanned of PB though the Xeus blacklist feature and may **not** be unbanned. Please ask a PIA agent to handle an unban if deemed necessary.").buildEmbed()).queue();
                            });
                        }
                    }
                }
            } else {
                return;
            }


        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}
