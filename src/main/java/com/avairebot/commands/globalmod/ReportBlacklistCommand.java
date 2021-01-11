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

package com.avairebot.commands.globalmod;

import com.avairebot.AvaIre;
import com.avairebot.blacklist.reports.Scope;
import com.avairebot.chat.SimplePaginator;
import com.avairebot.commands.CommandMessage;
import com.avairebot.commands.CommandPriority;
import com.avairebot.contracts.commands.Command;
import com.avairebot.language.I18n;
import com.avairebot.utilities.NumberUtil;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReportBlacklistCommand extends Command {

    public ReportBlacklistCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Report Blacklist Command";
    }

    @Override
    public String getDescription() {
        return "Add, Remove, and list users and servers on the Report blacklist.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Arrays.asList(
            "`:command list` - Lists users and servers on the Report blacklist",
            "`:command remove <id>` - Removes the entry with the given ID from the Report blacklist",
            "`:command add <type> <id> <reason>` - Add the type with the given ID to the Report blacklist"
        );
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "isAdminOrHigher"
        );
    }

    @Override
    public List <String> getExampleUsage() {
        return Arrays.asList(
            "`:command add U 321 Doing stuff` - Blacklists the user with an ID of 321 for \"Doing stuff\""
        );
    }

    @Override
    public List <String> getTriggers() {
        return Collections.singletonList("report-blacklist");
    }

    @Override
    public CommandPriority getCommandPriority() {
        return CommandPriority.NORMAL;
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(context, "Missing parameter, a valid `action` must be given!");
        }

        switch (args[0].toLowerCase()) {
            case "list":
                return listBlacklist(context, Arrays.copyOfRange(args, 1, args.length));

            case "add":
                return addEntryToBlacklist(context, Arrays.copyOfRange(args, 1, args.length));

            case "remove":
                return removeEntryFromBlacklist(context, Arrays.copyOfRange(args, 1, args.length));

            default:
                return sendErrorMessage(context, "Invalid `action` given, a valid `action` must be given!");
        }
    }

    private boolean listBlacklist(CommandMessage context, String[] args) {
        List<String> records = new ArrayList<>();

        avaire.getReportBlacklist().getBlacklistEntities().forEach(entity -> {
            records.add(I18n.format("{0} **{1}** `{2}` - `{3}`\n ► _\"{4}\"_",
                entity.getScope().getId() == 0 ? "\uD83E\uDD26" : "\uD83C\uDFEC",
                entity.getScope().getName(),
                entity.getId(),
                entity.getGuildId(),
                entity.getReason() == null ? "No reason was given" : entity.getReason()
            ));
        });

        SimplePaginator<String> paginator = new SimplePaginator<>(records, 10, 1);
        if (args.length > 0) {
            paginator.setCurrentPage(NumberUtil.parseInt(args[0], 1));
        }

        List<String> messages = new ArrayList<>();
        paginator.forEach((index, key, val) -> messages.add(val));

        context.makeInfo(String.join("\n", messages) + "\n\n" + paginator.generateFooter(
            context.getGuild(),
            generateCommandTrigger(context.getMessage()) + " list")
        )
            .setTitle("Blacklist Page #" + paginator.getCurrentPage())
            .queue();

        return false;
    }

    private boolean removeEntryFromBlacklist(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(context, "Missing arguments, the `id` argument is required!");
        }

        long id;
        long guildId = context.guild.getIdLong();
        try {
            id = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            return sendErrorMessage(context, "Invalid ID given, the ID must be a valid number value!");
        }

        User user;
        try {
            user = avaire.getShardManager().getUserById(id);
            if (user == null) {
                return sendErrorMessage(context, "User does not exists, please try again.");
            }
        } catch (NumberFormatException e) {
            return sendErrorMessage(context, "Invalid ID given, the ID must be a valid number value!");
        }

        if (!avaire.getReportBlacklist().isBlacklisted(user, guildId)) {
            return sendErrorMessage(context, "There are no records in the blacklist with an ID of `{0}`", "" + id);
        }

        avaire.getReportBlacklist().remove(id, guildId);

        context.makeSuccess("The Blacklist record with an ID of **:id** has been removed from the blacklist")
            .set("id", id)
            .queue();

        return true;
    }

    private boolean addEntryToBlacklist(CommandMessage context, String[] args) {
        if (args.length < 2) {
            return sendErrorMessage(context, "Missing arguments, `type` and `id` argument required!");
        }

        Scope scope = Scope.parse(args[0]);
        if (scope == null) {
            return sendErrorMessage(context, "Invalid type given, the type must be a valid blacklist scope!\nValid types are `M` for users.");
        }

        long id;
        try {
            id = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            return sendErrorMessage(context, "Invalid ID given, the ID must be a valid number value!");
        }

        String reason = null;
        args = Arrays.copyOfRange(args, 2, args.length);
        if (args.length > 0) {
            reason = String.join(" ", args);
        }

        avaire.getReportBlacklist().addIdToBlacklist(scope, id, reason, context.guild.getIdLong());

        context.makeSuccess("The **:type** with an ID of **:id** has been added to the report blacklist of :guild!")
            .set("type", scope.name().toLowerCase())
            .set("id", id)
            .set("guild", context.getGuild().getName())
            .queue();

        return true;
    }
}
