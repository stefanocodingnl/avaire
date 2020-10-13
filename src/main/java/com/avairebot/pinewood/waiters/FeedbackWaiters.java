package com.avairebot.pinewood.waiters;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Issue;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

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
     * <:xeus:737772128127942658>
     * <:PBSTHandbook:690133745805819917>
     * 🛢️ -> Oil Platform
     */
    public void startFeedbackVersionListener(CommandMessage context, Message p) {
        avaire.getWaiter().waitForEvent(GuildMessageReactionAddEvent.class, msg -> isValidDiscordUserPM(context, msg) && isValidFirstMessageChoiceEmoji(context, msg), v -> {
            p.clearReactions().queue();
            if (v.getReactionEmote().getName().equalsIgnoreCase("xeus")) {
                p.editMessage(context.makeInfo("You have selected a suggestion for the discord bot (Xeus), what is your suggestion?").buildEmbed()).queue(l -> {
                    startFeedbackWaiter(context, v.getReactionEmote().getName());
                });
            } else if (v.getReactionEmote().getName().equalsIgnoreCase("PBSTHandbook")) {
                p.editMessage(context.makeInfo("You have selected a suggestion for PBST, what is your suggestion?").buildEmbed()).queue(l -> {
                    startFeedbackWaiter(context, v.getReactionEmote().getName());
                });
            } else if (v.getReactionEmote().getName().equalsIgnoreCase("PB")) {
                p.editMessage(context.makeInfo("You have selected a suggestion for PB, what is your suggestion?").buildEmbed()).queue(l -> {
                    startFeedbackWaiter(context, v.getReactionEmote().getName());
                });
            }
            else if (v.getReactionEmote().getName().equalsIgnoreCase("\uD83D\uDEE2")) {
                p.editMessage(context.makeInfo("You have selected a suggestion for PBOP, what is your suggestion?").buildEmbed()).queue(l -> {
                    startFeedbackWaiter(context, v.getReactionEmote().getName());
                });
            }
            else if (v.getReactionEmote().getName().equalsIgnoreCase("PETHandbook")) {
                p.editMessage(context.makeInfo("You have selected a suggestion for PET, what is your suggestion?").buildEmbed()).queue(l -> {
                    startFeedbackWaiter(context, v.getReactionEmote().getName());
                });
            }
        });
    }

    private void startFeedbackWaiter(CommandMessage context, String name) {
        avaire.getWaiter().waitForEvent(GuildMessageReceivedEvent.class, m -> isValidDiscordUserPM(context, m) && characterCheck(context, m, name), m -> {
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
                                    "**Suggestion**:  \r\n" + m.getMessage().getContentRaw(), false, Constants.SUGGESTION_MANAGERS, null, "Suggestion", null, null, null, null);

                    context.makeSuccess("[Your suggestion has been added!](" + i.getWebUrl() + ")").queue();

                } catch (GitLabApiException e) {
                    context.makeError("Something went wrong when sending the issue to gitlab!\n[If you have a good suggestion, but Xeus doesn't want to send it. Post it on the Gitlab Issue's page!](https://gitlab.com/pinewood-builders/discord/xeus/-/issues)").queue();
                    e.printStackTrace();
                }
            } else if (name.equals("PBSTHandbook")) {
                TextChannel channel = avaire.getShardManager().getTextChannelById(Constants.FEEDBACK_CHANNEL_ID);
                sendFeedback(context, m, channel);
            } else if (name.equals("PB")) {
                TextChannel channel = avaire.getShardManager().getTextChannelById(Constants.PB_FEEDBACK_CHANNEL_ID);
                sendFeedback(context, m, channel);
            }
            else if (name.equals("\uD83D\uDEE2")) {
                TextChannel channel = avaire.getShardManager().getTextChannelById(Constants.PBOP_FEEDBACK_CHANNEL_ID);
                sendFeedback(context, m, channel);
            }
            else if (name.equals("PETHandbook")) {
                TextChannel channel = avaire.getShardManager().getTextChannelById(Constants.PET_FEEDBACK_CHANNEL_ID);
                sendFeedback(context, m, channel);
            }
        });
    }

    private void sendFeedback(CommandMessage context, GuildMessageReceivedEvent m, TextChannel channel) {
        if (channel != null) {
            String message = m.getMessage().getContentRaw();

            channel.sendMessage(context.makeEmbeddedMessage()
                    .setDescription(message)
                    .setTimestamp(Instant.now())
                    .setFooter(context.member.getEffectiveName(), context.member.getUser().getEffectiveAvatarUrl()).buildEmbed()).queue(r -> {
                createReactions(r);

                context.channel.sendMessage(context.makeSuccess("[Your feedback has been sent in the feedback channel](" + r.getJumpUrl() + "): \n```" + message + "```").buildEmbed()).queue();
                    }
            );
        }
    }

    private boolean characterCheck(CommandMessage context, GuildMessageReceivedEvent m, String name) {
        if (m.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
            return true;
        }
        if (name.equalsIgnoreCase("xeus")) {
            if (m.getMessage().getContentRaw().length() < 100) {
                context.makeError("Please make your message longer than 100 characters. This is to prevent spam, and so I have a clear image on what to add to Xeus.").queue();
                return false;
            }
            return true;
        }
        if (name.equalsIgnoreCase("PBSTHandbook")) {
            if (m.getMessage().getContentRaw().length() < 100) {
                context.makeError("Please make your message longer than 100 characters. This is to prevent spam, and people commenting on other people's feedback in the feedback channel.").queue();
                return false;
            }
            return true;
        }
        if (name.equalsIgnoreCase("PB")) {
            if (m.getMessage().getContentRaw().length() < 100) {
                context.makeError("Please make your message longer than 100 characters. This is to prevent spam, and people commenting on other people's feedback in the feedback channel.").queue();
                return false;
            }
            return true;
        }
        if (name.equalsIgnoreCase("\uD83D\uDEE2")) {
            if (m.getMessage().getContentRaw().length() < 100) {
                context.makeError("Please make your message longer than 100 characters. This is to prevent spam, and people commenting on other people's feedback in the feedback channel.").queue();
                return false;
            }
            return true;
        }
        if (name.equalsIgnoreCase("PETHandbook")) {
            if (m.getMessage().getContentRaw().length() < 100) {
                context.makeError("Please make your message longer than 100 characters. This is to prevent spam, and people commenting on other people's feedback in the feedback channel.").queue();
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isValidFirstMessageChoiceEmoji(CommandMessage context, GuildMessageReactionAddEvent msg) {
        if (!context.getMember().equals(msg.getMember())) {
            return false;
        }
        return msg.getReactionEmote().getName().equalsIgnoreCase("xeus") ||
                msg.getReactionEmote().getName().equalsIgnoreCase("PBSTHandbook") ||
                msg.getReactionEmote().getName().equalsIgnoreCase("PB") ||
                msg.getReactionEmote().getName().equalsIgnoreCase("\uD83D\uDEE2") ||
                msg.getReactionEmote().getName().equalsIgnoreCase("PETHandbook");
    }

    private static boolean isValidDiscordUserPM(CommandMessage context, GuildMessageReceivedEvent p) {
        return context.getMember().equals(p.getMember()) && p.getChannel().equals(context.getChannel());
    }

    private static boolean isValidDiscordUserPM(CommandMessage context, GuildMessageReactionAddEvent p) {
        return context.getMember().equals(p.getMember());
    }


/*    private static boolean checkEvidenceAcceptance(CommandMessage context, GuildMessageReceivedEvent pm) {
        String message = pm.getMessage().getContentRaw();
        if (!(message.startsWith("https://youtu.be") ||
                message.startsWith("http://youtu.be") ||
                message.startsWith("https://www.youtube.com/") ||
                message.startsWith("http://www.youtube.com/") ||
                message.startsWith("https://youtube.com/") ||
                message.startsWith("http://youtube.com/") ||
                message.startsWith("https://streamable.com/") ||
                message.contains("cdn.discordapp.com") ||
                message.contains("media.discordapp.com") ||
                message.contains("gyazo.com") ||
                message.contains("prntscr.com") || message.contains("imgur.com"))) {
            pm.getChannel().sendMessage(context.makeError("Sorry, but we are only accepting [YouTube links](https://www.youtube.com/upload), [Gyazo Links](https://gyazo.com), [LightShot Links](https://app.prntscr.com/), [Discord Image Links](https://cdn.discordapp.com/attachments/689520756891189371/733599719351123998/unknown.png) or [Imgur links](https://imgur.com/upload) as evidence. Try again").buildEmbed()).queue();
            return false;
        }
        return true;
    }*/



    public static void createReactions(Message r) {
        r.addReaction("\uD83D\uDC4D").queue();
        r.addReaction("\uD83D\uDC4E").queue();
        r.addReaction("✅").queue();
        r.addReaction("❌").queue();
        r.addReaction("trash:694314074179240027").queue();
        r.addReaction("\uD83D\uDCAC").queue();
        r.addReaction("\uD83D\uDD04").queue();
    }


}


