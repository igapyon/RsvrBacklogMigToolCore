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
package consulting.reservoir.backlog.migtool.core.exp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.nulabinc.backlog4j.Category;
import com.nulabinc.backlog4j.ResponseList;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.dao.H2CategoryDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;

/**
 * Backlog API を呼び出して `Category` 情報を取得して、ローカルの h2 database のテーブルにエクスポートします。
 */
public class RsvrBacklogExpCategory {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;

    public RsvrBacklogExpCategory(Connection conn, RsvrBacklogApiConn bklConn) {
        this.conn = conn;
        this.bklConn = bklConn;
    }

    /**
     * エクスポート対象を処理します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    public void process() throws SQLException, IOException {
        if (bklConn.getClient() == null) {
            throw new IllegalArgumentException("Not connected to Backlog. Please login() before process().");
        }

        // h2 にテーブルを作成します。
        H2CategoryDao.createTable(conn);

        // ローカルに保管します。
        toLocal();
    }

    /**
     * 情報をローカルに格納します。
     * 
     * @param bklConn Backlog 接続。
     * @throws SQLException
     * @throws IOException
     */
    private void toLocal() throws SQLException, IOException {
        ResponseList<Category> respList = bklConn.getClient().getCategories(bklConn.getProjectId());
        if (respList == null) {
            return;
        }
        for (Category look : respList) {
            H2CategoryDao.store2Local(conn, look, bklConn);
        }

        // API呼び出しインターバルをsleepします。
        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
    }
}
