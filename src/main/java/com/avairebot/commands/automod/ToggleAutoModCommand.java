package com.avairebot.commands.automod;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.administration.ListAliasesCommand;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.utilities.ComparatorUtil;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ToggleAutoModCommand extends Command {
    public ToggleAutoModCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Toggle AutoMod Command";
    }

    @Override
    public String getDescription() {
        return "Enable or disable the filter in a server.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command <true/false>` - Enable or disable the filter in the current server"
        );
    }

    @Override
    public List <String> getExampleUsage(@Nullable Message message) {
        return Collections.singletonList("`:command true` - Enable the filter in the current server");
    }

    @Override
    public List <Class <? extends Command>> getRelations() {
        return Collections.singletonList(ListAliasesCommand.class);
    }

    @Override
    public List <String> getTriggers() {
        return Arrays.asList("filter", "toggleautomod", "tam");
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "throttle:user,2,5",
            "isManagerOrHigher"
        );
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(CommandGroups.COMMAND_CUSTOMIZATION);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(context, "You didn't give any arguments.");
        }

        GuildTransformer transformer = context.getGuildTransformer();
        if (transformer == null) {
            return sendErrorMessage(context, "Unable to load the server settings.");
        }


        if (args[0].equals("set-channel")) {
            if (!(context.getMentionedChannels().size() > 0)) {
                context.makeError("You need to mention a channel to add it as a filter log channel.").queue();
                return false;
            }
            context.getGuildTransformer().setFilterLog(context.getMentionedChannels().get(0).getId());
            try {
                avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME).where("id", context.guild.getId()).update(statement -> {
                    statement.set("filter_log", context.getGuildTransformer().getFilterLog());
                });
            } catch (SQLException throwables) {
                context.makeError("Something went wrong when setting the log channel.").queue();
            }
            context.makeSuccess("The filter log channel has successfully been set to: " + context.getMentionedChannels().get(0).getAsMention()).queue();
            return true;
        }
        switch (ComparatorUtil.getFuzzyType(args[0])) {
            case FALSE:
                setStatus(false, context);
                break;

            case TRUE:
                setStatus(true, context);
                break;

            case UNKNOWN:
                return sendErrorMessage(context, "Please enter ``true`` or ``false`` to **enable** or **disable** the filter.");
        }
        return true;
    }

    private void setStatus(boolean b, CommandMessage context) {
        try {
            avaire.getDatabase().newQueryBuilder(Constants.GUILD_TABLE_NAME)
                .useAsync(true)
                .where("id", context.getGuild().getId())
                .update(statement -> statement.set("filter", b));
            context.getGuildTransformer().setFilter(b);
            context.makeSuccess("Filter has been set to: **``" + b + "``**").queue();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
