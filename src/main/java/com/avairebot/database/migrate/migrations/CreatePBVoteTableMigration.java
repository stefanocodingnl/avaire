package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class CreatePBVoteTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Thu, Oct 15th, 2020 11:38 PM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        return schema.createIfNotExists(Constants.MOTS_VOTE_TABLE_NAME, table -> {
            table.Increments("id");
            table.String("vote_id");
            table.String("voted_for");
            table.Long("voter_user_id");
            table.Long("vote_message_id").nullable();
            table.LongText("description").nullable();
            table.Boolean("accepted").defaultValue(false);

            table.Timestamps();
        });
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        return schema.dropIfExists(Constants.MOTS_VOTE_TABLE_NAME);
    }
}
