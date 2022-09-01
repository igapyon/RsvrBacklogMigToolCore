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
import java.sql.Types;
import java.util.List;

import com.nulabinc.backlog4j.Category;
import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Milestone;
import com.nulabinc.backlog4j.SharedFile;
import com.nulabinc.backlog4j.Version;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.log.RsvrLog;

/**
 * h2 database に対する `Issue` (インポート先ターゲット) に関する DAO クラス。
 * 
 * このテーブルは特殊。Issue作成時に、もとのIssueとの関係を記録する。
 */
public class H2TargetIssueDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogTargetIssue (" //
                + "TargetIssueId BIGINT NOT NULL" //
                + ",SourceIssueId BIGINT" //
                + ",IssueKey VARCHAR(80)" //
                + ",KeyId BIGINT" //
                + ",ProjectId BIGINT" //
                + ",IssueType VARCHAR(80)" //
                + ",Summary VARCHAR(8192)" //
                + ",Description VARCHAR(65535)" //
                + ",Resolution VARCHAR(80)" //
                + ",Priority VARCHAR(80)" //
                + ",Status VARCHAR(80)" //
                + ",Assignee BIGINT" //
                + ",Category VARCHAR(8192)" // 文字列でカンマ区切り列挙
                + ",Version VARCHAR(8192)" // 文字列でカンマ区切り列挙
                + ",Milestone VARCHAR(8192)" // 文字列でカンマ区切り列挙
                + ",StartDate DATE" //
                + ",DueDate DATE" //
                + ",EstimatedHours VARCHAR(80)" //
                + ",ActualHours VARCHAR(80)" //
                + ",ParentIssueId VARCHAR(80)" //
                + ",CreatedUser BIGINT" //
                + ",Created TIMESTAMP" //
                + ",UpdatedUser BIGINT" //
                + ",Updated TIMESTAMP"//
                + ",SharedFile VARCHAR(65535)" // 文字列でカンマ区切り列挙
                + ",PRIMARY KEY(TargetIssueId)" //
                + ")" //
        ))) {
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
    public static void store2Local(Connection conn, Issue source, long sourceIssueId, RsvrBacklogApiConn bklConn)
            throws SQLException {
        boolean isNew = false;
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT TargetIssueId FROM BacklogTargetIssue WHERE TargetIssueId=?"))) {
            stmt.setLong(source.getId());
            try (RsvrResultSet rset = stmt.executeQuery()) {
                isNew = (rset.next() == false);
            }
        }

        if (isNew) {
            try (RsvrPreparedStatement stmtMod = RsvrJdbc
                    .wrap(conn.prepareStatement("INSERT INTO BacklogTargetIssue (TargetIssueId) VALUES (?)"))) {
                stmtMod.setLong(source.getId());
                stmtMod.executeUpdateSingleRow();
            }
            bklConn.getProcessInfo().incrementIns("Issue");
        } else {
            bklConn.getProcessInfo().incrementUpd("Issue");
        }

        // 他の項目は全てUPDATEで処理する。
        try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE BacklogTargetIssue SET " //
                + "SourceIssueId=?, IssueKey=?, Summary=?, KeyId=?" //
                + ", ProjectId=?, IssueType=?, Description=?, Resolution=?, Priority=?, Status=?" //
                + ", Assignee=?, Category=?, Version=?, Milestone=?, StartDate=?, DueDate=?, EstimatedHours=?, ActualHours=?" //
                + ", ParentIssueId=?, CreatedUser=?, Created=?, UpdatedUser=?, Updated=?" //
                + ", SharedFile=?" //
                + " WHERE TargetIssueId = ? " //
        ))) {
            stmtMod.setLong(sourceIssueId);
            stmtMod.setString(source.getIssueKey());
            stmtMod.setString(source.getSummary());
            stmtMod.setLong(source.getKeyId());
            stmtMod.setLong(bklConn.getProjectId());
            stmtMod.setString(source.getIssueType().getName());
            stmtMod.setString(source.getDescription());
            stmtMod.setString((source.getResolution() == null ? null : source.getResolution().getName()));
            stmtMod.setString((source.getPriority() == null ? null : source.getPriority().getName()));
            stmtMod.setString((source.getStatus() == null ? null : source.getStatus().getName()));
            if (source.getAssignee() == null) {
                stmtMod.setNull(Types.BIGINT);
            } else {
                stmtMod.setLong(source.getAssignee().getId());
                H2UserDao.store2Local(conn, source.getAssignee(), bklConn.getProcessInfo());
            }

            // Categoryの一覧をカンマ区切り文字列化します。
            {
                String categoryString = "";
                for (Category look : source.getCategory()) {
                    if (categoryString.length() != 0) {
                        categoryString += ",";
                    }
                    categoryString += look.getName();
                }
                stmtMod.setString(categoryString);
            }

            // Versionの一覧をカンマ区切り文字列化します。
            {
                String versionString = "";
                for (Version look : source.getVersions()) {
                    if (versionString.length() != 0) {
                        versionString += ",";
                    }
                    versionString += look.getName();
                }
                stmtMod.setString(versionString);
            }

            // Milestoneの一覧をカンマ区切り文字列化します。
            {
                String milestoneString = "";
                for (Milestone look : source.getMilestone()) {
                    if (milestoneString.length() != 0) {
                        milestoneString += ",";
                    }
                    milestoneString += look.getName();
                }
                stmtMod.setString(milestoneString);
            }
            stmtMod.setJavaUtilDate(source.getStartDate());
            stmtMod.setJavaUtilDate(source.getDueDate());
            stmtMod.setBigDecimal(source.getEstimatedHours());
            stmtMod.setBigDecimal(source.getActualHours());
            stmtMod.setLong(source.getParentIssueId());
            if (source.getCreatedUser() == null) {
                stmtMod.setNull(Types.BIGINT);
            } else {
                stmtMod.setLong(source.getCreatedUser().getId());
                H2UserDao.store2Local(conn, source.getCreatedUser(), bklConn.getProcessInfo());
            }
            stmtMod.setJavaUtilDate(source.getCreated());
            if (source.getUpdatedUser() == null) {
                stmtMod.setNull(Types.BIGINT);
            } else {
                stmtMod.setLong(source.getUpdatedUser().getId());
                H2UserDao.store2Local(conn, source.getUpdatedUser(), bklConn.getProcessInfo());
            }
            stmtMod.setJavaUtilDate(source.getUpdated());

            // 一覧をカンマ区切り文字列化します。
            {
                String sharedFileString = "";
                for (SharedFile look : source.getSharedFiles()) {
                    if (sharedFileString.length() != 0) {
                        sharedFileString += ",";
                    }
                    sharedFileString += look.getId();
                }
                stmtMod.setString(sharedFileString);
                if (sharedFileString.length() > 0) {
                    RsvrLog.warn("SharedFileの使用例が登場: " + sharedFileString);
                }
            }

            stmtMod.setLong(source.getId());
            stmtMod.executeUpdateSingleRow();

            if (source.getCreatedUser() != null) {
                H2UserDao.store2Local(conn, source.getCreatedUser(), bklConn.getProcessInfo());
            }
            if (source.getUpdatedUser() != null) {
                H2UserDao.store2Local(conn, source.getUpdatedUser(), bklConn.getProcessInfo());
            }
        }

        // Issueに紐づくとマークされたカスタムフィールドをStore。
        List<CustomField> fields = source.getCustomFields();
        for (CustomField field : fields) {
            H2IssueCustomFieldDao.store2Local(conn, field, bklConn.getProcessInfo(), source.getId());
        }
    }
}
