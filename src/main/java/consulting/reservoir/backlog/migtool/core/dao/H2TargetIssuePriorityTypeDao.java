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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.nulabinc.backlog4j.Priority;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する 課題の `Priority` (インポート先ターゲット) に関する DAO クラス。
 */
public class H2TargetIssuePriorityTypeDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogTargetIssuePriorityType (" //
                + "IssuePriorityTypeId BIGINT NOT NULL" //
                + ",Name VARCHAR(1024)" //
                + ",PRIMARY KEY(IssuePriorityTypeId)" //
                + ")"))) {
            stmt.executeUpdate();
        }
    }

    /**
     * 与えられた情報を Dao 経由でデータベースに格納します。
     * 
     * @param conn        データベース接続
     * @param source      格納したいデータ。
     * @param processInfo 処理情報。
     * @param bklConn     Backlog接続情報。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void store2Local(Connection conn, Priority source, RsvrBacklogMigToolProcessInfo processInfo,
            RsvrBacklogApiConn bklConn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement( //
                "SELECT IssuePriorityTypeId FROM BacklogTargetIssuePriorityType WHERE IssuePriorityTypeId=?"))) {
            stmt.setLong(source.getId());
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement( //
                        "INSERT INTO BacklogTargetIssuePriorityType (IssuePriorityTypeId) VALUES (?)"))) {
                    stmtMod.setLong(source.getId());
                    stmtMod.executeUpdateSingleRow();
                }
                processInfo.incrementIns("TargetIssuePriorityType");
            } else {
                processInfo.incrementUpd("TargetIssuePriorityType");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc
                    .wrap(conn.prepareStatement("UPDATE BacklogTargetIssuePriorityType SET" //
                            + " Name=?" //
                            + " WHERE IssuePriorityTypeId=?"))) {
                stmtMod.setString(source.getName());
                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();
            }
        }
    }

    public static long getIssuePriorityTypeIdByName(Connection conn, String issuePriorityTypeString)
            throws SQLException, IOException {
        // 与えられた文字列をもとに IssueTypeId を取得。
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT IssuePriorityTypeId" //
                + " FROM BacklogTargetIssuePriorityType" //
                + " WHERE Name=?"))) {
            stmt.setString(issuePriorityTypeString);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                if (rset.next() == false) {
                    throw new IOException("ERROR: 指定の名称のIssueTypeが発見できない: " + issuePriorityTypeString);
                } else {
                    return rset.getLong();
                }
            }
        }
    }
}
