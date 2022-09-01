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

import com.nulabinc.backlog4j.BacklogClient;

import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolConf;
import consulting.reservoir.backlog.migtool.core.RsvrBacklogMigToolProcessInfo;

/**
 * Backlog API 呼び出しのための接続情報をまとめたクラス。
 */
public class RsvrBacklogApiConn {
    /**
     * BacklogMigToolの構成情報を蓄えるクラス。Backlog API 接続情報や h2 database 格納フォルダなどを指定。
     */
    private RsvrBacklogMigToolConf toolConf = null;

    /**
     * Backlog API 呼び出しのためのクライアントインスタンス。
     */
    private BacklogClient bklClient = null;

    /**
     * BacklogMigToolの動作結果情報を蓄えるクラス。何件レコード追加/更新したのかを保持します。
     */
    private RsvrBacklogMigToolProcessInfo processInfo = new RsvrBacklogMigToolProcessInfo();

    public BacklogClient getClient() {
        return bklClient;
    }

    public void setClient(BacklogClient bklClient) {
        this.bklClient = bklClient;
    }

    public RsvrBacklogMigToolConf getToolConf() {
        return toolConf;
    }

    public void setToolConf(RsvrBacklogMigToolConf toolConf) {
        this.toolConf = toolConf;
    }

    public RsvrBacklogMigToolProcessInfo getProcessInfo() {
        return processInfo;
    }

    ////////////////////////////////////////////////
    // 利便性のための簡易メソッド

    public long getProjectId() {
        return toolConf.getBacklogApiProjectId();
    }
}
