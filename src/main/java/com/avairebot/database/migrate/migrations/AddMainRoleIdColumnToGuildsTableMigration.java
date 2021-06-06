package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.connections.MySQL;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class AddMainRoleIdColumnToGuildsTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Sun, Apr 11, 2021 12:40 AM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        if (schema.hasColumn(Constants.GUILD_TABLE_NAME, "main_discord_role")) {
            return true;
        }

        if (schema.getDbm().getConnection() instanceof MySQL) {
            schema.getDbm().queryUpdate(String.format(

                "ALTER TABLE `%s` ADD `main_discord_role` VARCHAR(32) NULL DEFAULT NULL AFTER `filter`;",
                Constants.GUILD_TABLE_NAME
            ));
        } else {
            schema.getDbm().queryUpdate(String.format(
                "ALTER TABLE `%s` ADD `main_discord_role` VARCHAR(32) NULL DEFAULT NULL;",
                Constants.GUILD_TABLE_NAME
            ));
        }

        return true;
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        if (!schema.hasColumn(Constants.GUILD_TABLE_NAME, "main_discord_role")) {
            return true;
        }

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` DROP `main_discord_role`;",
            Constants.GUILD_TABLE_NAME
        ));

        return true;
    }
}
