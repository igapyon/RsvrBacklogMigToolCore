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
package consulting.reservoir.log;

import java.sql.Connection;
import java.sql.SQLException;

import consulting.reservoir.log.dao.H2RsvrLogDao;
import consulting.reservoir.log.dao.RsvrLogH2ConnUtil;

public class RsvrLog {
    /**
     * 同期ロック用のオブジェクト。
     */
    private static final Object lockObj = new Object();

    /**
     * データベース接続は開きっぱなしとします。
     */
    private static Connection connLogDb = null;

    /**
     * トレースレベルのログを出力。
     * 
     * @param message ログのメッセージ。
     */
    public static void trace(String message) {
        log("trace", message);
    }

    /**
     * info レベルのログを出力。
     * 
     * @param message ログのメッセージ。
     */
    public static void info(String message) {
        log("info", message);
    }

    /**
     * warn レベルのログを出力。
     * 
     * @param message ログのメッセージ。
     */
    public static void warn(String message) {
        log("warn", message);
    }

    /**
     * error レベルのログを出力。
     * 
     * @param message ログのメッセージ。
     */
    public static void error(String message) {
        log("error", message);
    }

    /**
     * fatal レベルのログを出力。
     * 
     * @param message ログのメッセージ。
     */
    public static void fatal(String message) {
        log("fatal", message);
    }

    /**
     * ログレベルを指定してログを出力。
     * 
     * @param level   ログレベル。
     * @param message ログのメッセージ。
     */
    private static void log(final String level, final String message) {
        {
            String displayLevel = level;
            if ("info".equals(level) || "warn".equals(level)) {
                displayLevel = displayLevel + " ";
            }

            // 標準エラー出力に表示
            System.err.println("log [" + displayLevel + "] " + message);
        }

        // ログDBに書込
        try {
            synchronized (lockObj) {
                if (connLogDb == null) {
                    connLogDb = RsvrLogH2ConnUtil.getLogDbConnection();
                }

                H2RsvrLogDao.createTable(connLogDb);
            }

            synchronized (lockObj) {
                H2RsvrLogDao.log(connLogDb, level, message);
            }
        } catch (SQLException ex) {
            System.err.println("想定外: ログ出力で問題: " + ex.getMessage());
        }
    }
}
