package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class CreateEventRequestsTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Tue, Dec 1nd, 2020 7:30 AM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        return schema.createIfNotExists(Constants.EVENT_SCHEDULE_REQUESTS_TABLE, table -> {
            table.Increments("id");
            table.Long("guild_id");
            table.Long("request_message_id");
            table.Long("requester_discord_id");
            table.Timestamps();
        });
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        return schema.dropIfExists(Constants.EVENT_SCHEDULE_REQUESTS_TABLE);
    }
}
