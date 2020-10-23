package com.avairebot.commands.globalmod;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.database.transformers.GuildTransformer;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AddPIAModWildcardCommand extends Command {

    public AddPIAModWildcardCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "AutoMod Wildcard Command";
    }

    @Override
    public String getDescription() {
        return "Add or remove global wildcard words for the filter.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Arrays.asList(
            "`:command <add/remove> <words/word>` - Add or remove a word from the global wildcard word list.",
            "`:command <list>` - See all the words in the global wildcard filter."
        );
    }

    @Override
    public List<String> getExampleUsage(@Nullable Message message) {
        return Arrays.asList(
            "`:command add diddleshot stole` - Add's the words ``diddleshot stole`` to the global wildcard filter.",
            "`:command remove diddleshot stole` - Removes the words ``diddleshot stole`` from the global wildcard filter.");
    }

    @Override
    public List<Class<? extends Command>> getRelations() {
        return Collections.singletonList(AddPIAModExactCommand.class);
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("pwcf", "pia-wcf");
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "isValidPIAMember"
        );
    }

    @Nonnull
    @Override
    public List<CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.MODERATION);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(context, "You didn't give any arguments.");
        }

        GuildTransformer transformer = context.getGuildTransformer();
        if (transformer == null) {
            return sendErrorMessage(context, "Unable to load the global server settings.");
        }

        String words = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
        if (args[0].equalsIgnoreCase("list")) {
            return getGlobalAutoModExactList(context, transformer);
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (args.length == 1) {
                return sendErrorMessage(context, "You didn't give any words to remove from the global filter.");
            }
            return removeAutoModExact(context, transformer, words);
        }
        if (args[0].equalsIgnoreCase("add")) {
            if (args.length == 1) {
                return sendErrorMessage(context, "You didn't give any words to add to the global filter.");
            }
            transformer.getPIAWordsWildcard().add(words);
            try {
                updateGuildAutoModExact(context, transformer);

                context.makeSuccess("Successfully added: ``" + words + "``")
                    .queue();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        if (args[0].equalsIgnoreCase("add-comma")) {
            if (args.length == 1) {
                return sendErrorMessage(context, "You didn't give any words to add to the global filter.");
            }
            transformer.getPIAWordsWildcard().add(words);
            try {
                updateGuildAutoModExact(context, transformer);

                context.makeSuccess("Successfully added: ``" + words + "``")
                    .queue();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return sendErrorMessage(context, "Invalid argument.");
        }
    }

    private boolean removeAutoModExact(CommandMessage context, GuildTransformer transformer, String args) {
        if (!transformer.getPIAWordsWildcard().contains(args)) {
            return sendErrorMessage(context, "This word does not exist in the wildcard list");
        }

        transformer.getPIAWordsWildcard().remove(args);

        try {
            updateGuildAutoModExact(context, transformer);

            context.makeSuccess("Deleted: ``" + args +"``")
                .queue();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean getGlobalAutoModExactList(CommandMessage context, GuildTransformer transformer) {
        context.makeSuccess("This the list of the current filtered wildcard words: \n```" + transformer.getPIAWordsWildcard() + "```").queue();
        return false;
    }

    private void updateGuildAutoModExact(CommandMessage message, GuildTransformer transformer) throws SQLException {
        for (String id : Constants.guilds) {
            avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
                .where("id", id)
                .update(statement -> statement.set("piaf_wildcard", AvaIre.gson.toJson(transformer.getPIAWordsWildcard()), true));
        }

    }
}
