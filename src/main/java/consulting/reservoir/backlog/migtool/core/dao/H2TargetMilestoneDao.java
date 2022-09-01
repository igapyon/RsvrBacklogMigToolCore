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
import java.util.ArrayList;
import java.util.List;

import com.nulabinc.backlog4j.Milestone;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する `Milestone` (インポート先ターゲット) に関する DAO クラス。
 */
public class H2TargetMilestoneDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogTargetMilestone (" //
                + "MilestoneId BIGINT NOT NULL" //
                + ",ProjectId BIGINT" //
                + ",Name VARCHAR(1024)" //
                + ",Description VARCHAR(65535)" //
                + ",StartDate DATE" //
                + ",ReleaseDueDate DATE" //
                + ",Archived BOOL" //
                + ",PRIMARY KEY(MilestoneId)" //
                + ")"))) {
            stmt.executeUpdate();
        }
    }

    /**
     * 与えられた情報を Dao 経由でデータベースに格納します。
     * 
     * @param conn    データベース接続
     * @param source  格納したいデータ。
     * @param bklConn Backlog接続情報。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void store2Local(Connection conn, Milestone source, RsvrBacklogApiConn bklConn) throws SQLException {
        boolean isNew = false;
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT MilestoneId FROM BacklogTargetMilestone WHERE MilestoneId=?"))) {
            stmt.setLong(source.getId());
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }
        }
        if (isNew) {
            try (RsvrPreparedStatement stmtMod = RsvrJdbc
                    .wrap(conn.prepareStatement("INSERT INTO BacklogTargetMilestone (MilestoneId) VALUES (?)"))) {
                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();
            }
            bklConn.getProcessInfo().incrementIns("TargetMilestone");
        } else {
            bklConn.getProcessInfo().incrementUpd("TargetMilestone");
        }

        // 他の項目は全てUPDATEで処理する。
        try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE BacklogTargetMilestone SET" //
                + " ProjectId=?, Name=?, Description=?, StartDate=?, ReleaseDueDate=?, Archived=?" //
                + " WHERE MilestoneId=?"))) {
            stmtMod.setLong(bklConn.getProjectId());
            stmtMod.setString(source.getName());
            stmtMod.setString(source.getDescription());
            stmtMod.setJavaUtilDate(source.getStartDate());
            stmtMod.setJavaUtilDate(source.getReleaseDueDate());
            stmtMod.setBoolean(source.getArchived());
            stmtMod.setLong(source.getId());
            stmtMod.executeUpdateSingleRow();
        }
    }

    public static Long getMilestoneIdByName(Connection conn, String name) throws IOException, SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT MilestoneId FROM BacklogTargetMilestone WHERE Name=?"))) {
            stmt.setString(name);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                if (rset.next() == false) {
                    throw new IOException("該当のMilestoneは発見できず:[" + name + "]");
                }
                return rset.getLong();
            }
        }
    }

    public static List<Long> getMilestoneIdListByNames(Connection conn, String names) throws IOException, SQLException {
        List<Long> idList = new ArrayList<Long>();

        String[] nameArray = names.split(",");
        for (String look : nameArray) {
            idList.add(getMilestoneIdByName(conn, look.trim()));
        }

        return idList;
    }
}
