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

import com.nulabinc.backlog4j.Version;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する `Version` (インポート先ターゲット) に関する DAO クラス。
 */
public class H2TargetVersionDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogTargetVersion (" //
                + "VersionId BIGINT NOT NULL" //
                + ",ProjectId BIGINT" //
                + ",Name VARCHAR(1024)" //
                + ",Description VARCHAR(65535)" //
                + ",StartDate DATE" //
                + ",ReleaseDueDate DATE" //
                + ",Archived BOOL" //
                + ",PRIMARY KEY(VersionId)" //
                + ")" //
        ))) {
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
    public static void store2Local(Connection conn, Version source, RsvrBacklogApiConn bklConn) throws SQLException {
        boolean isNew = false;
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT VersionId FROM BacklogTargetVersion WHERE VersionId=?"))) {
            stmt.setLong(source.getId());
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }
        }
        if (isNew) {
            try (RsvrPreparedStatement stmtMod = RsvrJdbc
                    .wrap(conn.prepareStatement("INSERT INTO BacklogTargetVersion (VersionId) VALUES (?)" //
                    ))) {
                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();
            }
            bklConn.getProcessInfo().incrementIns("TargetVersion");
        } else {
            bklConn.getProcessInfo().incrementUpd("TargetVersion");
        }

        // 他の項目は全てUPDATEで処理する。
        try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE BacklogTargetVersion SET" //
                + " ProjectId=?, Name=?, Description=?, StartDate=?, ReleaseDueDate=?, Archived=?" //
                + " WHERE VersionId=?"))) {
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

    public static Long getVersionIdByName(Connection conn, String name) throws IOException, SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT VersionId FROM BacklogTargetVersion WHERE Name=?"))) {
            stmt.setString(name);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                if (rset.next() == false) {
                    throw new IOException("該当のVersionは発見できず:[" + name + "]");
                }
                return rset.getLong();
            }
        }
    }

    public static List<Long> getVersionIdListByNames(Connection conn, String names) throws IOException, SQLException {
        List<Long> idList = new ArrayList<Long>();

        String[] nameArray = names.split(",");
        for (String look : nameArray) {
            idList.add(getVersionIdByName(conn, look.trim()));
        }

        return idList;
    }
}
