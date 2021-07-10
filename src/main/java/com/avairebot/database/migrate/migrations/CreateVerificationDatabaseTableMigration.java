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
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class CreateVerificationDatabaseTableMigration implements Migration {
    @Override
    public String created_at() {
        return "Thu, Jul 8, 2021 8:15 PM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        return schema.createIfNotExists(Constants.VERIFICATION_DATABASE_TABLE_NAME, table -> {
            table.Long("id");
            table.Long("robloxId");
            table.String("username", 16);
        });
    }


    @Override
    public boolean down(Schema schema) throws SQLException {
        return schema.dropIfExists(Constants.VERIFICATION_DATABASE_TABLE_NAME);
    }
}
