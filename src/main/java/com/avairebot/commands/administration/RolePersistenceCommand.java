package com.avairebot.commands.administration;

import com.avairebot.AvaIre;
import com.avairebot.Constants;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandGroup;
import com.avairebot.contracts.commands.CommandGroups;
import com.avairebot.database.collection.Collection;
import com.avairebot.database.query.QueryBuilder;
import com.avairebot.utilities.MentionableUtil;
import com.avairebot.utilities.NumberUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RolePersistenceCommand extends Command {

    public RolePersistenceCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Role Persistence Command";
    }

    @Override
    public String getDescription() {
        return "Manage all persistent roles.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
            "`:command <role> <user(ID)> ` - Toggle someone on a persistent role.",
            "`:command l/list` - Shows all roles persistent on the guild you executed the command in.",
            "`:command l/list <guild_id>` - Shows all persistent roles on the guild you added the argument to. **(PIA Required)**",
            "`:command r/remove <id>` - Remove a persistent role by it's ID (Use `:command list` to check the ID's)"
        );
    }

    @Override
    public List<String> getExampleUsage(@Nullable Message message) {
        return Arrays.asList(
            "`:command l/list` - Shows all persistent roles in the guild you ran the command in.",
            "`:command 438142943648415745 251818929226383361` - Force the `PIA` role on `CombatSwift`.",
            "`:command r/remove 22` - Remove persistent role 22.");
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("rolepersist", "role-persist", "rp");
    }

    @Override
    public List<String> getMiddleware() {
        return Arrays.asList(
            "isOfficialPinewoodGuild",
            "isManagerOrHigher"
        );
    }

    @Nonnull
    @Override
    public List<CommandGroup> getGroups() {
        return Collections.singletonList(
            CommandGroups.ROLE_ASSIGNMENTS
        );
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length < 1) {
            return sendErrorMessage(context, "Please use the correct prefix to use this command, the possibilities will be shown below.");
        }
        try {
            switch (args[0]) {
                case "l":
                case "list":
                    return runListArgument(context, args);
                case "r":
                case "remove":
                    return removeArgument(context, args);
                default:
                    return runToggleUserToPersistantRolesArgument(context, args);
            }
        } catch (SQLException throwables) {
            return sendErrorMessage(context, "An error has occurred when checking the database for errors. Please contact the developer of Xeus or try again.");
        }
    }

    private boolean removeArgument(CommandMessage context, String[] args) {
        if (args.length < 2) {
            context.makeWarning("You must give an ID to remove from the list. Check the ``" + generateCommandPrefix(context.message) + "rp list`` command.").queue();
            return false;
        }
        try {
            QueryBuilder c = avaire.getDatabase().newQueryBuilder(Constants.ROLE_PERSISTENCE_TABLE_NAME)
                .where("id", args[1]);
            Collection col = c.get();
            if (col.isEmpty()) {
                context.makeError("This ID does not exist.").queue();
                return false;
            } else {
                if (col.get(0).getLong("guild_id") == context.guild.getIdLong()) {
                    c.useAsync(true).delete();
                    context.makeSuccess("Removed ``" + args[1] + "`` from the database.\n\n" +
                        "``User:`` " + col.get(0).getLong("user_id") + "\n" +
                        "``Role:`` " + col.get(0).getLong("role_id")).queue();
                } else {
                    context.makeError("This persistent role is not linked to your guild, so you can't remove it!").queue();
                }
                return true;
            }
        } catch (SQLException throwables) {
            context.makeError("Something went wrong when checking the database when removing the role by ID, check with the developer.").queue();
            return false;
        }

    }

    private boolean runToggleUserToPersistantRolesArgument(CommandMessage context, String[] args) throws SQLException {
        User u = MentionableUtil.getUser(context, args, 1);
        Member m = u != null ? context.guild.getMember(u) : context.guild.getMemberById(args[1]);
        Role r = !NumberUtil.isNumeric(args[0]) ? MentionableUtil.getRole(context.getMessage(), new String[]{args[0]}) : context.guild.getRoleById(args[0]);

        if (r != null && m != null) {
            QueryBuilder qb = avaire.getDatabase().newQueryBuilder(Constants.ROLE_PERSISTENCE_TABLE_NAME)
                .where("guild_id", context.guild.getId()).andWhere("role_id", r.getIdLong()).andWhere("user_id", m.getIdLong());
            Collection c = qb.get();
            if (m.getRoles().contains(r)) {
                context.makeSuccess("User already has role... Checking the database if user has it persistent").queue();
                if (c.isEmpty()) {
                    context.makeWarning("User doesn't not have a record saved in the database about this role. Adding persistence to user...").queue();
                    qb.useAsync(true).insert(statement -> {
                        statement.set("guild_id", context.guild.getIdLong());
                        statement.set("user_id", m.getIdLong());
                        statement.set("role_id", r.getIdLong());
                    });
                    context.makeSuccess("Added " + r.getAsMention() + " to " + m.getAsMention() + "'s persistent roles.").queue();
                } else {
                    context.makeWarning("User has a record saved in the database about this role. Removing persistence from user (And role)...").queue();
                    qb.useAsync(true).delete();
                    context.guild.removeRoleFromMember(m, r).queue();
                    context.makeSuccess("Removed " + r.getAsMention() + " from " + m.getAsMention() + "'s persistent roles.").queue();
                }
            } else {
                context.makeWarning("User does NOT have the role you want to persist on them... Checking database if he/already has persistence...").queue();
                if (c.isEmpty()) {
                    context.makeWarning("User doesn't not have a record saved in the database about this role. Adding persistence to user + giving role...").queue();
                    qb.useAsync(true).insert(statement -> {
                        statement.set("guild_id", context.guild.getIdLong());
                        statement.set("user_id", m.getIdLong());
                        statement.set("role_id", r.getIdLong());
                    });
                    context.getGuild().addRoleToMember(m, r).reason("Auto-role executed by: " + context.member.getEffectiveName()).queue();
                    context.makeSuccess("Added " + r.getAsMention() + " to " + m.getAsMention() + "'s persistent roles.").queue();
                } else {
                    context.makeWarning("User has a record saved in the database about this role. Removing persistence from user + role...").queue();
                    qb.useAsync(true).delete();
                    context.makeSuccess("Removed " + r.getAsMention() + " from " + m.getAsMention() + "'s persistent roles.").queue();
                }
            }
            return true;
        } else if (r != null) {
            context.makeError("User not found. Role does however exist. Try again!").queue();
            return false;
        } else if (m != null) {
            context.makeError("Role you specified does not exist, member does however. Try again!").queue();
            return false;
        } else {
            context.makeError("Neither the user or the role you specified exists. Make sure you've got the correct user and role!").queue();
            return false;
        }
    }

    private boolean runListArgument(CommandMessage context, String[] args) throws SQLException {

        Collection c = avaire.getDatabase().newQueryBuilder(Constants.ROLE_PERSISTENCE_TABLE_NAME)
            .where("guild_id", context.guild.getId()).get();
        StringBuilder sb = new StringBuilder();

        if (!c.isEmpty()) {
            c.forEach(p -> {
                Role r = context.guild.getRoleById(p.getLong("role_id"));
                Member m = context.guild.getMemberById(p.getLong("user_id"));

                sb.append(p.getInt("id")).append(" - ``").append(context.guild.getName()).append("`` - ");
                if (r != null) {
                    sb.append(r.getAsMention()).append(" - ");
                } else {
                    sb.append("*ROLE NOT FOUND*");
                }

                if (m != null) {
                    sb.append(m.getAsMention()).append("\n");
                } else {
                    sb.append("*MEMBER NOT FOUND IN GUILD*\n");
                }
            });

            context.makeSuccess(sb.toString()).queue();
        } else {
            context.makeWarning("No persistence's found").queue();
        }

        return true;
    }
}
