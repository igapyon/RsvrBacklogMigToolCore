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
package consulting.reservoir.backlog.migtool.core.apicall.retryable;

import java.io.IOException;
import java.sql.SQLException;

import com.nulabinc.backlog4j.BacklogAPIException;
import com.nulabinc.backlog4j.BacklogException;

import consulting.reservoir.backlog.migtool.core.BMCMessages;
import consulting.reservoir.backlog.migtool.core.apicall.RsvrBacklogApiConn;
import consulting.reservoir.backlog.migtool.core.util.RsvrBacklogMigToolUtil;

/**
 * リトライ可能なAPI呼び出しのための抽象クラス。
 */
public abstract class AbstractRetryableApiCallout {
    /**
     * API呼び出しをリトライを含めて実施します。
     * 
     * @param bklConn Backlog接続情報。
     * @throws BacklogException Backlog例外が発生した場合。
     * @throws SQLException     SQL例外が発生した場合。
     * @throws IOException      IO例外が発生した場合。
     */
    public void execute(RsvrBacklogApiConn bklConn) throws BacklogException, SQLException, IOException {
        BacklogAPIException lastEx = null;
        for (int retry = 0; retry < 10; retry++) {
            try {
                // 実際のAPI呼び出し処理を記述します。
                processApiCallout(bklConn);

                // API呼び出しインターバルをsleepします。
                RsvrBacklogMigToolUtil.sleepApiInterval(bklConn.getToolConf());

                // 正常終了できたのでリトライループを離脱。
                return;
            } catch (BacklogAPIException ex) {
                if (ex.getStatusCode() == 429) {
                    // [BMC5501] Backlog API Rate Limit Exceed が出たので待機後ふたたび挑戦します。
                    System.err.println(BMCMessages.BMC5501);

                    RsvrBacklogMigToolUtil.sleepApiRateLimitExceedRetryInterval();
                    lastEx = ex;
                } else {
                    // 429以外の場合はリトライできません。そのままスローします。
                    throw ex;
                }
            }
        }
        throw lastEx;
    }

    /**
     * 実際の処理を記述。
     * 
     * @throws BacklogException Backlog例外が発生した場合。
     * @throws SQLException     SQL例外が発生した場合。
     * @throws IOException      IO例外が発生した場合。
     */
    abstract void processApiCallout(RsvrBacklogApiConn bklConn) throws BacklogException, SQLException, IOException;
}
