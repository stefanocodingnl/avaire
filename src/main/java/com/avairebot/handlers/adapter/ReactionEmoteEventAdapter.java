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

package com.avairebot.handlers.adapter;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.contracts.handlers.EventAdapter;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.collection.DataRow;
import com.avairebot.database.controllers.GuildController;
import com.avairebot.database.controllers.ReactionController;
import com.avairebot.database.query.QueryBuilder;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.database.transformers.ReactionTransformer;
import com.avairebot.factories.MessageFactory;
import com.avairebot.handlers.DatabaseEventHolder;
import com.avairebot.scheduler.tasks.DrainReactionRoleQueueTask;
import com.avairebot.utilities.CheckPermissionUtil;
import com.avairebot.utilities.RoleUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.emote.EmoteRemovedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ReactionEmoteEventAdapter extends EventAdapter {

    public ReactionEmoteEventAdapter(AvaIre avaire) {
        super(avaire);
    }

    public void onEmoteRemoved(EmoteRemovedEvent event) {
        Collection collection = ReactionController.fetchReactions(avaire, event.getGuild());
        if (collection == null || collection.isEmpty()) {
            return;
        }

        boolean wasActionTaken = false;
        for (DataRow row : collection) {
            ReactionTransformer transformer = new ReactionTransformer(row);

            if (transformer.removeReaction(event.getEmote())) {
                try {
                    QueryBuilder query = avaire.getDatabase().newQueryBuilder(Constants.REACTION_ROLES_TABLE_NAME)
                        .useAsync(true)
                        .where("guild_id", transformer.getGuildId())
                        .where("message_id", transformer.getMessageId());

                    if (transformer.getRoles().isEmpty()) {
                        query.delete();
                    } else {
                        query.update(statement -> {
                            statement.set("roles", AvaIre.gson.toJson(transformer.getRoles()));
                        });
                    }

                    wasActionTaken = true;
                } catch (SQLException ignored) {
                    // Since the query is running asynchronously the error will never
                    // actually be catched here since the database thread running
                    // the query will log the error instead.
                }
            }
        }

        if (wasActionTaken) {
            ReactionController.forgetCache(event.getGuild().getIdLong());
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getReactionEmote().isEmote()) {

            ReactionTransformer transformer = getReactionTransformerFromMessageIdAndCheckPermissions(
                event.getGuild(), event.getMessageId(), event.getReactionEmote().getEmote().getIdLong()
            );

            if (transformer == null) {
                return;
            }

            Role role = event.getGuild().getRoleById(transformer.getRoleIdFromEmote(event.getReactionEmote().getEmote()));
            if (role == null) {
                return;
            }

            if (RoleUtil.hasRole(event.getMember(), role) || !event.getGuild().getSelfMember().canInteract(role)) {
                return;
            }

            DrainReactionRoleQueueTask.queueReactionActionEntity(new DrainReactionRoleQueueTask.ReactionActionEntity(
                event.getGuild().getIdLong(),
                event.getMember().getUser().getIdLong(),
                role.getIdLong(),
                DrainReactionRoleQueueTask.ReactionActionType.ADD
            ));
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        if (event.getReactionEmote().isEmote()) {
            ReactionTransformer transformer = getReactionTransformerFromMessageIdAndCheckPermissions(
                event.getGuild(), event.getMessageId(), event.getReactionEmote().getEmote().getIdLong()
            );

            if (transformer == null) {
                return;
            }

            Role role = event.getGuild().getRoleById(transformer.getRoleIdFromEmote(event.getReactionEmote().getEmote()));
            if (role == null) {
                return;
            }

            if (!RoleUtil.hasRole(event.getMember(), role) || !event.getGuild().getSelfMember().canInteract(role)) {
                return;
            }

            DrainReactionRoleQueueTask.queueReactionActionEntity(new DrainReactionRoleQueueTask.ReactionActionEntity(
                event.getGuild().getIdLong(),
                event.getMember().getUser().getIdLong(),
                role.getIdLong(),
                DrainReactionRoleQueueTask.ReactionActionType.REMOVE
            ));
        }
    }

    private ReactionTransformer getReactionTransformerFromMessageIdAndCheckPermissions(@Nonnull Guild guild, @Nonnull String messageId, long emoteId) {
        if (!hasPermission(guild)) {
            return null;
        }

        Collection collection = ReactionController.fetchReactions(avaire, guild);
        if (collection == null || collection.isEmpty()) {
            return null;
        }

        ReactionTransformer transformer = getReactionTransformerFromId(collection, messageId);
        if (transformer == null || !transformer.getRoles().containsKey(emoteId)) {
            return null;
        }
        return transformer;
    }

    private boolean hasPermission(Guild guild) {
        return guild.getSelfMember().hasPermission(Permission.ADMINISTRATOR)
            || guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES);
    }

    @Nullable
    private ReactionTransformer getReactionTransformerFromId(@Nonnull Collection collection, @Nonnull String messageId) {
        List <DataRow> messages = collection.where("message_id", messageId);
        if (messages.isEmpty()) {
            return null;
        }
        return new ReactionTransformer(messages.get(0));
    }


    /**
     * Some custom stuff, related to Pinewood specifically.
     */

    public void onGuildSuggestionValidation(GuildMessageReactionAddEvent e) {
        loadDatabasePropertiesIntoMemory(e).thenAccept(databaseEventHolder -> {
            if (databaseEventHolder.getGuild().getVoteValidationChannel() != null) {
                if (e.getChannel().getId().equals(databaseEventHolder.getGuild().getVoteValidationChannel())) {
                    e.getChannel().retrieveMessageById(e.getMessageId()).queue(
                        message -> {
                            if (message.getEmbeds().size() > 0) {
                                try {
                                    if (e.getReactionEmote().getName().equals("✅")) {
                                        message.editMessage(MessageFactory.makeEmbeddedMessage(message.getTextChannel(), new Color(0, 255, 0))
                                            .setAuthor(message.getEmbeds().get(0).getAuthor().getName(), null, message.getEmbeds().get(0).getAuthor().getIconUrl())
                                            .setDescription(message.getEmbeds().get(0).getDescription())
                                            .setFooter(message.getEmbeds().get(0).getFooter().getText() + " | Accepted by: " + e.getMember().getEffectiveName())
                                            .setTimestamp(Instant.now()).buildEmbed()).queue();
                                        avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTE_TABLE_NAME).useAsync(true).where("vote_message_id", message.getId())
                                            .update(statement -> {
                                                statement.set("accepted", true);
                                            });

                                    } else if (e.getReactionEmote().getName().equals("❌")) {
                                        message.editMessage(MessageFactory.makeEmbeddedMessage(message.getTextChannel(), new Color(255, 0, 0))
                                            .setAuthor(message.getEmbeds().get(0).getAuthor().getName(), null, message.getEmbeds().get(0).getAuthor().getIconUrl())
                                            .setDescription(message.getEmbeds().get(0).getDescription())
                                            .setFooter(message.getEmbeds().get(0).getFooter().getText() + " | Denied by: " + e.getMember().getEffectiveName())
                                            .setTimestamp(Instant.now()).buildEmbed()).queue();

                                        avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTE_TABLE_NAME).useAsync(true)
                                            .where("vote_message_id", e.getMessageId())
                                            .delete();

                                    }
                                    message.clearReactions().queue();
                                } catch (SQLException throwables) {
                                    throwables.printStackTrace();
                                }
                            }
                        }

                    );
                }
            }
        });
    }


    private String getRole(GuildMessageReceivedEvent c) {
        return getString(c.getGuild(), c.getMember());
    }

    private String getRole(GuildMessageReactionAddEvent c) {
        return getString(c.getGuild(), c.getMember());
    }

    @NotNull
    private String getString(Guild guild, Member member) {
        return member.getRoles().size() > 0 ? member.getRoles().get(0).getAsMention() : "";
    }

    public void onReportsReactionAdd(GuildMessageReactionAddEvent e) {
        loadDatabasePropertiesIntoMemory(e).thenAccept(databaseEventHolder -> {
            if (databaseEventHolder.getGuild().getHandbookReportChannel() != null) {
                TextChannel tc = avaire.getShardManager().getTextChannelById(databaseEventHolder.getGuild().getHandbookReportChannel());
                if (tc != null) {
                    if (e.getChannel().equals(tc)) {
                        QueryBuilder qb = avaire.getDatabase().newQueryBuilder(Constants.REPORTS_DATABASE_TABLE_NAME).where("pb_server_id", e.getGuild().getId()).andWhere("report_message_id", e.getMessageId());
                        try {
                            DataRow c = qb.get().get(0);

                            if (qb.get().size() < 1) {
                                return;
                            }
                            String username = c.getString("reported_roblox_name");
                            String description = c.getString("report_reason");
                            String evidence = c.getString("report_evidence");
                            long reporter = c.getLong("reporter_discord_id");
                            String rank = c.getString("reported_roblox_rank");
                            Member memberAsReporter = e.getGuild().getMemberById(reporter);


                            switch (e.getReactionEmote().getName()) {
                                case "\uD83D\uDC4D": // 👍
                                    if (e.getReaction().retrieveUsers().complete().size() == 31) {
                                        tc.retrieveMessageById(c.getLong("report_message_id")).queue(v -> {
                                            if (v.getEmbeds().get(0).getColor().equals(new Color(255, 120, 0))) return;
                                            v.editMessage(MessageFactory.makeEmbeddedMessage(tc, new Color(255, 120, 0))
                                                .setAuthor("Report created for: " + username, null, getImageByName(tc.getGuild(), username))
                                                .setDescription(
                                                    "**Violator**: " + username + "\n" +
                                                        (rank != null ? "**Rank**: ``:rRank``\n" : "") +
                                                        "**Information**: \n" + description + "\n\n" +
                                                        "**Evidence**: \n" + evidence)
                                                .requestedBy(memberAsReporter != null ? memberAsReporter : e.getMember())
                                                .setTimestamp(Instant.now()).set("rRank", rank)
                                                .buildEmbed())
                                                .queue();
                                            v.clearReactions().queue();
                                        });
                                    }
                                    break;
                                case "✅":
                                    if (CheckPermissionUtil.getPermissionLevel(databaseEventHolder.getGuild(), e.getGuild(), e.getMember()).getLevel() >=
                                        CheckPermissionUtil.GuildPermissionCheckType.MANAGER.getLevel()) {


                                        tc.retrieveMessageById(c.getLong("report_message_id")).queue(v -> {
                                            if (v.getEmbeds().get(0).getColor().equals(new Color(0, 255, 0))) return;

                                            e.getChannel().sendMessage(e.getMember().getAsMention()).embed(MessageFactory.makeEmbeddedMessage(e.getChannel(), new Color(100, 200, 200), "You've chosen to approve a report, may I know the punishment you're giving to the user?").buildEmbed()).queue(z -> {
                                                avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, p -> {
                                                    return p.getMember().equals(e.getMember()) && e.getChannel().equals(p.getChannel());
                                                }, run -> {
                                                    v.editMessage(MessageFactory.makeEmbeddedMessage(tc, new Color(0, 255, 0))
                                                        .setAuthor("Report created for: " + username, null, getImageByName(tc.getGuild(), username))
                                                        .setDescription(
                                                            "**Violator**: " + username + "\n" +
                                                                (rank != null ? "**Rank**: ``:rRank``\n" : "") +
                                                                "**Information**: \n" + description + "\n\n" +
                                                                "**Evidence**: \n" + evidence + "\n\n" +
                                                                "**Punishment**: \n" + run.getMessage().getContentRaw())
                                                        .requestedBy(memberAsReporter != null ? memberAsReporter : e.getMember())
                                                        .setTimestamp(Instant.now()).set("rRank", rank)
                                                        .buildEmbed())
                                                        .queue();
                                                    try {
                                                        qb.useAsync(true).update(statement -> {
                                                            statement.set("report_punishment", run.getMessage().getContentRaw(), true);
                                                        });
                                                    } catch (SQLException throwables) {
                                                        e.getChannel().sendMessage(e.getMember().getAsMention()).embed(MessageFactory.makeEmbeddedMessage(e.getChannel(), new Color(255, 0, 0)).setDescription("Something went wrong in the database, please contact the developer.").setFooter("This message will self-destruct in 30s").buildEmbed()).queue(n -> {
                                                            n.delete().queueAfter(30, TimeUnit.SECONDS);
                                                        });
                                                    }
                                                    z.delete().queue();
                                                    v.clearReactions().queue();
                                                    run.getMessage().delete().queue();
                                                });
                                            });
                                        });
                                    } else {
                                        e.getChannel().sendMessage(e.getMember().getAsMention()).embed(MessageFactory.makeEmbeddedMessage(e.getChannel(), new Color(255, 0, 0)).setDescription("Sorry, but you're not allowed to approve this report. You have to be at least a **Manager** to do this.").setFooter("This message will self-destruct in 30s").buildEmbed()).queue(v -> {
                                            v.delete().queueAfter(30, TimeUnit.SECONDS);
                                        });
                                    }
                                    break;
                                case "❌":
                                    if (CheckPermissionUtil.getPermissionLevel(databaseEventHolder.getGuild(), e.getGuild(), e.getMember()).getLevel() >=
                                        CheckPermissionUtil.GuildPermissionCheckType.MANAGER.getLevel()) {


                                        tc.retrieveMessageById(c.getLong("report_message_id")).queue(v -> {
                                            if (v.getEmbeds().get(0).getColor().equals(new Color(0, 255, 0))) return;

                                            e.getChannel().sendMessage(e.getMember().getAsMention()).embed(MessageFactory.makeEmbeddedMessage(e.getChannel(), new Color(100, 200, 200), "You've chosen to reject a report, may I know the reason you're giving for this?").buildEmbed()).queue(z -> {
                                                avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, p -> {
                                                    return p.getMember().equals(e.getMember()) && e.getChannel().equals(p.getChannel());
                                                }, run -> {
                                                    v.editMessage(MessageFactory.makeEmbeddedMessage(tc, new Color(0, 255, 0))
                                                        .setAuthor("Report created for: " + username, null, getImageByName(tc.getGuild(), username))
                                                        .setDescription(
                                                            "**Violator**: " + username + "\n" +
                                                                (rank != null ? "**Rank**: ``:rRank``\n" : "") +
                                                                "**Information**: \n" + description + "\n\n" +
                                                                "**Evidence**: \n" + evidence + "\n\n" +
                                                                "**Denial Reason**: \n" + run.getMessage().getContentRaw())
                                                        .requestedBy(memberAsReporter != null ? memberAsReporter : e.getMember())
                                                        .setTimestamp(Instant.now()).set("rRank", rank)
                                                        .buildEmbed())
                                                        .queue();
                                                    v.clearReactions().queue();
                                                    try {
                                                        qb.useAsync(true).delete();
                                                    } catch (SQLException throwables) {
                                                        e.getChannel().sendMessage(e.getMember().getAsMention()).embed(MessageFactory.makeEmbeddedMessage(e.getChannel(), new Color(255, 0, 0)).setDescription("Something went wrong in the database, please contact the developer.").setFooter("This message will self-destruct in 30s").buildEmbed()).queue(n -> {
                                                            n.delete().queueAfter(30, TimeUnit.SECONDS);
                                                        });
                                                    }
                                                    z.delete().queue();
                                                    v.clearReactions().queue();
                                                    run.getMessage().delete().queue();
                                                });
                                            });
                                        });
                                    } else {
                                        e.getChannel().sendMessage(e.getMember().getAsMention()).embed(MessageFactory.makeEmbeddedMessage(e.getChannel(), new Color(255, 0, 0)).setDescription("Sorry, but you're not allowed to reject this report. You have to be at least a **Manager** to do this.").setFooter("This message will self-destruct in 30s").buildEmbed()).queue(v -> {
                                            v.delete().queueAfter(30, TimeUnit.SECONDS);
                                        });
                                    }
                                    break;
                                case "🚫":
                                    if (CheckPermissionUtil.getPermissionLevel(databaseEventHolder.getGuild(), e.getGuild(), e.getMember()).getLevel() >=
                                        CheckPermissionUtil.GuildPermissionCheckType.MANAGER.getLevel()) {


                                        tc.retrieveMessageById(c.getLong("report_message_id")).queue(v -> {
                                            v.delete().queue();
                                        });
                                        try {
                                            qb.useAsync(true).delete();
                                        } catch (SQLException throwables) {
                                            throwables.printStackTrace();
                                        }


                                    } else {
                                        e.getChannel().sendMessage(e.getMember().getAsMention()).embed(MessageFactory.makeEmbeddedMessage(e.getChannel(), new Color(255, 0, 0)).setDescription("Sorry, but you're not allowed to approve this report. You have to be at least a **Manager** to do this.").setFooter("This message will self-destruct in 30s").buildEmbed()).queue(v -> {
                                            v.delete().queueAfter(30, TimeUnit.SECONDS);
                                        });
                                    }
                                case "\uD83D\uDD04": // 🔄
                                    if (CheckPermissionUtil.getPermissionLevel(databaseEventHolder.getGuild(), e.getGuild(), e.getMember()).getLevel() >=
                                        CheckPermissionUtil.GuildPermissionCheckType.MOD.getLevel()) {

                                        tc.retrieveMessageById(c.getLong("report_message_id")).queue(v -> {
                                            v.clearReactions().queue();
                                            createReactions(v);
                                        });
                                    } else {
                                        e.getChannel().sendMessage(e.getMember().getAsMention()).embed(MessageFactory.makeEmbeddedMessage(e.getChannel(), new Color(255, 0, 0)).setDescription("Sorry, but you're not allowed to approve this report. You have to be at least a **Manager** to do this.").setFooter("This message will self-destruct in 30s").buildEmbed()).queue(v -> {
                                            v.delete().queueAfter(30, TimeUnit.SECONDS);
                                        });
                                    }
                                    break;
                            }
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }

                    }
                }
            }
        });
    }

    public void onSuggestionReactionEvent(GuildMessageReactionAddEvent e) {
        /*loadDatabasePropertiesIntoMemory(e).thenAccept(databaseEventHolder -> {
            if (databaseEventHolder.getGuild().getHandbookReportChannel() != null) {
                TextChannel tc = avaire.getShardManager().getTextChannelById(databaseEventHolder.getGuild().getHandbookReportChannel());
                if (tc != null) {
                    if (e.getChannel().equals(tc)) {
                        QueryBuilder qb = avaire.getDatabase().newQueryBuilder(Constants.REPORTS_DATABASE_TABLE_NAME).where("pb_server_id", e.getGuild().getId()).andWhere("report_message_id", e.getMessageId());
                        try {
                            DataRow c = qb.get().get(0);

                            String username = c.getString("reported_roblox_name");
                            String description = c.getString("report_reason");
                            String evidence = c.getString("report_evidence");
                            Long reporter = c.getLong("reporter_discord_id");
                            Member m = e.getGuild().getMemberById(reporter);

                            switch (e.getReactionEmote().getName()) {
                                case "\uD83D\uDC4D": // 👍
                                    switch (e.getReaction().getCount()) {
                                        case 2:


                                            tc.retrieveMessageById(c.getLong("suggestion_message_id")).queue(v -> {
                                                v.editMessage(MessageFactory.makeEmbeddedMessage(tc, new Color(255, 120, 0))
                                                    .setAuthor("Report created for: " + username, null, getImageByName(tc.getGuild(), username))
                                                    .setDescription(
                                                        "**Violator**: " + username + "\n" +
                                                            (c.getString("reported_roblox_rank") != null ? "**Rank**: ``:rRank``\n" : "") +
                                                            "**Information**: \n" + description + "\n\n" +
                                                            "**Evidence**: \n" + evidence)
                                                    .requestedBy(m != null ? m : e.getMember())
                                                    .setTimestamp(Instant.now())
                                                    .buildEmbed())
                                                    .queue();
                                            });
                                            e.getReaction().clearReactions().queue();
                                    }
                                case "\uD83D\uDC4E": // 👎

                                case "✅":
                                case "❌":
                                case "🚫":
                                case "\uD83D\uDCAC": // 💬
                                case "\uD83D\uDD04": // 🔄

                            }
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }

                    }
                }
            }
        });*/
    }

    private String getImageByName(Guild guild, String username) {
        List <Member> members = guild.getMembersByEffectiveName(username, true);

        if (members.size() < 1) return null;
        if (members.size() > 1) return null;
        else return members.get(0).getUser().getEffectiveAvatarUrl();
    }

    private void count(GuildMessageReactionAddEvent event, Message m, Message v, GuildMessageReceivedEvent c) {
        int likes = 0, dislikes = 0;
        for (MessageReaction reaction : m.getReactions()) {
            if (reaction.getReactionEmote().getName().equals("\uD83D\uDC4D")) {
                likes = reaction.getCount();
            }

            if (reaction.getReactionEmote().getName().equals("\uD83D\uDC4E")) {
                dislikes = reaction.getCount();
            }
        }
        m.editMessage(new EmbedBuilder()
            .setDescription(m.getEmbeds().get(0).getDescription() + "\n\n**Denial Reason given by " + getRole(event) + " " + event.getMember().getEffectiveName() + "**: \n" + c.getMessage().getContentRaw() + "\n\n**Public vote**:\n :+1: - " + likes + "\n:-1: - " + dislikes)
            .setTitle(m.getEmbeds().get(0).getTitle() + " | Denied by " + c.getMember().getEffectiveName())
            .setFooter(m.getEmbeds().get(0).getFooter().getText(), m.getEmbeds().get(0).getFooter().getIconUrl())
            .setTimestamp(m.getEmbeds().get(0).getTimestamp())
            .setColor(new Color(255, 0, 0))
            .build()).queue();

        v.delete().queue();
        c.getMessage().delete().queue();
        m.clearReactions().queue();
    }

    public void onPBSTRequestRewardMessageAddEvent(GuildMessageReactionAddEvent event) {
        MessageReaction.ReactionEmote emote = event.getReactionEmote();
        Message m = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
        if (emote.getName().equals("\uD83D\uDC4D") | emote.getName().equals("\uD83D\uDC4E")) {
            /*if (m.getEmbeds().get(0).getTitle().equals(event.getMember().getEffectiveName()) || m.getEmbeds().get(0).getFooter().getText().contains(event.getMember().getEffectiveName())) {
                event.getReaction().removeReaction(event.getUser()).queue();
                event.getMember().getUser().openPrivateChannel().complete().sendMessage("You reacted to a reward request that includes your name, you cannot do this.").queue();
                return;
            }*/
        }
        if (emote.getName().equals("✅") || emote.getName().equals("❌")) {
            if (!(isValidReportManager(event, 1) || event.getMember().getId().equals("173839105615069184"))) {
                event.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need to be the manager of points in your division to approve or deny a reward request.").build()).queue());
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);
                return;
            }

            if (emote.getName().equals("✅")) {
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);
                event.getChannel().sendMessage(event.getMember().getAsMention() + "\n" +
                    "You've chosen to approve this reward request, what reward will you give?").queue(v ->
                    avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, c -> c.getChannel().equals(event.getChannel()) && c.getMember().equals(event.getMember()), c -> {
                        int likes = 0, dislikes = 0;
                        for (MessageReaction reaction : m.getReactions()) {
                            if (reaction.getReactionEmote().getName().equals("\uD83D\uDC4D")) {
                                likes = reaction.getCount();
                            }

                            if (reaction.getReactionEmote().getName().equals("\uD83D\uDC4E")) {
                                dislikes = reaction.getCount();
                            }
                        }
                        m.editMessage(new EmbedBuilder()
                            .setDescription(m.getEmbeds().get(0).getDescription() + "\n\n**Reward given by " + getRole(event) + " " + event.getMember().getEffectiveName() + "**: \n" + c.getMessage().getContentRaw() + "\n\n**Public vote**:\n :+1: - " + likes + "\n:-1: - " + dislikes)
                            .setTitle(m.getEmbeds().get(0).getTitle() + " | Approved by " + c.getMember().getEffectiveName())
                            .setTimestamp(m.getEmbeds().get(0).getTimestamp())
                            .setFooter(m.getEmbeds().get(0).getFooter().getText(), m.getEmbeds().get(0).getFooter().getIconUrl())
                            .setColor(new Color(0, 255, 0))
                            .build()).queue();
                        v.delete().queue();
                        c.getMessage().delete().queue();
                        m.clearReactions().queue();
                        if (event.getGuild().getMembersByEffectiveName(m.getEmbeds().get(0).getFooter().getText(), true).size() > 0) {
                            for (Member u : event.getGuild().getMembersByEffectiveName(m.getEmbeds().get(0).getTitle(), true)) {
                                u.getUser().openPrivateChannel().complete()
                                    .sendMessage(new EmbedBuilder()
                                        .setDescription("Hello there ``" + u.getEffectiveName() + "``.\n" +
                                            "It seems like you have gotten reward!\n" +
                                            "If you want to check the report, [click here](" + m.getJumpUrl() + ")\n" +
                                            "The reward was given by **" + event.getMember().getEffectiveName() + "** in ``" + event.getGuild().getName() + "``!\n\n" +
                                            "**The reward**:\n" + c.getMessage().getContentRaw()).build()).queue();
                            }
                        }
                    }, 90, TimeUnit.SECONDS, () -> {
                        v.delete().queue();
                        m.getMember().getUser().openPrivateChannel().complete().sendMessage("You took to long to send a reaction, please re-react to the message!").queue();
                    }));

            }
            if (emote.getName().equals("❌")) {
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);

                event.getChannel().sendMessage(event.getMember().getAsMention() + "\n" +
                    "You've chosen to deny this reward request, what reason will you give?").queue(v -> avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, c -> c.getChannel().equals(event.getChannel()) && c.getMember().equals(event.getMember()), c -> {
                    count(event, m, v, c);
                    if (event.getGuild().getMembersByEffectiveName(m.getEmbeds().get(0).getFooter().getText(), true).size() > 0) {
                        for (Member u : event.getGuild().getMembersByEffectiveName(m.getEmbeds().get(0).getFooter().getText(), true)) {
                            u.getUser().openPrivateChannel().complete()
                                .sendMessage(new EmbedBuilder()
                                    .setColor(new Color(255, 0, 0))
                                    .setDescription("Hello there ``" + u.getEffectiveName() + "``.\n" +
                                        "It seems like your reward request on ``" + m.getEmbeds().get(0).getTitle() + "`` has been denied!\n" +
                                        "If you want to check the reward request, [click here](" + m.getJumpUrl() + ")\n" +
                                        "The denial was given by **" + event.getMember().getEffectiveName() + "** in ``" + event.getGuild().getName() + "``!\n\n" +
                                        "**The reason**:\n" + c.getMessage().getContentRaw()).build()).queue();
                        }
                    }
                }, 90, TimeUnit.SECONDS, () -> {
                    v.delete().queue();
                    m.getMember().getUser().openPrivateChannel().complete().sendMessage("You took to long to send a comment, please re-react to the message!").queue();
                }));

            }
        }
        if (emote.getName().equals("trash")) {
            if (!(isValidReportManager(event, 1) || event.getMember().getId().equals("173839105615069184"))) {
                event.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need to be the manager of points in your division to remove a report.").build()).queue());
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);
                return;
            }
            m.delete().queue();
        }
        if (emote.getName().equals("\uD83D\uDD04")) {
            if (!(isValidReportManager(event, 1) || event.getMember().getId().equals("173839105615069184"))) {
                event.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need to be the manager of points in your division to refresh the emoji's.").build()).queue());
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);
                return;
            }
            m.clearReactions().queue();
            m.addReaction("\uD83D\uDC4D").queue();
            m.addReaction("\uD83D\uDC4E").queue();
            m.addReaction("✅").queue();
            m.addReaction("❌").queue();
            m.addReaction("🚫").queue();
            m.addReaction("\uD83D\uDD04").queue();
        }
    }

    private boolean isValidReportManagerRole(GuildMessageReactionAddEvent e) {
        /*if (e.getGuild().getRolesByName("Trainer", true).size() > 0
            || e.getGuild().getRolesByName("Division Trainer", true).size() > 0
            || e.getGuild().getRolesByName("Team Chief", true).size() > 0
            || e.getGuild().getRolesByName("Instructor", true).size() > 0
            || e.getGuild().getRolesByName("The Architect", true).size() > 0 || e.getGuild().getRolesByName("Admins", true).size() > 0 || e.getGuild().getRolesByName("PIA", true).size() > 0 || e.getGuild().getRolesByName("Owner", true).size() > 0) {
            Role r = getGuildRole(e);
            if (r != null) {
                return e.getMember().getRoles().contains(r) || e.getMember().hasPermission(Permission.ADMINISTRATOR);
            }
        }*/
        return false;
    }

    public static void createReactions(Message r) {
            r.addReaction("\uD83D\uDC4D").queue();   // 👍
            r.addReaction("\uD83D\uDC4E").queue();  // 👎
            r.addReaction("✅").queue();
            r.addReaction("❌").queue();
            r.addReaction("🚫").queue();
            r.addReaction("\uD83D\uDD04").queue(); // 🔄
    }

    private boolean isValidReportManager(GuildMessageReactionAddEvent e, Integer i) {
        GuildTransformer transformer = GuildController.fetchGuild(avaire, e.getGuild());
        List <Role> roles = new ArrayList <>();
        if (i == 1) {
            if (transformer != null) {
                for (Long roleId : transformer.getModeratorRoles()) {
                    Role r = e.getGuild().getRoleById(roleId);
                    if (r != null) {
                        roles.add(r);
                    }
                }
                for (Long roleId : transformer.getManagerRoles()) {
                    Role r = e.getGuild().getRoleById(roleId);
                    if (r != null) {
                        roles.add(r);
                    }
                }
                for (Long roleId : transformer.getAdministratorRoles()) {
                    Role r = e.getGuild().getRoleById(roleId);
                    if (r != null) {
                        roles.add(r);
                    }
                }
            }
        }
        if (i == 2) {
            if (transformer != null) {
                for (Long roleId : transformer.getAdministratorRoles()) {
                    Role r = e.getGuild().getRoleById(roleId);
                    if (r != null) {
                        roles.add(r);
                    }
                }

            }
        }
        if (i == 3) {
            if (transformer != null) {
                for (Long roleId : transformer.getAdministratorRoles()) {
                    Role r = e.getGuild().getRoleById(roleId);
                    if (r != null) {
                        roles.add(r);
                    }
                }
                for (Long roleId : transformer.getManagerRoles()) {
                    Role r = e.getGuild().getRoleById(roleId);
                    if (r != null) {
                        roles.add(r);
                    }
                }

            }
        }
        if (roles.size() == 0) {
            return false;
        }
        return e.getMember().getRoles().stream().anyMatch(roles::contains);
    }


    private CompletableFuture <DatabaseEventHolder> loadDatabasePropertiesIntoMemory(final GuildMessageReactionAddEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            if (!event.getChannel().getType().isGuild()) {
                return new DatabaseEventHolder(null, null);
            }

            GuildTransformer guild = GuildController.fetchGuild(avaire, event.getGuild());

            if (guild == null || !guild.isLevels() || event.getMember().getUser().isBot()) {
                return new DatabaseEventHolder(guild, null);
            }
            return new DatabaseEventHolder(guild, null);
        });
    }


}
