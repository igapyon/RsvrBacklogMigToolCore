/*
 * Copyright 2022 Reservoir Consulting - Toshiki Iga
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulting.reservoir.backlog.migtool.core.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.nulabinc.backlog4j.User;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する `User` (インポート先ターゲット) に関する DAO クラス。
 */
public class H2TargetUserDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * User の ID をキーとして扱います。マルチテナント的な挙動は考慮していません。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogTargetUser (" //
                + "UserId BIGINT NOT NULL" //
                + ",Name VARCHAR(8192)" //
                + ",BacklogUserId VARCHAR(8192)" //
                + ",MailAddress VARCHAR(8192)" //
                + ",RoleType INT" //
                + ",Lang VARCHAR(256)" //
                + ",PRIMARY KEY(UserId)" //
                + ")" //
        ))) {
            stmt.executeUpdate();
        }
    }

    /**
     * 与えられた情報を Dao 経由でデータベースに格納します。なお、このメソッドは Issueなど呼び出し時につどつど呼ぶようにします。
     * 
     * @param conn   データベース接続
     * @param source 格納したいデータ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void store2Local(Connection conn, User source, RsvrBacklogMigToolProcessInfo processInfo)
            throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT UserId FROM BacklogTargetUser WHERE UserId = ?"//
                ))) {
            stmt.setLong(source.getId());
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                try (RsvrPreparedStatement stmtMod = RsvrJdbc
                        .wrap(conn.prepareStatement("INSERT INTO BacklogTargetUser (UserId) VALUES (?)" //
                        ))) {
                    stmtMod.setLong(source.getId());
                    stmtMod.executeUpdateSingleRow();
                }
                processInfo.incrementIns("TargetUser");
            } else {
                processInfo.incrementUpd("TargetUser");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE BacklogTargetUser SET " //
                    + "Name=?, BacklogUserId=?, MailAddress=?, RoleType=?, Lang=?" //
                    + " WHERE UserId = ? " //
            ))) {
                stmtMod.setString(source.getName());
                stmtMod.setString(source.getUserId());
                stmtMod.setString(source.getMailAddress());
                stmtMod.setInt(source.getRoleType().getIntValue());
                stmtMod.setString(source.getLang());
                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();
            }
        }
    }
}
