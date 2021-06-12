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

package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.connections.MySQL;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class AddOnWatchToGuildsTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Mon, Feb 12, 2018 3:11 AM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        if (schema.hasColumn(Constants.GUILD_TABLE_NAME, "on_watch") && schema.hasColumn(Constants.GUILD_TABLE_NAME, "on_watch_case")) {
            return true;
        }

        if (schema.getDbm().getConnection() instanceof MySQL) {
            schema.getDbm().queryUpdate(String.format(
                "ALTER TABLE `%s` ADD `on_watch` VARCHAR(32) NULL DEFAULT NULL AFTER `autorole`, ADD `on_watch_case` INT NOT NULL DEFAULT '0' AFTER `on_watch`;",
                Constants.GUILD_TABLE_NAME
            ));
        } else {
            schema.getDbm().queryUpdate(String.format(
                "ALTER TABLE `%s` ADD `on_watch` VARCHAR(32) NULL DEFAULT NULL;",
                Constants.GUILD_TABLE_NAME
            ));

            schema.getDbm().queryUpdate(String.format(
                "ALTER TABLE `%s` ADD `on_watch_case` INT NOT NULL DEFAULT '0';",
                Constants.GUILD_TABLE_NAME
            ));
        }

        return true;
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        if (!schema.hasColumn(Constants.GUILD_TABLE_NAME, "on_watch") && !schema.hasColumn(Constants.GUILD_TABLE_NAME, "on_watch_case")) {
            return true;
        }

        schema.getDbm().queryUpdate(String.format(
            "ALTER TABLE `%s` DROP `on_watch`, DROP `on_watch_case`;",
            Constants.GUILD_TABLE_NAME
        ));

        return true;
    }
}
