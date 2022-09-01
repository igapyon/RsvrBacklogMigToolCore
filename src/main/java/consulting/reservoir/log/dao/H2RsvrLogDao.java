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
package consulting.reservoir.log.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * h2 database に対する RsvrBacklogMigTool の ログデータ に関する DAO クラス。
 */
public class H2RsvrLogDao {
    /**
     * このDaoが対象とするテーブルを作成。
     * 
     * @param connLogDb データベース接続。
     * @throws SQLException SQL例外が発生した場合。
     */
    public static void createTable(Connection connLogDb) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(connLogDb.prepareStatement("CREATE TABLE IF NOT EXISTS " //
                + "RsvrLog (" //
                + "LogId IDENTITY NOT NULL" //
                + ",Datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP" //
                + ",Level VARCHAR(80)" //
                + ",Message VARCHAR(8192)" //
                + ",PRIMARY KEY(LogId)" //
                + ")"))) {
            stmt.executeUpdate();
        }
    }

    public static void log(Connection connLogDb, String level, String message) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(connLogDb.prepareStatement("INSERT INTO RsvrLog (Level, Message) VALUES (?,?)"))) {
            stmt.setString(level);
            stmt.setString(message);
            stmt.executeUpdateSingleRow();
        }
    }

    public static void dumpLogAll(Connection connLogDb) throws SQLException {
        H2RsvrLogDao.createTable(connLogDb);

        System.err.println("Dump all log data.");
        final SimpleDateFormat dtf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(
                connLogDb.prepareStatement("SELECT Datetime, Level, Message FROM BacklogMigToolLog ORDER BY LogId"))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    System.err.println(
                            dtf.format(rset.getJavaUtilDate()) + " [" + rset.getString() + "] " + rset.getString());
                }
            }
        }
    }
}
