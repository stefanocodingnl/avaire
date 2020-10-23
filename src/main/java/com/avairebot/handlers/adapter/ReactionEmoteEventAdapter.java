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
import com.avairebot.database.controllers.PlayerController;
import com.avairebot.database.controllers.ReactionController;
import com.avairebot.database.query.QueryBuilder;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.database.transformers.ReactionTransformer;
import com.avairebot.factories.MessageFactory;
import com.avairebot.handlers.DatabaseEventHolder;
import com.avairebot.pinewood.waiters.FeedbackWaiters;
import com.avairebot.scheduler.tasks.DrainReactionRoleQueueTask;
import com.avairebot.utilities.RoleUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.emote.EmoteRemovedEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.GenericEvent;
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
        List<DataRow> messages = collection.where("message_id", messageId);
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
                    // TODO: Code approve and deny system
                }
            }
        });
    }

    public void onPBFeedbackMessageEvent(GuildMessageReactionAddEvent e) {
        e.getChannel().retrieveMessageById(e.getMessageId()).queue(msg -> {
            if (e.getReactionEmote().getName().equals("\uD83D\uDC4D") | e.getReactionEmote().getName().equals("\uD83D\uDC4E")) {

                int likes = 0, dislikes = 0;
                for (MessageReaction reaction : msg.getReactions()) {
                    if (reaction.getReactionEmote().getName().equals("\uD83D\uDC4D")) {
                        likes = reaction.getCount();
                    }

                    if (reaction.getReactionEmote().getName().equals("\uD83D\uDC4E")) {
                        dislikes = reaction.getCount();
                    }
                }

                if (likes > 29) {
                    msg.editMessage(new EmbedBuilder()
                        .setColor(new Color(255, 150, 0))
                        .setDescription(buildSuggestionEmbed(e, likes, dislikes, msg))
                        .setTimestamp(msg.getEmbeds().get(0).getTimestamp())
                        .setFooter(msg.getEmbeds().get(0).getFooter().getText(), msg.getEmbeds().get(0).getFooter().getIconUrl()).build()).queue();
                    msg.clearReactions("\uD83D\uDC4D").queueAfter(1, TimeUnit.SECONDS);
                    msg.clearReactions("\uD83D\uDC4E").queueAfter(1, TimeUnit.SECONDS);
                    runPerGuildAction(e, msg);
                }

                if (dislikes > 39) {
                    msg.editMessage(new EmbedBuilder()
                        .setColor(new Color(255, 0, 0))
                        .setDescription(buildDeniedSuggestionEmbed(msg, likes, dislikes))
                        .setTimestamp(msg.getEmbeds().get(0).getTimestamp())
                        .setFooter(msg.getEmbeds().get(0).getFooter().getText(), msg.getEmbeds().get(0).getFooter().getIconUrl()).build()).queue();
                    msg.clearReactions().queue();
                }
            }
            if (e.getReactionEmote().getName().equals("❌") || e.getReactionEmote().getName().equals("✅") || e.getReactionEmote().getName().equals("\uD83D\uDD04")) {
                switch (e.getReactionEmote().getName()) {
                    case "❌":
                        if (!(isValidReportManager(e, 1) || e.getMember().getId().equals("173839105615069184"))) {
                            e.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need a mod role to deny a suggestion.").build()).queue());
                            e.getReaction().removeReaction(e.getUser()).queueAfter(1, TimeUnit.SECONDS);
                            return;
                        }
                        msg.editMessage(new EmbedBuilder()
                            .setColor(new Color(255, 0, 0))
                            .setAuthor("Denied by " + e.getMember().getEffectiveName(), null, e.getUser().getEffectiveAvatarUrl())
                            .setDescription(msg.getEmbeds().get(0).getDescription())
                            .setTimestamp(msg.getEmbeds().get(0).getTimestamp())
                            .setFooter(msg.getEmbeds().get(0).getFooter().getText(), msg.getEmbeds().get(0).getFooter().getIconUrl()).build()).queue();
                        msg.clearReactions().queue();
                        if (msg.isPinned()) {
                            msg.unpin().queueAfter(1, TimeUnit.SECONDS);
                        }
                        break;
                    case "✅":
                        if (!(isValidReportManager(e, 2) || e.getMember().getId().equals("173839105615069184"))) {
                            e.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need a manager role to approve a suggestion.").build()).queue());
                            e.getReaction().removeReaction(e.getUser()).queueAfter(1, TimeUnit.SECONDS);
                            return;
                        }
                        msg.editMessage(new EmbedBuilder()
                            .setColor(new Color(0, 255, 0))
                            .setAuthor("Approved by " + e.getMember().getEffectiveName(), null, e.getUser().getEffectiveAvatarUrl())
                            .setDescription(msg.getEmbeds().get(0).getDescription())
                            .setTimestamp(msg.getEmbeds().get(0).getTimestamp())
                            .setFooter(msg.getEmbeds().get(0).getFooter().getText(), msg.getEmbeds().get(0).getFooter().getIconUrl()).build()).queue(r -> {

                            sendApprovedMessage(e, r);
                        });
                        msg.clearReactions().queue();
                        if (msg.isPinned()) {
                            msg.unpin().queueAfter(1, TimeUnit.SECONDS);
                        }
                        break;
                    case "\uD83D\uDD04":
                        if (!(isValidReportManager(e, 1) || e.getMember().getId().equals("173839105615069184"))) {
                            e.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need a mod role to refresh the icons.").build()).queue());
                            e.getReaction().removeReaction(e.getUser()).queueAfter(1, TimeUnit.SECONDS);
                            return;
                        }
                        msg.clearReactions().queue();
                        msg.addReaction("\uD83D\uDC4D").queue();
                        msg.addReaction("\uD83D\uDC4E").queue();
                        msg.addReaction("✅").queue();
                        msg.addReaction("❌").queue();
                        msg.addReaction("trash:694314074179240027").queue();
                        msg.addReaction("\uD83D\uDD04").queue();
                }
            }
            if (e.getReactionEmote().getName().equals("trash")) {
                if (!(isValidReportManager(e, 1) || e.getMember().getId().equals("173839105615069184") || msg.getEmbeds().get(0).getFooter().getText().equals(e.getMember().getEffectiveName()))) {
                    e.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need to be a mod or higher to remove a feedback.").build()).queue());
                    e.getReaction().removeReaction(e.getUser()).queueAfter(1, TimeUnit.SECONDS);
                    return;
                }
                msg.delete().queue();
            }
            if (e.getReactionEmote().getName().equals("\uD83D\uDCAC")) {
                if (!(e.getMember().hasPermission(Permission.MESSAGE_MANAGE) || isValidReportManager(e, 1) || msg.getEmbeds().get(0).getFooter().getText().equalsIgnoreCase(e.getMember().getEffectiveName()))) {
                    e.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need to be an mod or higher to comment on a suggestion!").build()).queue());
                    e.getReaction().removeReaction(e.getUser()).queueAfter(1, TimeUnit.SECONDS);
                    return;
                }
                e.getReaction().removeReaction(e.getUser()).queue();

                if (isValidReportManager(e, 1)) {
                    msg.getTextChannel().sendMessage(e.getMember().getAsMention() + "\nWhat is your comment?").queue(
                        v -> avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, c -> c.getChannel().equals(e.getChannel()) && c.getMember().equals(e.getMember()), c -> {
                            v.delete().queue();
                            msg.editMessage(new EmbedBuilder()
                                .setColor(msg.getEmbeds().get(0).getColor())
                                .setDescription(msg.getEmbeds().get(0).getDescription() + "\n\n" + getRole(c) + " - :speech_balloon: **``" + e.getMember().getEffectiveName() + "``:**\n" + c.getMessage().getContentRaw())
                                .setTimestamp(msg.getEmbeds().get(0).getTimestamp())
                                .setFooter(msg.getEmbeds().get(0).getFooter().getText(), msg.getEmbeds().get(0).getFooter().getIconUrl()).build()).queue();
                            c.getMessage().delete().queue();
                            if (e.getGuild().getMembersByEffectiveName(msg.getEmbeds().get(0).getFooter().getText(), true).size() > 0) {
                                for (Member u : e.getGuild().getMembersByEffectiveName(msg.getEmbeds().get(0).getFooter().getText(), true)) {
                                    u.getUser().openPrivateChannel().complete()
                                        .sendMessage(new EmbedBuilder()
                                            .setDescription("Hello there ``" + u.getEffectiveName() + "``.\n" +
                                                "It seems like you have gotten a comment on one of your suggestions!\n" +
                                                "If you want to check the feedback, [click here](" + msg.getJumpUrl() + ")\n" +
                                                "You received a comment from **" + e.getMember().getEffectiveName() + "** in ``" + e.getGuild().getName() + "``!\n\n" +
                                                "**Comment**:\n" + c.getMessage().getContentRaw()).build()).queue();
                                }
                            }
                        }, 90, TimeUnit.SECONDS, () -> {
                            v.delete().queue();
                            msg.getMember().getUser().openPrivateChannel().queue(l -> l.sendMessage("You took to long to send a comment, please re-react to the message!").queue());
                        })
                    );
                } else {
                    e.getMember().getUser().openPrivateChannel().queue(p -> {
                        p.sendMessage(e.getMember().getAsMention() + "\nWhat is your comment?").queue(
                            v -> avaire.getWaiter().waitForEvent(PrivateMessageReceivedEvent.class, c -> c.getChannel().equals(e.getChannel()) && c.getAuthor().equals(e.getMember().getUser()), c -> {
                                msg.editMessage(new EmbedBuilder()
                                    .setColor(msg.getEmbeds().get(0).getColor())
                                    .setDescription(msg.getEmbeds().get(0).getDescription() + "\n\n - :speech_balloon: **``" + e.getMember().getEffectiveName() + "``:**\n" + c.getMessage().getContentRaw())
                                    .setTimestamp(msg.getEmbeds().get(0).getTimestamp())
                                    .setFooter(msg.getEmbeds().get(0).getFooter().getText(), msg.getEmbeds().get(0).getFooter().getIconUrl()).build()).queue();
                            }, 90, TimeUnit.SECONDS, () -> {

                                v.delete().queue();
                                msg.getMember().getUser().openPrivateChannel().complete().sendMessage("You took to long to send a comment, please re-react to the message!").queue();
                            })
                        );
                    });
                }
            }
        });

    }

    private void runPerGuildAction(GuildMessageReactionAddEvent e, Message msg) {
        if (e.getGuild().getId().equals("371062894315569173")) {
            e.getGuild().getTextChannelById("767806684403204097").sendMessage(msg).queue(
                p -> {
                    p.addReaction(":white_check_mark:").queue();
                    p.addReaction(":x:").queue();
                }
            );
        } else {
            msg.pin().queue();
        }
    }

    private String buildDeniedSuggestionEmbed(Message msg, int likes, int dislikes) {
        if (msg.getEmbeds().get(0).getDescription().length() > 1500) {return " ";}

        return "**Reactions**: \n" +
            ":+1: - " + likes + "\n" +
            ":-1: - " + dislikes + "\n" +
            "This suggestion has now failed the community vote, this meant the suggestion wont be used.\n\n**Suggestion**:\n" + msg.getEmbeds().get(0).getDescription();
    }

    private void sendApprovedMessage(GuildMessageReactionAddEvent e, Message r) {
        if (e.getGuild().getId().equals("371062894315569173")) {
            e.getGuild().getTextChannelById(Constants.FEEDBACK_APPROVED_CHANNEL_ID).sendMessage(r).queue();
        }
    }


    private CharSequence buildSuggestionEmbed(GuildMessageReactionAddEvent e, int likes, int dislikes, Message msg) {
        if (msg.getEmbeds().get(0).getDescription().length() > 1500) {return " " + msg.getEmbeds().get(0).getDescription();}

        if (e.getGuild().getId().equals("438134543837560832") || e.getGuild().getId().equals("758057400635883580") || e.getGuild().getId().equals("436670173777362944")) {
            return "**Reactions**: \n" +
                ":+1: - " + likes + "\n" +
                ":-1: - " + dislikes + "\n" +
                "This suggestion has now passed the community vote, any " + getUserOrRankThatAccepts(e) + " will now be able to press the ✅ and ❌ to think about the suggestion!\n\n**Suggestion**:\n" + msg.getEmbeds().get(0).getDescription();
        } else if (e.getGuild().getId().equals("371062894315569173")) {
            return "**Reactions**: \n" +
                ":+1: - " + likes + "\n" +
                ":-1: - " + dislikes + "\n" +
                "This suggestion has now passed the community vote. Meaning people really want this, " + getUserOrRankThatAccepts(e) + " will be able to consider this suggestion!\n\n**Suggestion**:\n" + msg.getEmbeds().get(0).getDescription();

        }
        return "ROLE OR PERSON NOT FOUND";
    }

    private String getUserOrRankThatAccepts(GuildMessageReactionAddEvent e) {
        if (e.getGuild().getId().equals("438134543837560832")) {
            return e.getGuild().getRoleById("438136063219859458").getAsMention();
        } else if (e.getGuild().getId().equals("371062894315569173")) {
            return "<@&422187845621383179> or <@&728904323647406082> member";
        } else if (e.getGuild().getId().equals("758057400635883580")) {
            return e.getGuild().getRoleById("758062772708835429").getAsMention();
        } else if (e.getGuild().getId().equals("436670173777362944")) {
            return e.getGuild().getRoleById("440320321006862336").getAsMention();
        }
        return "<ROLE NOT FOUND>";
    }

    private String getRole(GuildMessageReceivedEvent c) {
        return getString(c.getGuild(), c.getMember());
    }

    private String getRole(GuildMessageReactionAddEvent c) {
        return getString(c.getGuild(), c.getMember());
    }

    @NotNull
    private String getString(Guild guild, Member member) {
        /*if (guild.getId().equals("438134543837560832")) {
            return member.getRoles().contains(guild.getRoleById("438136063219859458")) ? "<@&438136063219859458>" : "<@&438136063001886741>";
        }
        if (guild.getId().equals("572104809973415943")) {
            return member.getRoles().contains(guild.getRoleById("572105257811705895")) ? "<@&572105257811705895>" : "<@&572107276828278785>";
        }
        if (guild.getId().equals("436670173777362944")) {
            return member.getRoles().contains(guild.getRoleById("440320321006862336")) ? "<@&440320321006862336>" : "<@&560318095235874829>";
        }*/
        return member.getRoles().size() > 0 ? member.getRoles().get(0).getAsMention() : "";
    }

    public void onReportsReactionAdd(GuildMessageReactionAddEvent event) {
        MessageReaction.ReactionEmote emote = event.getReactionEmote();
        Message m = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
        if (emote.getName().equals("\uD83D\uDC4D") | emote.getName().equals("\uD83D\uDC4E")) {
            if (m.getEmbeds().get(0).getTitle().equals(event.getMember().getEffectiveName()) || m.getEmbeds().get(0).getFooter().getText().contains(event.getMember().getEffectiveName())) {
                event.getReaction().removeReaction(event.getUser()).queue();
                event.getMember().getUser().openPrivateChannel().complete().sendMessage("You reacted to a report that includes your name, you cannot do this.").queue();
                return;
            }
        }
        if (emote.getName().equals("✅") || emote.getName().equals("❌")) {
            if (!(isValidReportManagerRole(event) || event.getMember().getId().equals("173839105615069184"))) {
                event.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need to be the manager of points in your division to approve or deny a report.").build()).queue());
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);
                return;
            }

            if (emote.getName().equals("✅")) {
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);
                event.getChannel().sendMessage(event.getMember().getAsMention() + "\n" +
                    "You've chosen to approve this report, what punishment will you give?").queue(v ->
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
                            .setDescription(m.getEmbeds().get(0).getDescription() + "\n\n**Punishment given by " + getRole(event) + " " + event.getMember().getEffectiveName() + "**: \n" + c.getMessage().getContentRaw() + "\n\n**Public vote**:\n :+1: - " + likes + "\n:-1: - " + dislikes)
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
                                            "It seems like you have gotten a punishment for violating the rules!\n" +
                                            "If you want to check the report, [click here](" + m.getJumpUrl() + ")\n" +
                                            "The punishment was issued by **" + event.getMember().getEffectiveName() + "** in ``" + event.getGuild().getName() + "``!\n\n" +
                                            "**The punishment**:\n" + c.getMessage().getContentRaw()).build()).queue();
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
                    "You've chosen to deny this report, what reason will you give?").queue(v -> avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, c -> c.getChannel().equals(event.getChannel()) && c.getMember().equals(event.getMember()), c -> {
                    count(event, m, v, c);
                    if (event.getGuild().getMembersByEffectiveName(m.getEmbeds().get(0).getFooter().getText(), true).size() > 0) {
                        for (Member u : event.getGuild().getMembersByEffectiveName(m.getEmbeds().get(0).getFooter().getText(), true)) {
                            u.getUser().openPrivateChannel().complete()
                                .sendMessage(new EmbedBuilder()
                                    .setColor(new Color(255, 0, 0))
                                    .setDescription("Hello there ``" + u.getEffectiveName() + "``.\n" +
                                        "It seems like your report on ``" + m.getEmbeds().get(0).getTitle() + "`` has been denied!\n" +
                                        "If you want to check the report, [click here](" + m.getJumpUrl() + ")\n" +
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
            if (!(isValidReportManagerRole(event) || event.getMember().getId().equals("173839105615069184"))) {
                event.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need to be the manager of points in your division to remove a report.").build()).queue());
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);
                return;
            }
            m.delete().queue();
        }
        if (emote.getName().equals("\uD83D\uDD04")) {
            if (!(isValidReportManagerRole(event) || event.getMember().getId().equals("173839105615069184"))) {
                event.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need to be the manager of points in your division to refresh the emoji's.").build()).queue());
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);
                return;
            }
            m.clearReactions().queue();
            FeedbackWaiters.createReactions(m);
        }
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
            if (m.getEmbeds().get(0).getTitle().equals(event.getMember().getEffectiveName()) || m.getEmbeds().get(0).getFooter().getText().contains(event.getMember().getEffectiveName())) {
                event.getReaction().removeReaction(event.getUser()).queue();
                event.getMember().getUser().openPrivateChannel().complete().sendMessage("You reacted to a reward request that includes your name, you cannot do this.").queue();
                return;
            }
        }
        if (emote.getName().equals("✅") || emote.getName().equals("❌")) {
            if (!(isValidReportManagerRole(event) || event.getMember().getId().equals("173839105615069184"))) {
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
            if (!(isValidReportManagerRole(event) || event.getMember().getId().equals("173839105615069184"))) {
                event.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need to be the manager of points in your division to remove a report.").build()).queue());
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);
                return;
            }
            m.delete().queue();
        }
        if (emote.getName().equals("\uD83D\uDD04")) {
            if (!(isValidReportManagerRole(event) || event.getMember().getId().equals("173839105615069184"))) {
                event.getMember().getUser().openPrivateChannel().queue(v -> v.sendMessage(new EmbedBuilder().setDescription("Sorry, but you need to be the manager of points in your division to refresh the emoji's.").build()).queue());
                event.getReaction().removeReaction(event.getUser()).queueAfter(1, TimeUnit.SECONDS);
                return;
            }
            m.clearReactions().queue();
            m.addReaction("\uD83D\uDC4D").queue();
            m.addReaction("\uD83D\uDC4E").queue();
            m.addReaction("✅").queue();
            m.addReaction("❌").queue();
            m.addReaction("trash:694314074179240027").queue();
            m.addReaction("\uD83D\uDD04").queue();
        }
    }

    private boolean isValidReportManagerRole(GuildMessageReactionAddEvent e) {
        if (e.getGuild().getRolesByName("Trainer", true).size() > 0
            || e.getGuild().getRolesByName("Division Trainer", true).size() > 0
            || e.getGuild().getRolesByName("Team Chief", true).size() > 0
            || e.getGuild().getRolesByName("Instructor", true).size() > 0
            || e.getGuild().getRolesByName("The Architect", true).size() > 0 || e.getGuild().getRolesByName("Admins", true).size() > 0 || e.getGuild().getRolesByName("PIA", true).size() > 0 || e.getGuild().getRolesByName("Owner", true).size() > 0) {
            Role r = getGuildRole(e);
            if (r != null) {
                return e.getMember().getRoles().contains(r) || e.getMember().hasPermission(Permission.ADMINISTRATOR);
            }
        }
        return false;
    }

    private boolean isValidReportManager(GuildMessageReactionAddEvent e, Integer i) {
        GuildTransformer transformer = GuildController.fetchGuild(avaire, e.getGuild());
        List<Role> roles = new ArrayList<>();
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
        return e.getMember().getRoles().stream().anyMatch(roles::contains);
    }

    @Nullable
    private Role getGuildRole(GuildMessageReactionAddEvent e) {
        if (e.getGuild().getId().equals("436670173777362944")) {
            if (e.getMember().getRoles().contains(e.getGuild().getRolesByName("Division Trainer", true).get(0))) {
                return e.getGuild().getRolesByName("Division Trainer", true).get(0);
            }
            return e.getGuild().getRolesByName("Team Chief", true).get(0);
        }

        if (e.getGuild().getId().equals("438134543837560832")) {
            return e.getGuild().getRolesByName("Trainer", true).get(0);
        }

        if (e.getGuild().getId().equals("572104809973415943")) {
            if (e.getMember().getRoles().contains(e.getGuild().getRolesByName("Instructor", true).get(0))) {
                return e.getGuild().getRolesByName("Instructor", true).get(0);
            }
            return e.getGuild().getRolesByName("The Architect", true).get(0);
        }

        if (e.getGuild().getId().equals("371062894315569173")) {
            if (e.getMember().getRoles().contains(e.getGuild().getRolesByName("Admins", true).get(0))) {
                return e.getGuild().getRolesByName("Admins", true).get(0);
            }
            return e.getGuild().getRolesByName("PIA", true).get(0);
        } else {
            return null;
        }
    }


    private CompletableFuture<DatabaseEventHolder> loadDatabasePropertiesIntoMemory(final GuildMessageReactionAddEvent event) {
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
