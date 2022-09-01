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
package consulting.reservoir.backlog.migtool.core.apicall;

import java.io.IOException;
import java.sql.SQLException;

import com.nulabinc.backlog4j.BacklogClientFactory;
import com.nulabinc.backlog4j.Space;
import com.nulabinc.backlog4j.conf.BacklogComConfigure;
import com.nulabinc.backlog4j.conf.BacklogConfigure;
import com.nulabinc.backlog4j.conf.BacklogJpConfigure;

import consulting.reservoir.backlog.migtool.core.BMCMessages;
import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolConf;
import consulting.reservoir.log.RsvrLog;

/**
 * Backlog API 呼び出しに関するユーティリティクラス。
 */
public class RsvrBacklogApiConnUtil {
    /**
     * Backlog API にログインします。
     * 
     * @param toolConf BacklogMigTool 構成ファイル。
     * @return Backlog API 呼び出し接続情報をまとめたもの。
     * @throws SQLException SQL例外が発生した場合。
     * @throws IOException  IO例外が発生した場合。
     */
    public static RsvrBacklogApiConn login(final RsvrBacklogMigToolConf toolConf) throws SQLException, IOException {
        RsvrBacklogApiConn bklConn = new RsvrBacklogApiConn();
        // このタイミングでtoolConfインスタンスを記憶。
        bklConn.setToolConf(toolConf);

        // jp と com とで接続に利用する Configure クラスが異なります。
        BacklogConfigure bklConfig;
        if (toolConf.isBacklogApiIsSiteJp() == false) {
            bklConfig = new BacklogComConfigure(toolConf.getBacklogApiSpaceName()).apiKey(toolConf.getBacklogApiKey());
        } else {
            bklConfig = new BacklogJpConfigure(toolConf.getBacklogApiSpaceName()).apiKey(toolConf.getBacklogApiKey());
        }

        // 取得したクライアントインスタンスを記憶します。
        bklConn.setClient(new BacklogClientFactory(bklConfig).newClient());

        // Backlog API を経由してスペースを読み込めることを確認します。
        Space spc = bklConn.getClient().getSpace();

        // [BMC0001] Space Name:
        RsvrLog.info(BMCMessages.BMC0001 + spc.getName() + " (" + toolConf.getBacklogApiSpaceName() + ")");

        return bklConn;
    }
}
