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

import com.nulabinc.backlog4j.Project;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する `Project` (インポート先ターゲット) に関する DAO クラス。
 */
public class H2TargetProjectDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogTargetProject (" //
                + "ProjectId BIGINT NOT NULL" //
                + ",ProjectKey VARCHAR(80)" //
                + ",Name VARCHAR(8192)" //
                + ",IsChartEnabled BOOL" //
                + ",IsSubtaskingEnabled BOOL" //
                + ",TextFormattingRule VARCHAR(80)" //
                + ",IsArchived BOOL" //
                + ",DisplayOrder BIGINT" //
                + ",UseWiki BOOL" //
                + ",UseFileSharing BOOL" //
                + ",UseDevAttributes BOOL" //
                + ",UseResolvedForChart BOOL" //
                + ",UseWikiTreeView BOOL" //
                + ",UseOriginalImageSizeAtWiki BOOL" //
                + ",PRIMARY KEY(ProjectId)" //
                + ")" //
        ))) {
            stmt.executeUpdate();
        }
    }

    /**
     * 与えられた情報を Dao 経由でデータベースに格納します。
     * 
     * @param conn   データベース接続
     * @param source 格納したいデータ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void store2Local(Connection conn, Project source, RsvrBacklogMigToolProcessInfo processInfo)
            throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(
                conn.prepareStatement("SELECT ProjectId FROM BacklogTargetProject WHERE ProjectId =  " + source.getId() //
                ))) {
            boolean isNew = false;
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }

            if (isNew) {
                try (RsvrPreparedStatement stmtMod = RsvrJdbc
                        .wrap(conn.prepareStatement("INSERT INTO BacklogTargetProject (ProjectId) VALUES (?)" //
                        ))) {
                    stmtMod.setLong(source.getId());
                    stmtMod.executeUpdateSingleRow();
                }
                processInfo.incrementIns("TargetProject");
            } else {
                processInfo.incrementUpd("TargetProject");
            }

            // 他の項目は全てUPDATEで処理する。
            try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE BacklogTargetProject SET " //
                    + "ProjectKey=?, Name=?" //
                    + ", IsChartEnabled=?, IsSubtaskingEnabled=?, TextFormattingRule=?, IsArchived=?" //
                    + ", DisplayOrder=?, UseWiki=?, UseFileSharing=?, UseDevAttributes=?, UseResolvedForChart=?, UseWikiTreeView=?, UseOriginalImageSizeAtWiki=?"
                    + " WHERE ProjectId = ?" //
            ))) {
                stmtMod.setString(source.getProjectKey());
                stmtMod.setString(source.getName());
                stmtMod.setBoolean(source.isChartEnabled());
                stmtMod.setBoolean(source.isSubtaskingEnabled());
                stmtMod.setString(source.getTextFormattingRule().getStrValue());
                stmtMod.setBoolean(source.isArchived());
                stmtMod.setLong(source.getDisplayOrder());
                stmtMod.setBoolean(source.getUseWiki());
                stmtMod.setBoolean(source.getUseFileSharing());
                stmtMod.setBoolean(source.getUseDevAttributes());
                stmtMod.setBoolean(source.getUseResolvedForChart());
                stmtMod.setBoolean(source.getUseWikiTreeView());
                stmtMod.setBoolean(source.getUseOriginalImageSizeAtWiki());
                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();
            }
        }
    }

    /**
     * このh2 database の登録済みProjectが自分かどうか確認する。登録が0件の場合は自分の持ち物と理解する。
     * 
     * @param conn
     * @param projectId
     * @return
     * @throws SQLException
     */
    public static boolean isH2OnlyMyProject(Connection conn, String projectKey) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT COUNT(ProjectId) FROM BacklogTargetProject"))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                rset.next();
                Long count = rset.getLong();
                if (count == 0) {
                    // 自分が初めての登録だ。これは私のプロジェクトと判断できる。
                    return true;
                }
                if (count > 1) {
                    // すでにプロジェクトが2つある。これは処理ができない。危険だ。
                    return false;
                }
            }
        }

        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT ProjectId FROM BacklogTargetProject WHERE ProjectKey=?"))) {
            stmt.setString(projectKey);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    // 自分が既に登録済みのプロジェクトだ。
                    return true;
                } else {
                    // 自分以外がこのDBのオーナーだ。他人のプロジェクトだ。
                    return false;
                }
            }
        }
    }

    /**
     * Project Key から Project Id を取り出すための簡易メソッド。
     * 
     * @param conn       h2 database データベース接続。
     * @param projectKey Project Key。
     * @return ProjectId。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static Long getProjectId(Connection conn, String projectKey) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT ProjectKey FROM BacklogTargetProject WHERE ProjectKey=?"))) {
            stmt.setString(projectKey);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                if (rset.next() == false) {
                    return null;
                } else {
                    return rset.getLong();
                }
            }
        }
    }
}
