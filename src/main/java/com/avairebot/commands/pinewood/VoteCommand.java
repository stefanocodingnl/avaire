package com.avairebot.commands.pinewood;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.chat.PlaceholderMessage;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.administration.SoftBanCommand;
import com.avairebot.commands.administration.UnbanCommand;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.controllers.GuildController;
import com.avairebot.database.query.QueryBuilder;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.time.Carbon;
import com.avairebot.utilities.ComparatorUtil;
import com.avairebot.utilities.MentionableUtil;
import com.avairebot.utilities.NumberUtil;
import com.avairebot.utilities.RandomString;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoteCommand extends Command {

    public VoteCommand(AvaIre avaire) {
        super(avaire, true);
    }

    private final Pattern timeRegEx = Pattern.compile("([0-9]+[w|d|h|m|s])");

    @Override
    public String getName() {
        return "Vote Command";
    }

    @Override
    public String getDescription() {
        return "Appel";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
            "`:command` - Vote for a vote."
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList("`:command `");
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("vote");
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
            "throttle:user,1,25"
        );
    }

    @Nonnull
    @Override
    public List<CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.MISCELLANEOUS);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (!context.isGuildMessage()) {
            return runDMArguments(context, args);

        }

        if (args.length < 1) {
            context.makeInfo("Please tell a valid option to work with the vote system:\n" +
                " - ``create``\n" +
                " - ``delete``\n" +
                " - ``responses``\n" +
                " - ``list``\n" +
                " - ``show``\n" +
                " - ``set-vote-validation-channel``").queue();
            return false;
        }
        if (args[0].equalsIgnoreCase("responses")) {
            if (args.length == 1) {
                context.makeError("Please give the the ID of the vote you want to remove! (This removes both the awnsers, votes and the vote itself").queue();
                return false;
            }

        }
        if (args[0].equalsIgnoreCase("create")) {
            //context.makeInfo("TODO: Make the vote creation system.").queue();
            if (args.length == 1) { //!vote create
                context.makeError("Please enter the items you'd like to enter into the vote").setTitle("Need items!").queue();
                return false;
            }

            if (args.length == 2) { //!vote create <votes>
                if (!args[1].contains(",")) {
                    context.makeError("Please have more then 1 option in the vote!").queue();
                    return false;
                }
                String[] strings = args[1].split(","); //!vote create <votes,with,sperators>
                context.makeError("You have given me the following options: \n" +
                    " - " + String.join("\n - ", strings)).setTitle("Missing question! (Add a question)").queue();
                return false;
            }
            if (!args[1].contains(",")) {
                context.makeError("Please have more then 1 option in the vote!").queue();
                return false;
            }

            String[] strings = args[1].split(",");
            String question = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

            String vote_id = new RandomString(5, new Random()).nextString();

            try {
                avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTES_TABLE_NAME).useAsync(true).insert(statement -> {
                    statement.set("question", question);
                    statement.set("total_votes", 0);
                    statement.set("vote_id", vote_id);
                    statement.set("guild_id", context.guild.getId());
                });

                for (String awnser : strings) {
                    avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTABLE_TABLE_NAME).useAsync(true).insert(statement -> {
                        statement.set("vote_id", vote_id);
                        statement.set("item", awnser);
                        statement.set("added_by", context.getMember().getEffectiveName());
                    });
                    context.makeWarning("Added :awnser to :id").set("awnser", awnser).set("id", vote_id).queue();
                }

            } catch (SQLException throwables) {
                context.makeError("Something went wrong when trying to connect to the database :(").queue();
            }

            context.makeSuccess("Succesfully created \"" + vote_id + "\" vote with the options: \n - " + String.join("\n - ", strings) + "\nWith the question: **" + question + "**").queue();
            return false; // TODO: Make the vote creation system.
        }
        if (args[0].equalsIgnoreCase("delete")) {
            if (args.length == 1) {
                context.makeError("Please give the the ID of the vote you want to remove! (This removes both the awnsers, votes and the vote itself").queue();
                return false;
            }
            if (args.length == 2) {
                try {
                    QueryBuilder cvotes = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTES_TABLE_NAME).where("vote_id", args[1]);
                    QueryBuilder canswers = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTABLE_TABLE_NAME).where("vote_id", args[1]);
                    QueryBuilder cvote = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTE_TABLE_NAME).where("vote_id", args[1]);

                    Collection votes = cvotes.get();
                    Collection answers = canswers.get();
                    Collection vote = cvote.get();


                    if (votes.size() < 0 && answers.size() < 0 && vote.size() < 0) {
                        context.makeError("No votes, or answers exist with this ID").queue();
                        return true;
                    }


                    if (votes.size() > 0) {
                        if (votes.get(0).getLong("guild_id") != context.guild.getIdLong()) {
                            context.makeError("This vote is not linked to your guild, so you can't delete it.").queue();
                            return false;
                        }
                        cvotes.delete();
                        context.makeInfo("Deleted all existing votes with the ID: " + args[1]).queue();
                    }
                    if (answers.size() > 0) {
                        canswers.delete();
                        context.makeInfo("Deleted all existing answers with the ID: " + args[1]).queue();
                    }
                    if (vote.size() > 0) {
                        cvote.delete();
                        context.makeInfo("Deleted all existing responses with the ID: " + args[1]).queue();
                    }

                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }

            } else {
                context.makeError("Please only have 1 argument in the command.").queue();
            }

            //context.makeInfo("ODO: Make the vote deletion system.").queue();
            return false; // TODO: Make the vote deletion system.
        }
        if (args[0].equalsIgnoreCase("show")) {
            if (args.length == 1) {
                context.makeError("Please give the the ID of the vote you want to remove! (This removes both the awnsers, votes and the vote itself").queue();
                return false;
            }


            try {
                Collection votes = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTES_TABLE_NAME).where("vote_id", args[1]).get();
                Collection answers = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTABLE_TABLE_NAME).where("vote_id", args[1]).get();
                StringBuilder sb = new StringBuilder();
                sb.setLength(0);

                if (votes.size() > 0) {
                    if (votes.get(0).getLong("guild_id") != context.guild.getIdLong()) {
                        context.makeError(":warning:  This vote is not linked to your guild, so you can't delete it.").queue();
                        return false;
                    }

                    if (answers.size() < 1) {
                        context.makeError(":warning: This vote does not seem to have any votable answers, please check the database!").queue();
                        return false;
                    }

                    answers.forEach(l -> sb.append("\n - ``").append(l.getString("item")).append("``"));

                    context.makeEmbeddedMessage(new Color(0, 158, 224))
                        .addField("Question:", votes.get(0).getString("question"), true)
                        .addField("Possible answers:", sb.toString(), true)
                        .setFooter("!vote " + args[1] + " <item> (reason) {In DM's}")
                        .setTimestamp(Instant.now()).queue();
                }

            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if (args[0].equalsIgnoreCase("list")) {
            if (args.length > 1) {
                context.makeError("This (sub-)command doesn't need more arguments!").queue();
                return false;
            }


            try {
                Collection votes = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTES_TABLE_NAME).get();
                StringBuilder sb = new StringBuilder();
                sb.setLength(0);

                if (votes.size() > 0) {
                    if (votes.get(0).getLong("guild_id") != context.guild.getIdLong()) {
                        context.makeError(":warning:  This vote is not linked to your guild, so you can't delete it.").queue();
                        return false;
                    }
                    votes.forEach(dataRow -> {
                        sb.append("\n - **").append(dataRow.getString("question")).append("** (").append(dataRow.getString("vote_id")).append(")");
                    });

                    context.makeEmbeddedMessage(new Color(0, 158, 224))
                        .addField("Existing Votes (" + votes.size() + "): ", sb.toString(), true)
                        .queue();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if (args[0].equalsIgnoreCase("set-vote-validation-channel")) {
            return runVoteUpdateChannelChannelCommand(context, args);
        }
        return false;
    }

    private boolean runDMArguments(CommandMessage context, String[] args) {
        if (args.length < 1) {
            context.makeError("Hello there! It seems like you tried to run this command directly from the DM's. However, this will only work if you're actually voting for something! Please check if you have entered the correct ID!").queue();
            return false;
        }
        try {
            Collection votes = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTES_TABLE_NAME).where("vote_id", args[0]).get();
            Collection answers = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTABLE_TABLE_NAME).where("vote_id", args[0]).get();
            QueryBuilder vote = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTE_TABLE_NAME).where("voter_user_id", context.getAuthor().getId());

            StringBuilder sb = new StringBuilder();
            sb.setLength(0);

            if (votes.size() == 1) {
                if (answers.size() < 1) {
                    context.makeError(":warning: This vote does not seem to have any votable answers, please check the database!").queue();
                    return false;
                }
                Guild gc = avaire.getShardManager().getGuildById(votes.get(0).getLong("guild_id"));
                if (args.length < 2) {
                    answers.forEach(l -> sb.append("\n - ``").append(l.getString("item")).append("``"));

                    context.makeEmbeddedMessage(new Color(0, 158, 224))
                        .addField("Question:", votes.get(0).getString("question"), true)
                        .addField("Possible answers:", sb.toString(), true)
                        .addField("Connected guild: ", gc != null ? gc.getName() + " - " + gc.getId() : "Guild not found", false).queue();
                }
                if (args.length >= 2) {
                    if (gc == null) {
                        context.makeError("Sorry, but something went wrong in fetching the the guild.").queue();
                        return false;
                    }

                    if (!answers.contains(args[1])) {
                        context.makeError("This is not a votable option!").queue();
                        return false;
                    }
                    GuildTransformer gt = GuildController.fetchGuild(avaire, gc);
                    if (gt.getVoteValidationChannel() == null) {
                        QueryBuilder ivote = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTE_TABLE_NAME);

                        if (vote.andWhere("vote_id", args[0]).get().size() > 0) {
                            context.makeInfo(":warning: You've already voted for ``" + vote.andWhere("vote_id", args[0]).get().get(0).get("voted_for") + "``. Your vote will be overridden!").queue();
                            ivote.useAsync(true)
                                .where("vote_id", args[0])
                                .andWhere("voter_user_id", context.getAuthor().getId())
                                .update(statement -> {
                                    statement.set("voted_for", args[1]);
                                });
                            context.makeSuccess("Changed your vote to: ``" + args[1] + "``").queue();
                        } else {
                            ivote.useAsync(true).insert(statement -> {
                                statement.set("vote_id", args[0]);
                                statement.set("voted_for", args[1]);
                                statement.set("voter_user_id", context.getAuthor().getId());
                                statement.set("accepted", true);
                            });
                            context.makeSuccess("You've voted for: ``" + args[1] + "``").queue();
                        }
                    } else {
                        if (args.length == 2) {
                            context.makeError("Please give a reason why you would like to vote for this person (Retype the command)").queue();
                            return true;
                        }

                        if (args.length < 22) {
                            context.makeError("Please explain in more then 20 words why you'd like to vote for this item! This is to verify valid votes.").queue();
                            return false;
                        }



                        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                        avaire.getShardManager().getTextChannelById(gt.getVoteValidationChannel())
                            .sendMessage(context.makeEmbeddedMessage(new Color(54, 57, 63)).setAuthor(context.getAuthor().getName() + "#" + context.getAuthor().getDiscriminator() + " - " + args[1], null, context.getAuthor().getEffectiveAvatarUrl())
                                .setDescription(description).setFooter(args[0]).setTimestamp(Instant.now()).buildEmbed()).queue(message -> {
                            try {
                                QueryBuilder ivote = avaire.getDatabase().newQueryBuilder(Constants.MOTS_VOTE_TABLE_NAME);

                                if (vote.andWhere("vote_id", args[0]).get().size() > 0) {
                                    context.makeInfo(":warning: You've already voted for ``" + vote.get().get(0).getString("voted_for") + "``. Your vote will be overridden!").queue();
                                    avaire.getShardManager().getTextChannelById(gt.getVoteValidationChannel()).retrieveMessageById(vote.get().get(0).getString("vote_message_id"))
                                        .queue(m -> {m.delete().queue(t -> {context.makeInfo("Deleted old vote!").queue();});});

                                    ivote.useAsync(true)
                                        .where("vote_id", args[0])
                                        .andWhere("voter_user_id", context.getAuthor().getId())
                                        .update(statement -> {
                                            statement.set("voted_for", args[1]);
                                            statement.set("accepted", false);
                                            statement.set("description", description, true);
                                            statement.set("vote_message_id", message.getId());
                                        });
                                    context.makeSuccess("Changed your vote to ``" + args[1] + "`` with the description: ```"  + description + "```").queue();
                                } else {
                                    ivote.useAsync(true).insert(statement -> {
                                        statement.set("vote_id", args[0]);
                                        statement.set("voted_for", args[1]);
                                        statement.set("voter_user_id", context.getAuthor().getId());
                                        statement.set("description", description, true);
                                        statement.set("vote_message_id", message.getId());
                                        statement.set("accepted", false);
                                    });
                                    context.makeSuccess("You've voted for ``" + args[1] + "`` with the description: ```" + description + "```").queue();
                                }

                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                            }
                            message.addReaction("✅").queue();
                            message.addReaction("❌").queue();
                        });
                    }

                }
            } else if (votes.size() < 1) {
                context.makeError("This vote does not exist. Try again.").queue();
            }
            return true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        // TODO: Check if there is a vote on the first argument, if not. Continue down.


        return false;
    }

    private Boolean runVoteUpdateChannelChannelCommand(CommandMessage context, String[] args) {
        GuildTransformer transformer = context.getGuildTransformer();
        if (transformer == null) {
            return sendErrorMessage(context, "The guildtransformer can't be found :(");
        }

        if (args.length == 1) {
            sendVoteValidationChannel(context, transformer).queue();
            return true;
        }

        if (ComparatorUtil.isFuzzyFalse(args[1])) {
            return disableVoteValidation(context, transformer);
        }

        GuildChannel channel = MentionableUtil.getChannel(context.getMessage(), args, 1);
        if (!(channel instanceof TextChannel)) {
            return sendErrorMessage(context, "You must mentions a channel, or call out it's exact name!");
        }

        if (!((TextChannel) channel).canTalk() || !context.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
            return sendErrorMessage(context, context.i18n("\"I can't send embedded messages in the specified channel, please change my permission level for the {0} channel if you want to use it as a \"vote validation\" channel.", ((TextChannel) channel).getAsMention()));
        }

        try {
            updateVoteValidation(transformer, context, channel.getId());

            context.makeSuccess("The vote validation channel is set to :channel this guild.")
                .set("channel", ((TextChannel) channel).getAsMention())
                .queue();
        } catch (SQLException ex) {
            AvaIre.getLogger().error(ex.getMessage(), ex);
        }
        return true;
    }

    private boolean disableVoteValidation(CommandMessage context, GuildTransformer transformer) {
        try {
            updateVoteValidation(transformer, context, null);

            context.makeSuccess("The vote validation channel has been disabled on this guild.")
                .queue();
        } catch (SQLException ex) {
            AvaIre.getLogger().error(ex.getMessage(), ex);
        }

        return true;
    }

    private PlaceholderMessage sendVoteValidationChannel(CommandMessage context, GuildTransformer transformer) {
        if (transformer.getVoteValidationChannel() == null) {
            return context.makeWarning("The vote validation channel is disabled on this guild.");
        }

        TextChannel modlogChannel = context.getGuild().getTextChannelById(transformer.getVoteValidationChannel());
        if (modlogChannel == null) {
            try {
                updateVoteValidation(transformer, context, null);
            } catch (SQLException ex) {
                AvaIre.getLogger().error(ex.getMessage(), ex);
            }
            return context.makeInfo("The vote validation channel is disabled on this guild.");
        }

        return context.makeSuccess("The vote validation channel is set to :channel this guild.")
            .set("channel", modlogChannel.getAsMention());
    }

    private void updateVoteValidation(GuildTransformer transformer, CommandMessage context, String value) throws
        SQLException {
        transformer.setVoteValidationChannel(value);
        avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
            .where("id", context.getGuild().getId())
            .update(statement -> statement.set("vote_validation_channel", value));
    }

}
