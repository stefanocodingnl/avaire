package com.avairebot.database.migrate.migrations;

import com.avairebot.Constants;
import com.avairebot.contracts.database.migrations.Migration;
import com.avairebot.database.schema.Schema;

import java.sql.SQLException;

public class CreatePBVotableTableMigration implements Migration {

    @Override
    public String created_at() {
        return "Thu, Oct 15, 2020 19:38 PM";
    }

    @Override
    public boolean up(Schema schema) throws SQLException {
        return schema.createIfNotExists(Constants.MOTS_VOTABLE_TABLE_NAME, table -> {
            table.Increments("id");
            table.String("vote_id");
            table.String("item");
            table.String("added_by");
        });
    }

    @Override
    public boolean down(Schema schema) throws SQLException {
        return schema.dropIfExists(Constants.MOTS_VOTABLE_TABLE_NAME);
    }
}
