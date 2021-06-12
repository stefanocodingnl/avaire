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
import com.avairebot.cache.MessageCache;
import com.avairebot.contracts.cache.CachedMessage;
import com.avairebot.contracts.handlers.EventAdapter;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.controllers.GuildController;
import com.avairebot.database.query.QueryBuilder;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.factories.MessageFactory;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.TargetType;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.update.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;

import java.awt.*;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuildEventAdapter extends EventAdapter {

    public GuildEventAdapter(AvaIre avaire) {
        super(avaire);
    }

    public void onGuildPIAMemberBanEvent(GuildUnbanEvent e) {
        try {
            QueryBuilder qb = avaire.getDatabase().newQueryBuilder(Constants.ANTI_UNBAN_TABLE_NAME).where("userId", e.getUser().getId());
            Collection unbanCollection = qb.get();
            if (unbanCollection.size() > 0) {
                e.getGuild().retrieveAuditLogs().queue(items -> {
                    List<AuditLogEntry> logs = items.stream().filter(d -> d.getType().equals(ActionType.UNBAN) && d.getTargetType().equals(TargetType.MEMBER) && d.getTargetId().equals(e.getUser().getId())).collect(Collectors.toList());
                    MessageChannel tc = avaire.getShardManager().getTextChannelById(Constants.PIA_LOG_CHANNEL);

                    if (logs.size() < 1) {
                        if (tc != null) {
                            tc.sendMessage(MessageFactory.makeEmbeddedMessage(tc).setDescription(e.getUser().getAsTag() + " has been unbanned from **" + e.getGuild() + "**, however, I could not find the user responsible for the unban. Please check the audit logs in the responsible server for more information. (User has been re-banned)").buildEmbed()).queue();
                        }
                        e.getGuild().ban(e.getUser().getId(), 0, "User was unbanned, user has been re-banned due to permban system in Xeus. Original ban reason (Do not unban without PIA permission): " + unbanCollection.get(0).getString("reason")).reason("PIA BAN: " + unbanCollection.get(0).getString("reason")).queue();
                    } else {
                        if (tc != null) {
                            if (Constants.piaMembers.contains(logs.get(0).getUser().getId())) {
                                tc.sendMessage(MessageFactory.makeEmbeddedMessage(tc).setDescription("**" + e.getUser().getName() + e.getUser().getDiscriminator() + "**" + " has been unbanned from **" + e.getGuild().getName() + "**\nIssued by PIA Member: " + logs.get(0).getUser().getName() + "#" + logs.get(0).getUser().getDiscriminator() + "\nWith reason: " + (logs.get(0).getReason() != null ? logs.get(0).getReason() : "No reason given")).buildEmbed()).queue();
                                logs.get(0).getUser().openPrivateChannel().queue(o -> {
                                    if (o.getUser().isBot()) return;
                                    o.sendMessage(MessageFactory.makeEmbeddedMessage(tc).setDescription("You have currently unbanned a Global-Banned user.").buildEmbed()).queue();
                                });
                            } else {
                                tc.sendMessage(MessageFactory.makeEmbeddedMessage(tc).setDescription("**" + e.getUser().getName() + e.getUser().getDiscriminator() + "** has been unbanned from **" + e.getGuild().getName() + "**\nIssued by Guild Member: " + logs.get(0).getUser().getName() + "#" + logs.get(0).getUser().getDiscriminator() + " (User has been re-banned)").buildEmbed()).queue();

                                logs.get(0).getUser().openPrivateChannel().queue(o -> {
                                    if (o.getUser().isBot()) return;
                                    o.sendMessage(MessageFactory.makeEmbeddedMessage(tc).setDescription("Sorry, but this user **:bannedUser** was permbanned of PB though the Xeus blacklist feature and may **not** be unbanned. Please ask a PIA agent to handle an unban if deemed necessary.").set("bannedUser", e.getUser().getAsTag() + " / " + e.getUser().getName()).buildEmbed()).queue();
                                });

                                String agent = avaire.getShardManager().getUserById(unbanCollection.get(0).getLong("punishedId")) != null ? avaire.getShardManager().getUserById(unbanCollection.get(0).getLong("punishedId")).getName() : "No PIA Member found";
                                e.getGuild().ban(e.getUser().getId(), 0, "Banned by: " + agent + "\n" +
                                    "For: " + unbanCollection.get(0).getString("reason") + "\n*THIS IS A PIA GLOBAL BAN, DO NOT REVOKE THIS BAN WITHOUT CONSULTING THE PIA MEMBER WHO INITIATED THE GLOBAL BAN, REVOKING THIS BAN WITHOUT PIA APPROVAL WILL RESULT IN DISCIPlINARY ACTION!*").reason("Global Ban, executed by " + agent + ". For: \n" + unbanCollection.get(0).getString("reason")).queue();
                            }
                        }
                    }
                });

            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void onGenericGuildEvent(GenericGuildEvent event) {
        GuildTransformer transformer = GuildController.fetchGuild(avaire, event.getGuild());
        if (transformer.getAuditLogChannel() != 0) {
            TextChannel tc = event.getGuild().getTextChannelById(transformer.getAuditLogChannel());
            if (tc != null) {
                if (event instanceof GuildBanEvent) {
                    GuildBanEvent e = (GuildBanEvent) event;
                    MessageFactory.makeEmbeddedMessage(tc, new Color(255, 50, 0))
                        .setAuthor("User banned"
                            , null, e.getUser().getEffectiveAvatarUrl())
                        .setDescription(e.getUser().getName() + "#" + e.getUser().getDiscriminator() + "**(:banned)**")
                        .set("banned", e.getUser().getAsMention())
                        .setTimestamp(Instant.now()).queue();
                } else if (event instanceof GuildUnbanEvent) {
                    GuildUnbanEvent e = (GuildUnbanEvent) event;
                    MessageFactory.makeEmbeddedMessage(tc, new Color(255, 233, 44))
                        .setAuthor("User unbanned"
                            , null, e.getUser().getEffectiveAvatarUrl())
                        .setDescription(e.getUser().getName() + "#" + e.getUser().getDiscriminator() + "**(:unbanned)**")
                        .set("unbanned", e.getUser().getAsMention())
                        .setTimestamp(Instant.now()).queue();
                } else if (event instanceof GuildUpdateAfkChannelEvent) {
                    GuildChannel oldChannel = getModifiedChannel(event, false);
                    GuildChannel newChannel = getModifiedChannel(event, true);

                    MessageFactory.makeEmbeddedMessage(tc, new Color(255, 0, 15))
                        .setAuthor("AFK Channel was modified"
                            , null, event.getGuild().getIconUrl())
                        .addField("**Old Channel**:", oldChannel.getName(), true)
                        .addField("**New channel**:", newChannel.getName(), true)
                        .setTimestamp(Instant.now()).queue();
                } else if (event instanceof GuildUpdateSystemChannelEvent) {
                    GuildChannel oldChannel = getModifiedChannel(event, false);
                    GuildChannel newChannel = getModifiedChannel(event, true);

                    MessageFactory.makeEmbeddedMessage(tc, new Color(120, 120, 120))
                        .setAuthor("System Channel was modified"
                            , null, event.getGuild().getIconUrl())
                        .addField("**Old Channel**:", oldChannel.getName(), true)
                        .addField("**New channel**:", newChannel.getName(), true)
                        .setTimestamp(Instant.now()).queue();
                } else if (event instanceof GuildUpdateRegionEvent) {
                    GuildUpdateRegionEvent e = (GuildUpdateRegionEvent) event;
                    MessageFactory.makeEmbeddedMessage(tc, new Color(255, 71, 15))
                        .setAuthor("Region was updated"
                            , null, event.getGuild().getIconUrl())
                        .addField("**Old region**:", e.getOldRegion().getName() + " - " + e.getOldRegion().getEmoji(), true)
                        .addField("**New region**:", e.getOldRegion().getName() + " - " + e.getNewRegion().getEmoji(), true)
                        .setTimestamp(Instant.now()).queue();
                } else if (event instanceof GuildUpdateBoostCountEvent) {
                    GuildUpdateBoostCountEvent e = (GuildUpdateBoostCountEvent) event;
                    MessageFactory.makeEmbeddedMessage(tc, new Color(255, 0, 255))
                        .setAuthor("Boost count was updated"
                            , null, event.getGuild().getIconUrl())
                        .addField("**Old Boost count**:", String.valueOf(e.getOldBoostCount()), true)
                        .addField("**New Boost count**:", String.valueOf(e.getNewBoostCount()), true)
                        .setTimestamp(Instant.now()).queue();
                } else if (event instanceof GuildUpdateBoostTierEvent) {
                    GuildUpdateBoostTierEvent e = (GuildUpdateBoostTierEvent) event;
                    MessageFactory.makeEmbeddedMessage(tc, new Color(255, 0, 255))
                        .setAuthor("Boost **tier** was updated"
                            , null, event.getGuild().getIconUrl())
                        .addField("Old Boost **Tier**:", String.valueOf(e.getOldBoostTier()), true)
                        .addField("New Boost **Tier**:", String.valueOf(e.getNewBoostTier()), true)
                        .setTimestamp(Instant.now()).queue();
                } else if (event instanceof GuildMemberJoinEvent) {
                    GuildMemberJoinEvent e = (GuildMemberJoinEvent) event;

                    if (checkAccountAge(e)) {
                        if (transformer.getMemberToYoungChannelId() != null && event.getGuild().getTextChannelById(transformer.getMemberToYoungChannelId()) != null) {
                            MessageFactory.makeEmbeddedMessage(event.getGuild().getTextChannelById(transformer.getMemberToYoungChannelId()))
                                .setThumbnail(e.getUser().getEffectiveAvatarUrl())
                                .setDescription("User found with an *VERY* new account!!!\n\n" + e.getUser().getName() + "#" + e.getUser().getDiscriminator() + "\n" +
                                    "**Created on**: " + e.getUser().getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n**User ID**: ``" + e.getUser().getId() + "``"
                                ).queue();
                        }

                    }
                } else if (event instanceof GuildMemberRoleAddEvent) {
                    GuildMemberRoleAddEvent e = (GuildMemberRoleAddEvent) event;
                    StringBuilder sb = new StringBuilder();
                    for (Role role : e.getRoles()) {
                        sb.append("\n - **").append(role.getName()).append("**");
                    }
                    MessageFactory.makeEmbeddedMessage(tc, new Color(255, 129, 31))
                        .setAuthor("Roles were added to member!"
                            , null, e.getUser().getEffectiveAvatarUrl())
                        .setDescription("**Member**: " + e.getUser().getAsMention() + "\n" +
                            "**User**: " + e.getUser().getName() + "#" + e.getUser().getDiscriminator() + "\n" +
                            "\n**Roles given**: " + sb.toString())
                        .setFooter("UserID: " + e.getUser().getId())
                        .setTimestamp(Instant.now())
                        .queue();
                } else if (event instanceof GuildMemberRoleRemoveEvent) {
                    GuildMemberRoleRemoveEvent e = (GuildMemberRoleRemoveEvent) event;
                    StringBuilder sb = new StringBuilder();
                    for (Role role : e.getRoles()) {
                        sb.append("\n - **").append(role.getName()).append("**");
                    }
                    MessageFactory.makeEmbeddedMessage(tc, new Color(92, 135, 186))
                        .setAuthor("Roles where removed from member!"
                            , null, e.getUser().getEffectiveAvatarUrl())
                        .setDescription("**Member**: " + e.getUser().getAsMention() + "\n" +
                            "**User**: " + e.getUser().getName() + "#" + e.getUser().getDiscriminator() + "\n" +
                            "\n**Roles removed**: " + sb.toString())
                        .setFooter("UserID: " + e.getUser().getId())
                        .setTimestamp(Instant.now())
                        .queue();
                } else if (event instanceof GuildMemberUpdateNicknameEvent) {
                    GuildMemberUpdateNicknameEvent e = (GuildMemberUpdateNicknameEvent) event;
                    MessageFactory.makeEmbeddedMessage(tc, new Color(255, 195, 0))
                        .setAuthor("User nick was changed!"
                            , null, e.getUser().getEffectiveAvatarUrl())
                        .setDescription("**Member**: " + e.getUser().getAsMention() + "\n" +
                            "**User**: " + e.getUser().getName() + "#" + e.getUser().getDiscriminator() + "\n" +
                            "**Old name**: ``" + e.getOldNickname() + "``\n" +
                            "**New name**: ``" + e.getNewNickname() + "``")
                        .setFooter("UserID: " + e.getUser().getId())
                        .setTimestamp(Instant.now())
                        .queue();
                } else if (event instanceof GuildMessageDeleteEvent) {
                    GuildMessageDeleteEvent e = (GuildMessageDeleteEvent) event;
                    messageDeleteEvent(e, tc);
                } else if (event instanceof GuildMessageUpdateEvent) {
                    GuildMessageUpdateEvent e = (GuildMessageUpdateEvent) event;
                    messageUpdateEvent(e, tc);
                } else if (event instanceof GuildVoiceJoinEvent) {
                    GuildVoiceJoinEvent e = (GuildVoiceJoinEvent) event;

                    MessageFactory.makeEmbeddedMessage(tc, new Color(28, 255, 0))
                        .setAuthor(e.getMember().getEffectiveName() + " joined a voice channel!"
                            , null, e.getMember().getUser().getEffectiveAvatarUrl())
                        .setDescription("**Member**: " + e.getMember().getUser().getAsMention() + "\n" +
                            "**User**: " + e.getMember().getUser().getName() + "#" + e.getMember().getUser().getDiscriminator() + "\n" +
                            "**Joined channel**: \uD83D\uDD08 " + e.getChannelJoined().getName())
                        .setFooter("UserID: " + e.getMember().getUser().getId())
                        .setTimestamp(Instant.now())
                        .queue();
                } else if (event instanceof GuildVoiceLeaveEvent) {
                    GuildVoiceLeaveEvent e = (GuildVoiceLeaveEvent) event;

                    MessageFactory.makeEmbeddedMessage(tc, new Color(255, 11, 0))
                        .setAuthor(e.getMember().getEffectiveName() + " left a voice channel!"
                            , null, e.getMember().getUser().getEffectiveAvatarUrl())
                        .setDescription("**Member**: " + e.getMember().getUser().getAsMention() + "\n" +
                            "**User**: " + e.getMember().getUser().getName() + "#" + e.getMember().getUser().getDiscriminator() + "\n" +
                            "**Left channel**: \uD83D\uDD07 " + e.getChannelLeft().getName())
                        .setFooter("UserID: " + e.getMember().getUser().getId())
                        .setTimestamp(Instant.now())
                        .queue();
                } else if (event instanceof GuildVoiceMoveEvent) {
                    GuildVoiceMoveEvent e = (GuildVoiceMoveEvent) event;
                    MessageFactory.makeEmbeddedMessage(tc, new Color(156, 0, 255))
                        .setAuthor(e.getMember().getEffectiveName() + " moved voice channels!"
                            , null, e.getMember().getUser().getEffectiveAvatarUrl())
                        .setDescription("**Member**: " + e.getMember().getUser().getAsMention() + "\n" +
                            "**User**: " + e.getMember().getUser().getName() + "#" + e.getMember().getUser().getDiscriminator() + "\n" +
                            "**Joined channel**: \uD83D\uDD08 " + e.getChannelJoined().getName() + "\n" +
                            "**Left channel**: \uD83D\uDD07 " + e.getChannelLeft().getName())
                        .setFooter("UserID: " + e.getMember().getUser().getId())
                        .setTimestamp(Instant.now())
                        .queue();
                }
            }

        }
    }

    public void onJoinLogsEvent(GenericGuildEvent event) {
        GuildTransformer transformer = GuildController.fetchGuild(avaire, event.getGuild());
        if (transformer == null) return;

        if (transformer.getJoinLogsChannel() != 0) {
            TextChannel tc = avaire.getShardManager().getTextChannelById(transformer.getJoinLogsChannel());
            if (event instanceof GuildMemberJoinEvent) {
                GuildMemberJoinEvent e = (GuildMemberJoinEvent) event;
                MessageFactory.makeEmbeddedMessage(tc, new Color(77, 224, 102))
                    .setAuthor("Member joined the server!"
                        , null, e.getUser().getEffectiveAvatarUrl())
                    .setDescription("**Member**: " + e.getUser().getAsMention() + "\n" +
                        "**User**: " + e.getUser().getName() + "#" + e.getUser().getDiscriminator() + "\n" +
                        "**Account Age**: " + e.getUser().getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                    .setFooter("UserID: " + e.getUser().getId())
                    .setTimestamp(Instant.now())
                    .queue();
            } else if (event instanceof GuildMemberRemoveEvent) {
                GuildMemberRemoveEvent e = (GuildMemberRemoveEvent) event;

                MessageFactory.makeEmbeddedMessage(tc, new Color(255, 67, 65))
                    .setAuthor("Member left the server!"
                        , null, e.getUser().getEffectiveAvatarUrl())
                    .setDescription("**Member**: " + e.getUser().getAsMention() + "\n" +
                        "**User**: " + e.getUser().getName() + "#" + e.getUser().getDiscriminator() + "\n" +
                        "**Account Age**: " + e.getUser().getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                    .setFooter("UserID: " + e.getUser().getId())
                    .setTimestamp(Instant.now())
                    .queue();
            }
        }
    }

    private boolean checkAccountAge(GuildMemberJoinEvent event) {
        return TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - event.getMember().getUser().getTimeCreated().toInstant().toEpochMilli()) < 60;
    }

    private void messageUpdateEvent(GuildMessageUpdateEvent event, TextChannel tc) {
        MessageCache cache = MessageCache.getCache(event.getGuild().getIdLong());

        if (cache.isInCache(event.getMessage())) {
            Message newMessage = event.getMessage();
            CachedMessage oldMessage = cache.get(event.getMessageIdLong());
            MessageChannel channel = event.getChannel();
            String oldContent = oldMessage.getContentRaw();
            String newContent = newMessage.getContentRaw();
            Guild guild = event.getGuild();

            if (newMessage.getAuthor().isBot()) return;
            if (newContent.length() >= 2000) newContent = newContent.substring(0, 1500) + " **...**";
            if (oldContent.length() >= 2000) newContent = newContent.substring(0, 1500) + " **...**";

            if (oldContent.equals(newContent) && oldMessage.getEmbedList().size() == newMessage.getEmbeds().size()) {
                if (!oldMessage.isPinned()) {
                    tc.sendMessage(MessageFactory.makeEmbeddedMessage(tc)
                        .setAuthor("A message was pinned", newMessage.getJumpUrl(), guild.getIconUrl())
                        .setDescription("**Message sent by**: " + newMessage.getAuthor().getAsMention() +
                            "\n**Sent In**: " + guild.getTextChannelById(channel.getId()).getAsMention() +
                            "\n**Sent On**: " + newMessage.getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) +
                            "\n**[Pinned message](:jumpurl)**")
                        .setColor(new Color(211, 255, 0))
                        .setThumbnail(oldMessage.getAttachment())
                        .setTimestamp(Instant.now()).set("jumpurl", newMessage.getJumpUrl())
                        .buildEmbed()).queue();
                } else {
                    tc.sendMessage(MessageFactory.makeEmbeddedMessage(tc)
                        .setAuthor("A message was unpinned", newMessage.getJumpUrl(), guild.getIconUrl())
                        .setDescription("**Message sent by**: " + newMessage.getAuthor().getAsMention() +
                            "\n**Sent In**: " + guild.getTextChannelById(channel.getId()).getAsMention() +
                            "\n**Sent On**: " + newMessage.getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) +
                            "\n**[Unpinned message](:jumpurl)**")
                        .setColor(new Color(255, 61, 0))
                        .setThumbnail(oldMessage.getAttachment())
                        .setTimestamp(Instant.now()).set("jumpurl", newMessage.getJumpUrl())
                        .buildEmbed()).queue();
                }
            } else {
                tc.sendMessage(MessageFactory.makeEmbeddedMessage(tc)
                    .setAuthor("A message was edited", newMessage.getJumpUrl(), newMessage.getAuthor().getEffectiveAvatarUrl())
                    .setDescription("**Author**: " + newMessage.getAuthor().getAsMention() +
                        "\n**Sent In**: " + guild.getTextChannelById(channel.getId()).getAsMention() +
                        "\n**Sent On**: " + newMessage.getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) +
                        "\n\n**Message Content Before**:\n" + oldContent +
                        "\n\n**Message Content After**:\n" + newContent)
                    .setColor(new Color(0, 255, 171))
                    .setThumbnail(oldMessage.getAttachment())
                    .setTimestamp(Instant.now())
                    .buildEmbed()).queue();
            }

            cache.update(oldMessage, new CachedMessage(newMessage));

        }
    }

    private void messageDeleteEvent(GuildMessageDeleteEvent event, TextChannel tc) {
        if (event.getChannel().getType().equals(ChannelType.TEXT)) {
            MessageCache cache = MessageCache.getCache(event.getGuild());

            if (cache.isInCache(event.getMessageIdLong())) {
                CachedMessage message = cache.get(event.getMessageIdLong());
                Guild guild = event.getGuild();
                MessageChannel channel = event.getChannel();
                String content = message.getContentRaw();

                if (tc != null) {
                    if (message.getAuthor().isBot()) return;
                    if (content.length() >= 1500) content = content.substring(0, 1500) + " **...**";

                    tc.sendMessage(MessageFactory.makeEmbeddedMessage(tc)
                        .setAuthor("A message was deleted", null, message.getAuthor().getGetEffectiveAvatarUrl())
                        .setDescription("**Author**: " + message.getAuthor().getAsMention() +
                            "\n**Sent In**: " + guild.getTextChannelById(channel.getId()).getAsMention() +
                            "\n**Sent On**: " + message.getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) +
                            "\n\n**Message Content**:\n" + content)
                        .setColor(new Color(255, 0, 0))
                        .setTimestamp(Instant.now())
                        .buildEmbed()).queue();
                    cache.remove(message);
                }
            }
        }
    }

    private GuildChannel getModifiedChannel(GenericGuildEvent event, boolean newChannel) {
        if (event instanceof GuildUpdateAfkChannelEvent) {
            if (newChannel) {
                return ((GuildUpdateAfkChannelEvent) event).getNewAfkChannel();
            } else {
                return ((GuildUpdateAfkChannelEvent) event).getOldAfkChannel();
            }
        } else if (event instanceof GuildUpdateSystemChannelEvent) {
            if (newChannel) {
                return ((GuildUpdateSystemChannelEvent) event).getNewSystemChannel();
            } else {
                return ((GuildUpdateSystemChannelEvent) event).getOldSystemChannel();
            }
        }
        return null;
    }

}
