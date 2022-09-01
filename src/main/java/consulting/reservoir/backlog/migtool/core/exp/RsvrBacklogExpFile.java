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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;

import com.nulabinc.backlog4j.ResponseList;
import com.nulabinc.backlog4j.SharedFile;
import com.nulabinc.backlog4j.SharedFileData;

import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.dao.H2FileDao;
import consulting.reservoir.backlog.migtool.core.dao.H2ProjectDao;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;
import consulting.reservoir.log.RsvrLog;

/**
 * Backlog API を呼び出して `File` 情報を取得して、ローカルの h2 database
 * のテーブルおよびローカルファイルシステムにエクスポートします。
 */
public class RsvrBacklogExpFile {
    private Connection conn = null;
    private RsvrBacklogApiConn bklConn = null;

    public RsvrBacklogExpFile(Connection conn, RsvrBacklogApiConn bklConn) {
        this.conn = conn;
        this.bklConn = bklConn;
    }

    /**
     * エクスポート対象を処理します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    public void process(File baseDir) throws SQLException, IOException {
        if (bklConn.getClient() == null) {
            throw new IllegalArgumentException("Not connected to Backlog. Please login() before process().");
        }

        File baseProjectDir = new File(baseDir, H2ProjectDao.getProjectKey(conn, bklConn.getProjectId()));

        // h2 に Fileテーブルを作成します。
        H2FileDao.createTable(conn);

        // Fileをローカルに保管します。
        toLocal(baseProjectDir, "");
    }

    /**
     * Fileの一覧をローカルに格納します。
     * 
     * @param bklConn Backlog 接続。
     * @throws SQLException
     * @throws IOException
     */
    private void toLocal(File baseProjectDir, String path) throws SQLException, IOException {
        ResponseList<SharedFile> fileList = bklConn.getClient().getSharedFiles(bklConn.getProjectId(), path);

        // API呼び出しインターバルをsleepします。
        RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

        for (SharedFile lookup : fileList) {
            H2FileDao.store2Local(conn, lookup, bklConn.getProcessInfo());

            if ("directory".equals(lookup.getType())) {
                File dir = new File(baseProjectDir, path + (path.length() == 0 ? "" : "/") + lookup.getName());
                if (dir.exists() == false) {
                    dir.mkdirs();
                }

                toLocal(baseProjectDir, (path.length() == 0 ? path + lookup.getName() : path + "/" + lookup.getName()));
            } else if ("file".equals(lookup.getType())) {
                SharedFileData fileData = bklConn.getClient().downloadSharedFile(bklConn.getProjectId(),
                        lookup.getId());
                FileUtils.copyToFile(fileData.getContent(),
                        new File(baseProjectDir, path + "/" + fileData.getFilename()));

                // API呼び出しインターバルをsleepします。
                RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());
            } else {
                RsvrLog.error("ファイルをExport時に不明なタイプ: " + lookup.getType());
            }
        }
    }
}
