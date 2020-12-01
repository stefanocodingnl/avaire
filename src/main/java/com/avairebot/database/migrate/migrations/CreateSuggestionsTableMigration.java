package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class CreateSuggestionsTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Tuesday, December 1nd, 2020 7:30 AM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        return schema.createIfNotExists(Constants.PB_SUGGESTIONS_TABLE_NAME, table -> {
            table.Increments("id");
            table.Long("pb_server_id");
            table.Long("suggestion_message_id");
            table.Long("suggester_discord_id");
            table.String("suggester_discord_name");
            table.String("suggestion_response").nullable();
            table.String("suggestion_description");
            table.String("suggestion_comments").nullable();
            table.Timestamps();
        });
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        return schema.dropIfExists(Constants.REPORTS_DATABASE_TABLE_NAME);
    }
}
