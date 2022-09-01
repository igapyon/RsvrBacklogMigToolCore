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
package consulting.reservoir.backlog.migtool.core.map;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.dao.H2MappingUserDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;

/**
 * ローカルの h2 database 上にある User 情報をもとにマッピングテーブルを構築します。
 */
public class RsvrBacklogMapImpUser {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;

    public RsvrBacklogMapImpUser(Connection conn, RsvrBacklogApiConn bklConn) {
        this.conn = conn;
        this.bklConn = bklConn;
    }

    /**
     * 対象を処理します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    public void process() throws SQLException, IOException {
        if (bklConn.getClient() == null) {
            throw new IllegalArgumentException("Not connected to Backlog. Please login() before process().");
        }

        // h2 にテーブルを作成します。
        H2MappingUserDao.createTable(conn);

        // ローカルで処理します。
        mapping();
    }

    /**
     * 情報を処理します。
     * 
     * @throws SQLException
     * @throws IOException
     */
    private void mapping() throws SQLException, IOException {
        // EMAILで完全一致したらそれをセット
        H2MappingUserDao.autoMappingWithEmailAddress(conn, bklConn);

        // 名前で一致したとしてもそれをセット
        H2MappingUserDao.autoMappingWithName(conn, bklConn);

        // マッピング状況を報告。
        H2MappingUserDao.reportUserMapping(conn, bklConn);

        // API呼び出しインターバルをsleepします。
        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
    }
}
