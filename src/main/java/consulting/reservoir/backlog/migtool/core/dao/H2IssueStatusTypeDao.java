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

import com.nulabinc.backlog4j.Status;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する 課題の `IssueStatusType` に関する DAO クラス。
 */
public class H2IssueStatusTypeDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogIssueStatusType (" //
                + "IssueStatusTypeId INT NOT NULL" //
                + ",ProjectId BIGINT" //
                + ",Name VARCHAR(1024)" //
                + ",DisplayOrder INT" //
                + ",PRIMARY KEY(IssueStatusTypeId)" //
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
    public static void store2Local(Connection conn, Status source, RsvrBacklogMigToolProcessInfo processInfo,
            RsvrBacklogApiConn bklConn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn
                .prepareStatement("SELECT IssueStatusTypeId FROM BacklogIssueStatusType WHERE IssueStatusTypeId=?"))) {
            stmt.setInt(source.getId());
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement( //
                        "INSERT INTO BacklogIssueStatusType (IssueStatusTypeId) VALUES (?)"))) {
                    stmtMod.setInt(source.getId());
                    stmtMod.executeUpdateSingleRow();
                }
                processInfo.incrementIns("IssueStatusType");
            } else {
                processInfo.incrementUpd("IssueStatusType");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE BacklogIssueStatusType SET" //
                    + " ProjectId=?, Name=?, DisplayOrder=?" //
                    + " WHERE IssueStatusTypeId = ?" //
            ))) {
                stmtMod.setLong(bklConn.getProjectId());
                stmtMod.setString(source.getName());
                stmtMod.setInt(source.getDisplayOrder());
                stmtMod.setInt(source.getId());
                stmtMod.executeUpdateSingleRow();
            }
        }
    }
}
