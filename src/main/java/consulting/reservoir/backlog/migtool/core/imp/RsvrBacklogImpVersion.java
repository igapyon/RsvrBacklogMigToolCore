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
package consulting.reservoir.backlog.migtool.core.imp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.nulabinc.backlog4j.ResponseList;
import com.nulabinc.backlog4j.Version;
import com.nulabinc.backlog4j.api.option.AddVersionParams;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.apicall.retryable.RetryableAddVersion;
import consulting.reservoir.backlog.migtool.core.dao.H2TargetVersionDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;

/**
 * ローカルの h2 database の `Version` 情報をもとに、Backlog API を呼び出してターゲット Backlog
 * プロジェクトにインポートします。
 */
public class RsvrBacklogImpVersion {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;

    public RsvrBacklogImpVersion(Connection conn, RsvrBacklogApiConn bklConn) {
        this.conn = conn;
        this.bklConn = bklConn;
    }

    /**
     * インポート対象を処理します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    public void process(boolean forceProduction) throws SQLException, IOException {
        if (bklConn.getClient() == null) {
            throw new IllegalArgumentException("Not connected to Backlog. Please login() before process().");
        }

        // 非本番モードの場合に、プロジェクト名が MIGTEST になっているかどうかチェック。
        if (RsvrBacklogMigToolUtil.checkTargetProjectNameForNonProductionModeMIGTEST(conn, bklConn,
                forceProduction) == false) {
            return;
        }

        H2TargetVersionDao.createTable(conn);

        // ターゲットの最新情報をゲット。
        expFromTargetToLocal();

        // ローカルからインポートします。
        impFromLocal();

        // ターゲットの最新情報をゲット。
        expFromTargetToLocal();
    }

    /**
     * 情報をターゲットBacklogに格納します。
     * 
     * @param conn    入力となるデータベース接続。
     * @param bklConn Backlog 接続。
     * @throws SQLException
     */
    private void impFromLocal() throws SQLException, IOException {
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("SELECT" //
                + " Name" //
                + ",Description" //
                + ",StartDate" //
                + ",ReleaseDueDate" //
                + " FROM BacklogVersion" //
                + " ORDER BY VersionId" //
        ))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                for (; rset.next();) {
                    String name = rset.getString();
                    String description = rset.getString();
                    Date startDate = rset.getJavaUtilDate();
                    Date releaseDueDate = rset.getJavaUtilDate();

                    // 今から登録しようとしているものがすでに登録済みかどうか確認。
                    try (RsvrPreparedStatement stmt2 = RsvrJdbc
                            .wrap(conn.prepareStatement("SELECT VersionId FROM BacklogTargetVersion WHERE Name=?"))) {
                        stmt2.setString(name);
                        try (RsvrResultSet rset2 = stmt2.executeQuery()) {
                            if (rset2.next()) {
                                // 既に登録済み。
                            } else {
                                // 新規もの。作成します。
                                final SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd");

                                AddVersionParams params = new AddVersionParams(bklConn.getProjectId(), name);
                                if (description != null) {
                                    params.description(description);
                                }
                                if (startDate != null) {
                                    params.startDate(dtf.format(startDate));
                                }
                                if (releaseDueDate != null) {
                                    params.releaseDueDate(dtf.format(releaseDueDate));
                                }

                                RetryableAddVersion apicallout = new RetryableAddVersion(params);
                                apicallout.execute(bklConn);
                                apicallout.getResult();
                            }
                        }
                    }
                }
            }
        }
    }

    private void expFromTargetToLocal() throws SQLException, IOException {
        // API呼び出しインターバルをsleepします。
        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

        ResponseList<Version> respList = bklConn.getClient().getVersions(bklConn.getProjectId());
        for (Version look : respList) {
            H2TargetVersionDao.store2Local(conn, look, bklConn);
        }

        // API呼び出しインターバルをsleepします。
        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
    }
}
