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

import consulting.reservoir.backlog.migtool.core.BMCMessages;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.log.RsvrLog;

/**
 * h2 database に対する `User` のエクスポート後とインポート先のマッピング に関する DAO クラス。
 */
public class H2MappingUserDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param conn データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "BacklogMappingUser (" //
                + "SourceUserId BIGINT NOT NULL" //
                + ",TargetUserId BIGINT" //
                + ",Reviewed BOOL DEFAULT FALSE" //
                + ",MappingResult VARCHAR(65535)" //
                + ",Created TIMESTAMP DEFAULT CURRENT_TIMESTAMP" //
                + ",PRIMARY KEY(SourceUserId)" //
                + ")"))) {
            stmt.executeUpdate();
        }
    }

    /**
     * まずは Source側の User情報をもとに初期データをテーブルにセットアップ。
     * 
     * このメソッドは、4:Wiki や 5:Issue の後に再び実行すると想定される。
     * 
     * @param conn
     * @param bklConn
     * @throws SQLException
     */
    public static void setupInitialData(Connection conn, RsvrBacklogApiConn bklConn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT UserId FROM BacklogUser ORDER BY UserId"))) {
            try (RsvrPreparedStatement stmtCheckExists = RsvrJdbc
                    .wrap(conn.prepareStatement("SELECT SourceUserId FROM BacklogMappingUser WHERE SourceUserId=?"))) {

                try (RsvrResultSet rset = stmt.executeQuery()) {
                    for (; rset.next();) {
                        final Long sourceUserId = rset.getLong();

                        // 検索のパラメータをクリア。
                        stmtCheckExists.clearParameters();
                        stmtCheckExists.setLong(sourceUserId);
                        try (RsvrResultSet rsetCheckExists = stmtCheckExists.executeQuery()) {
                            if (rsetCheckExists.next() == false) {
                                // 新規にレコードが必要。
                                try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement(
                                        "INSERT INTO BacklogMappingUser (SourceUserId,TargetUserId,MappingResult) VALUES (?,0,?)"))) {
                                    stmtMod.setLong(sourceUserId);
                                    stmtMod.setString("新規に検知されたUser");
                                    stmtMod.executeUpdateSingleRow();
                                }
                                bklConn.getProcessInfo().incrementIns("MappingUser");
                            }
                        }
                    }
                }
            }
        }
    }

    public static void autoMappingWithEmailAddress(Connection conn, RsvrBacklogApiConn bklConn) throws SQLException {
        // TargetUserId が0のひと(マッピングが決まっていない人)を対象に、メールアドレスでマップを試みます。
        RsvrLog.trace("Userマッピング: EMAILドレスによるユーザーマッピングを試行");
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT m.SourceUserId, u1.Name, u2.UserId AS TargetUserIdCandidate" //
                        + " FROM BacklogMappingUser m" //
                        + " INNER JOIN BacklogUser u1 ON m.SourceUserId = u1.UserId" //
                        + " LEFT OUTER JOIN BacklogTargetUser u2 ON u1.MailAddress = u2.MailAddress" //
                        + " WHERE m.TargetUserId = 0 AND u2.UserId IS NOT NULL" //
                        + " ORDER BY m.SourceUserId"))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    final Long sourceUserId = rset.getLong();
                    final String name = rset.getString();
                    final Long targetUserIdCandidate = rset.getLong();

                    try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement(
                            "UPDATE BacklogMappingUser SET TargetUserId=?,MappingResult=? WHERE SourceUserId=?"))) {
                        stmtMod.setLong(targetUserIdCandidate);
                        stmtMod.setString("MailAddress完全一致によりUserを引き当てました。");
                        stmtMod.setLong(sourceUserId);
                        stmtMod.executeUpdateSingleRow();
                        bklConn.getProcessInfo().incrementUpd("MappingUser");
                    }
                    // [MBC5111] import: Mapping User: 新旧ユーザをメールアドレスをもとに引き当て:
                    RsvrLog.info(BMCMessages.MBC5111 + name);
                }
            }
        }
    }

    public static void autoMappingWithName(Connection conn, RsvrBacklogApiConn bklConn) throws SQLException {
        // TargetUserId が0のひと(マッピングが決まっていない人)を対象に、メールアドレスでマップを試みます。
        RsvrLog.trace("Userマッピング: 名前によるユーザーマッピングを試行");
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT m.SourceUserId, u1.Name, u2.UserId AS TargetUserIdCandidate" //
                        + " FROM BacklogMappingUser m" //
                        + " INNER JOIN BacklogUser u1 ON m.SourceUserId = u1.UserId" //
                        + " LEFT OUTER JOIN BacklogTargetUser u2 ON u1.Name = u2.Name" //
                        + " WHERE m.TargetUserId = 0 AND u2.UserId IS NOT NULL" //
                        + " ORDER BY m.SourceUserId"))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    final Long sourceUserId = rset.getLong();
                    final String name = rset.getString();
                    final Long targetUserIdCandidate = rset.getLong();

                    try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement(
                            "UPDATE BacklogMappingUser SET TargetUserId=?,MappingResult=? WHERE SourceUserId=?"))) {
                        stmtMod.setLong(targetUserIdCandidate);
                        stmtMod.setString("名前が一致によりUserを引き当てました。");
                        stmtMod.setLong(sourceUserId);
                        stmtMod.executeUpdateSingleRow();
                        bklConn.getProcessInfo().incrementUpd("MappingUser");
                    }
                    // [MBC5112] import: Mapping User: 新旧ユーザを名前をもとに引き当て:
                    RsvrLog.info(BMCMessages.MBC5112 + name);
                }
            }
        }
    }

    public static void reportUserMapping(Connection conn, RsvrBacklogApiConn bklConn) throws SQLException {
        // [MBC5113] import: Mapping User: ユーザマッピング状況は以下のようになります。
        RsvrLog.info(BMCMessages.MBC5113);
        int notMappedUser = 0;
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement(
                "SELECT u1.Name AS SourceUserName, m.SourceUserId AS SourceUserId, u2.Name TargetUserName, u2.UserId AS TargetUserId, " //
                        + " FROM BacklogMappingUser m" //
                        + " LEFT OUTER JOIN BacklogUser       u1 ON m.SourceUserId = u1.UserId" //
                        + " LEFT OUTER JOIN BacklogTargetUser u2 ON m.TargetUserId = u2.UserId" //
                        + " ORDER BY m.SourceUserId"))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    final String sourceUserName = rset.getString();
                    final Long sourceUserId = rset.getLong();
                    final String targetUserName = rset.getString();
                    final Long targetUserId = rset.getLong();
                    if (targetUserId != null && targetUserId != 0) {
                        RsvrLog.info("  - " + sourceUserName + "(" + sourceUserId + ") => " + targetUserName + "("
                                + targetUserId + ")");
                    } else {
                        RsvrLog.info("  - " + sourceUserName + "(" + sourceUserId + ") => ((未割当)))");
                        notMappedUser++;
                    }
                }
            }
        }
        if (notMappedUser > 0) {
            // [MBC5114] import: Mapping User: ターゲット Backlog ユーザへの割り当てが未実施のユーザが存在します。未割当数:
            RsvrLog.warn(BMCMessages.MBC5114 + notMappedUser);

            // [MBC5115] import: Mapping User: h2 database
            // の「BacklogMappingUser」テーブルを編集してすべてのユーザマッピングを割当済にしてください。
            RsvrLog.warn(BMCMessages.MBC5115);

            // [MBC5116] マッピング未割当を解消してから Import Phase4 以降を実施してください。";
            RsvrLog.warn(BMCMessages.MBC5116);
        } else {
            // [MBC5117] マッピングはすべて割当済みです。Import Phase4 以降を実施が可能です。
            RsvrLog.info(BMCMessages.MBC5117);
        }
    }

    public static Long getRepresentativeUserId(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement(
                "SELECT UserId AS TargetUserIdCandidate FROM BacklogTargetUser ORDER BY RoleType,UserId"))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                if (rset.next() == false) {
                    System.err.println("想定外。代表的なユーザ見つからず");
                    return null;
                } else {
                    return rset.getLong();
                }
            }
        }
    }

    public static Long getTargetUserIdBySourceUserId(Connection conn, Long sourceUserId) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT m.TargetUserId FROM BacklogMappingUser m" //
                        + " WHERE m.TargetUserId<>0 AND m.SourceUserId=?"))) {
            stmt.setLong(sourceUserId);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getLong();
                } else {
                    RsvrLog.warn("指定のユーザには MappingUser 指定が見つかりませんでした。代わりに代表ユーザを指定します。: " + sourceUserId);
                    return getRepresentativeUserId(conn);
                }
            }
        }
    }

    /**
     * source側のユーザ名をもとに、target側のUserIdを取得する。
     * 
     * BacklogIssueCommentChangeLog において、内包する値そのまま h2 database
     * に格納している箇所があり、そこだけは名前から引き当ての必要がある。
     * 
     * なお、このメソッドはなるべく呼ばれないことが好ましい。原則 UserId でハンドリングされて欲しいため。
     * 
     * @param conn
     * @param sourceUserName source側のユーザ名。
     * @return target側のユーザId.
     * @throws SQLException
     */
    public static Long getTargetUserIdBySourceUserName(Connection conn, String sourceUserName) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT m.TargetUserId" //
                + " FROM BacklogMappingUser m" //
                + " INNER JOIN BacklogUser u1 ON m.SourceUserId = u1.UserId" //
                + " WHERE m.TargetUserId<>0 AND u1.Name=?"))) {
            stmt.setString(sourceUserName);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getLong();
                } else {
                    RsvrLog.warn("指定のユーザには MappingUser 指定が見つかりませんでした。代わりに代表ユーザを指定します。: " + sourceUserName);
                    return getRepresentativeUserId(conn);
                }
            }
        }
    }
}
