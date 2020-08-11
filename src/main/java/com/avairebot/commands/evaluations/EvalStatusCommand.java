package com.avairebot.commands.evaluations;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.collection.DataRow;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.avairebot.utils.JsonReader.readArrayJsonFromUrl;
import static com.avairebot.utils.JsonReader.readJsonFromUrl;

public class EvalStatusCommand extends Command {
    public EvalStatusCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Evaluation Status Command";
    }

    @Override
    public String getDescription() {
        return "Command to see your evaluation status.";
    }

    @Override
    public List <String> getUsageInstructions() {
        return Collections.singletonList(
            "`:command` - Get your eval status"
        );
    }

    @Override
    public List <String> getExampleUsage() {
        return Collections.singletonList(
            "`:command` - Get your eval status"
        );
    }


    @Override
    public List <String> getTriggers() {
        return Arrays.asList("evalstatus", "es");
    }

    @Nonnull
    @Override
    public List <CommandGroup> getGroups() {
        return Collections.singletonList(
            CommandGroups.EVALUATIONS
        );
    }

    @Override
    public List <String> getMiddleware() {
        return Arrays.asList(
            "throttle:user,1,3",
            "isOfficialPinewoodGuild"
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {

        String name = context.member.getEffectiveName();
        try {
        if (!(isValidRobloxUser(name) && isValidUserInPBSTGroup(getRobloxId(name)))){
            context.makeError("Your discord name is not a valid roblox name and/or is not in the PBST Group...\nPlease do ``!verify`` to re-verify yourself. (``v!verify`` if the prefix isn't changed").queue();
            return true;
        }

            Collection collection = avaire.getDatabase().newQueryBuilder(Constants.EVALS_DATABASE_TABLE_NAME).where("roblox_id", getRobloxId(name)).get();

            if (args.length < 2) {

                if (collection.size() < 1) {
                    context.makeEmbeddedMessage(new Color(21, 34, 255)).setDescription("You have not yet passed anything:\n\n" +
                        "**Passed Quiz**: <:no:694270050257076304>\n" +
                        "**Passed Patrol**: <:no:694270050257076304>\n\n" +
                        "**Last Evaluator**: ``No evaluator found``").queue();
                    return true;
                }

                if (collection.size() > 2) {
                    context.makeError("Something is wrong in the database, there are records with multiple usernames, but the same user id. Please check if this is correct.").queue();
                    return false;
                }
                if (collection.size() == 1) {
                    DataRow row = collection.get(0);
                    String passed_quiz = row.getBoolean("passed_quiz") ? "<:yes:694268114803621908>" : "<:no:694270050257076304>";
                    String passed_patrol = row.getBoolean("passed_patrol") ? "<:yes:694268114803621908>" : "<:no:694270050257076304>";
                    String evaluator = row.getString("evaluator") != null ? row.getString("evaluator") : "Unkown Evaluator";

                    context.makeEmbeddedMessage(new Color(21, 34, 255)).setDescription("This user has this information in the database:\n\n" +
                        "**Passed Quiz**: " + passed_quiz + "\n" +
                        "**Passed Patrol**: " + passed_patrol + "\n"
                        + (row.getBoolean("passed_quiz") && row.getBoolean("passed_patrol") ? "**You have passed both!**\n\n" : "\n") +
                        "**Last Evaluator**: " + evaluator).queue();
                    return true;
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
            return false;

    }

    public boolean isValidUserInPBSTGroup(int id) {

        try {
            JSONArray json = readArrayJsonFromUrl("https://api.roblox.com/users/" + id +"/groups");
            for (int i = 0; i < json.length(); i++) {
                JSONObject object = json.getJSONObject(i);
                if (object.getInt("Id") != 645836) continue;
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public int getRobloxId(String un) {

        try {
            JSONObject json = readJsonFromUrl("http://api.roblox.com/users/get-by-username?username=" + un);
            return json.getInt("Id");
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isValidRobloxUser(String un) {

        try {
            JSONObject json = readJsonFromUrl("http://api.roblox.com/users/get-by-username?username=" + un);
            String username = json.getString("Username");

            return username != null;
        } catch (Exception e) {
            return false;
        }
    }

}
