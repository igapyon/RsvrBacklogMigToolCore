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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolConf;

/**
 * h2 database に対する DAO の共通クラス。
 */
public class H2DaoUtil {
    /**
     * h2データベースの接続を取得します。
     * 
     * @param toolConf BacklogMigToolの構成情報を蓄えるクラス。Backlog API 接続情報や h2 database
     *                 格納フォルダなどを指定。
     * @return データベース接続。
     * @throws IOException 入出力例外が発生した場合。
     */
    public static Connection getConnection(RsvrBacklogMigToolConf toolConf) throws IOException {
        Connection conn;
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new IOException("No class found: " + ex.toString());
        }

        new File(toolConf.getDirDb()).mkdirs();
        final File dbFile = new File(toolConf.getDirDbPath());
        final String jdbcConnStr = "jdbc:h2:file:" + dbFile.getCanonicalPath()
                + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
        // System.err.println("TRACE: Connect: " + jdbcConnStr);
        try {
            conn = DriverManager.getConnection(jdbcConnStr, "sa", "");
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException("Create db failed: " + ex.toString());
        }
        return conn;
    }

    public static String formatDatetime2String(java.util.Date arg) {
        if (arg == null) {
            return "";
        }
        final SimpleDateFormat dtf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dtf.format(arg);
    }

    public static String formatDate2String(java.util.Date arg) {
        if (arg == null) {
            return "";
        }
        final SimpleDateFormat dtf = new SimpleDateFormat("yyyy/MM/dd");
        return dtf.format(arg);
    }

    public static String safe2String(BigDecimal arg) {
        if (arg == null) {
            return "";
        }
        return arg.toPlainString();
    }

    public static String safeToString(String arg) {
        if (arg == null) {
            return "";
        }
        return arg;
    }
}
