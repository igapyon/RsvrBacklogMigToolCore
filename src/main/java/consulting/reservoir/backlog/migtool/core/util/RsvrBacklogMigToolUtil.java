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
package consulting.reservoir.backlog.migtool.core.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import consulting.reservoir.backlog.migtool.core.BMCMessages;
import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolConf;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.log.RsvrLog;

/**
 * `RsvrBacklogMigTool` の待機やロギングを担うユーティリティクラス。
 */
public class RsvrBacklogMigToolUtil {
    /**
     * API呼び出しインターバルをsleepします。
     */
    public static final void sleepApiInterval(RsvrBacklogMigToolConf toolConf) {
        try {
            Thread.sleep(toolConf.getApiInterval());
        } catch (InterruptedException ex) {
            System.err.println("Unexpected case: " + ex.toString());
        }
    }

    public static final void sleepApiRateLimitExceedRetryInterval() {
        try {
            Thread.sleep(60000);
        } catch (InterruptedException ex) {
            System.err.println("Unexpected case: " + ex.toString());
        }
    }

    /**
     * ターゲットのBacklogのプロジェクト名が非本番モードの場合にはMIGTESTである事の確認。
     * 
     * @param conn
     * @param bklConn
     * @param forceProduction
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static boolean checkTargetProjectNameForNonProductionModeMIGTEST(Connection conn, RsvrBacklogApiConn bklConn,
            boolean forceProduction) throws SQLException, IOException {
        // プロジェクト名が MIGTEST になっているかどうかチェック。
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT ProjectKey, Name FROM BacklogTargetProject WHERE ProjectId=?"))) {
            stmt.setLong(bklConn.getProjectId());
            try (RsvrResultSet rset = stmt.executeQuery()) {
                if (rset.next() == false) {
                    throw new IOException("想定外: 存在するはずのプロジェクトが見つからず: " + bklConn.getProjectId());
                }

                String projectKey = rset.getString();
                String name = rset.getString();
                if (forceProduction == false && projectKey.startsWith("MIGTEST") == false) {
                    // [BMC5204] Import: Warn: 非本番モードであるのに、ProjectKey が MIGTEST
                    // から始まる名前ではありません。非本番モードでは ProjectKey は MIGTEST で開始するようにしてください. 処理スキップします.";
                    RsvrLog.warn(BMCMessages.BMC5204 + ": [" + projectKey + "] " + name);
                    return false;
                }
            }
        }

        // 処理して良いプロジェクトです。
        return true;
    }
}
