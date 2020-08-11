package com.avairebot.pinewood.waiters;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Issue;

import java.time.Instant;

public class FeedbackWaiters {
    private static AvaIre avaire;
    public static FeedbackWaiters instance;

    public static FeedbackWaiters getInstance() {
        return instance;
    }
    public FeedbackWaiters(AvaIre av) {
        avaire = av;
        instance = this;
    }

    /**
     * <:xeus:737772128127942658> -> S
     * <:PBSTHandbook:690133745805819917>
     */
    public void startFeedbackVersionListener(CommandMessage context, Message p) {
        avaire.getWaiter().waitForEvent(GuildMessageReactionAddEvent.class, msg -> isValidDiscordUserPM(context, msg) && isValidFirstMessageChoiceEmoji(context, msg), v -> {
            context.getMessage().clearReactions().queue();
            if (v.getReactionEmote().getName().equals("xeus")) {
                p.editMessage(context.makeInfo("You have selected a suggestion for the discord bot (Xeus), what is your suggestion?").buildEmbed()).queue(l-> {startPBSTFeedbackWaiter(context, v.getReactionEmote().getName());});
            }
            else if (v.getReactionEmote().getName().equals("PBSTHandbook")) {
                p.editMessage(context.makeInfo("You have selected a suggestion for PBST, what is your suggestion?").buildEmbed()).queue(l-> {startPBSTFeedbackWaiter(context, v.getReactionEmote().getName());});
            }
        });
    }

    private void startPBSTFeedbackWaiter(CommandMessage context, String name) {
        avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, m -> isValidDiscordUserPM(context, m) && characterCheck(context, m, name), m-> {
            if (m.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
                context.makeWarning("Cancelled the suggestion.").queue();
                return;
            }
            if (name.equals("xeus")) {
                try {
                    Issue i = avaire.getGitLabApi().getIssuesApi().createIssue(17658373, "Suggestion from: " +
                            context.getMember().getEffectiveName(),
                        "**Sent from**: " + context.getGuild().getName() + "  \r\n" +
                            "**Sent by**: " + context.getMember().getEffectiveName() + " (" + context.getMember().getUser().getId() + ")  \r\n" +
                            "**Suggestion**:  \r\n" + m.getMessage().getContentRaw() , false, Constants.SUGGESTION_MANAGERS, null, "Suggestion", null, null, null, null);

                    context.makeSuccess("[Your suggestion has been added!](" + i.getWebUrl() + ")").queue();

                } catch (GitLabApiException e) {
                    context.makeError("Something went wrong when sending the issue to gitlab!\n[If you have a good suggestion, but Xeus doesn't want to send it. Post it on the Gitlab Issue's page!](https://gitlab.com/pinewood-builders/discord/xeus/-/issues)").queue();
                    e.printStackTrace();
                }
            }

            else if (name.equals("PBSTHandbook")) {
                TextChannel channel = context.getJDA().getTextChannelById(Constants.FEEDBACK_CHANNEL_ID);
                if (channel != null) {
                    String message = m.getMessage().getContentRaw();

                    channel.sendMessage(context.makeEmbeddedMessage()
                        .setDescription(message)
                        .setTimestamp(Instant.now())
                        .setFooter(context.member.getEffectiveName(), context.member.getUser().getEffectiveAvatarUrl()).buildEmbed()).queue(r -> {
                            r.addReaction("\uD83D\uDC4D").queue();
                            r.addReaction("\uD83D\uDC4E").queue();
                            r.addReaction("✅").queue();
                            r.addReaction("❌").queue();
                            r.addReaction("trash:694314074179240027").queue();
                            r.addReaction("\uD83D\uDD04").queue();

                            context.channel.sendMessage(context.makeSuccess("[Your feedback has been sent in the feedback channel](" + r.getJumpUrl() + "): \n```" + message + "```").buildEmbed()).queue();
                        }
                    );
                }
            }
        });
    }

    private boolean characterCheck(CommandMessage context, GuildMessageReceivedEvent m, String name) {
        if (m.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
            return true;
        }
        if (name.equals("xeus")) {
            if (m.getMessage().getContentRaw().length() < 100) {
                context.makeError("Please make your message longer than 100 characters. This is to prevent spam, and so I have a clear image on what to add to Xeus.").queue();
                return false;
            }
            return true;
        }
        if (name.equals("PBSTHandbook")) {
            if (m.getMessage().getContentRaw().length() < 100) {
                context.makeError("Please make your message longer than 100 characters. This is to prevent spam, and people commenting on other people's feedback in the feedback channel.").queue();
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isValidFirstMessageChoiceEmoji(CommandMessage context, GuildMessageReactionAddEvent msg) {
        if (!context.getMember().equals(msg.getMember())) {return false;}
        return msg.getReactionEmote().getName().equals("xeus") || msg.getReactionEmote().getName().equals("PBSTHandbook");
    }

    private static boolean isValidDiscordUserPM(CommandMessage context, GuildMessageReceivedEvent p) {
        return context.getMember().equals(p.getMember());
    }

    private static boolean isValidDiscordUserPM(CommandMessage context, GuildMessageReactionAddEvent p) {
        return context.getMember().equals(p.getMember());
    }


}
