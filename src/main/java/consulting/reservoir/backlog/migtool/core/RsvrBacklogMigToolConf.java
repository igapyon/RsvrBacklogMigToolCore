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
package consulting.reservoir.backlog.migtool.core;

/**
 * BacklogMigToolの構成情報を蓄えるクラス。Backlog API 接続情報や h2 database 格納フォルダなどを指定できます。
 */
public class RsvrBacklogMigToolConf {
    private boolean backlogApiIsSiteJp = false;
    private String backlogApiSpaceName = "nospacename";
    private String backlogApiKey = "noapikey";
    private long backlogApiProjectId = -1;
    private String backlogApiProjectKey = "nokeyname";

    private int apiInterval = 1000;

    /**
     * DBファイル名はこれに /backlogDb を付与したもの。
     */
    private String dirDb = "./target/backlogmig/db";
    // private String dirDb = "./src/main/resources/db/";

    private String dirExpAttachment = "./target/backlogmig/res/attachment";
    // private String dirExpAttachment = "./src/main/resources/static/attachment";
    private String dirExpFile = "./target/backlogmig/res/file";
    // private String dirExpFile = "./src/main/resources/static/file";
    private String dirExpWikiAttachment = "./target/backlogmig/res/wikiattachment";

    private boolean isDebug = false;

    /**
     * Backlog API の接続先が .com か .jp のいずれかを取得。
     * 
     * @return .jp サイトであれば true。
     */
    public boolean isBacklogApiIsSiteJp() {
        return backlogApiIsSiteJp;
    }

    /**
     * Backlog API の接続先が .com か .jp のいずれかを設定。
     * 
     * @param backlogApiIsSiteJp .jp サイトであれば true。
     */
    public void setBacklogApiIsSiteJp(boolean backlogApiIsSiteJp) {
        this.backlogApiIsSiteJp = backlogApiIsSiteJp;
    }

    /**
     * Backlog API の接続先のスペース名を取得します。これはURLのサブドメインに相当します。
     * 
     * @return スペース名。
     */
    public String getBacklogApiSpaceName() {
        return backlogApiSpaceName;
    }

    /**
     * Backlog API の接続先のスペース名を設定します。これはURLのサブドメインに相当します。
     * 
     * @param backlogApiSpaceName スペース名。
     */
    public void setBacklogApiSpaceName(String backlogApiSpaceName) {
        this.backlogApiSpaceName = backlogApiSpaceName;
    }

    /**
     * Backlog API の接続時の API Key を取得します。
     * 
     * @return API Key。
     */
    public String getBacklogApiKey() {
        return backlogApiKey;
    }

    /**
     * Backlog API の接続時の API Key を取得します。
     * 
     * @param backlogApiKey API Key。
     */
    public void setBacklogApiKey(String backlogApiKey) {
        this.backlogApiKey = backlogApiKey;
    }

    /**
     * Backlog API の接続先の ProjectId を取得します。
     * 
     * @return ProjectId
     */
    public long getBacklogApiProjectId() {
        return backlogApiProjectId;
    }

    /**
     * Backlog API の接続先の ProjectId を設定します。
     * 
     * @param backlogApiProjectId ProjectId
     */
    public void setBacklogApiProjectId(long backlogApiProjectId) {
        this.backlogApiProjectId = backlogApiProjectId;
    }

    /**
     * Backlog の接続先 Project Key を取得します。基本的に RsvrBacklogMigTool は
     * ProjectIdで動作し、Project Key が有効なのは一時的なものです。
     * 
     * @return Backlog の Project Key。
     */
    public String getBacklogApiProjectKey() {
        return backlogApiProjectKey;
    }

    /**
     * Backlog の接続先 Project Key を設定します。基本的に RsvrBacklogMigTool は
     * ProjectIdで動作し、Project Key が有効なのは一時的なものです。
     * 
     * @param backlogApiProjectKey Backlog の Project Key。
     */
    public void setBacklogApiProjectKey(String backlogApiProjectKey) {
        this.backlogApiProjectKey = backlogApiProjectKey;
    }

    /**
     * h2 database の実ファイルの配置先ディレクトリ名を取得します。
     * 
     * @return h2 database の配置先ディレクトリ名。
     */
    public String getDirDb() {
        return dirDb;
    }

    /**
     * h2 database の実ファイルの配置先ディレクトリ名を設定します。
     * 
     * @param dirDb h2 database の配置先ディレクトリ名。
     */
    public void setDirDb(String dirDb) {
        this.dirDb = dirDb;
    }

    /**
     * h2 database の実ファイルの配置先ディレクトリ名を含めたデータベースファイル名を取得します。
     * 
     * @return h2 database のデータベースファイル名。
     */
    public String getDirDbPath() {
        return getDirDb() + "/backlogDb";
    }

    /**
     * エクスポートした課題の添付ファイルの配置先ディレクトリ名を取得します。
     * 
     * @return 添付ファイルの配置先ディレクトリ名。
     */
    public String getDirExpAttachment() {
        return dirExpAttachment;
    }

    /**
     * エクスポートした課題の添付ファイルの配置先ディレクトリ名を設定します。
     * 
     * @param dirExpAttachment 添付ファイルの配置先ディレクトリ名。
     */
    public void setDirExpAttachment(String dirExpAttachment) {
        this.dirExpAttachment = dirExpAttachment;
    }

    /**
     * エクスポートしたファイルの配置先ディレクトリ名を取得します。
     * 
     * @return ファイルの配置先ディレクトリ名。
     */
    public String getDirExpFile() {
        return dirExpFile;
    }

    /**
     * エクスポートしたファイルの配置先ディレクトリ名を設定します。
     * 
     * @param dirExpFile ファイルの配置先ディレクトリ名。
     */
    public void setDirExpFile(String dirExpFile) {
        this.dirExpFile = dirExpFile;
    }

    /**
     * 各 Backlog API 呼び出しの間隔をミリ秒で取得します。
     * 
     * @return 呼び出し間隔 (ミリ秒)
     */
    public int getApiInterval() {
        return apiInterval;
    }

    /**
     * 各 Backlog API 呼び出しの間隔をミリ秒で設定します。
     * 
     * @param apiInterval 呼び出し間隔 (ミリ秒)
     */
    public void setApiInterval(int apiInterval) {
        this.apiInterval = apiInterval;
    }

    /**
     * エクスポートした Wiki添付ファイルの配置先ディレクトリ名を取得します。
     * 
     * @return dirExpWikiAttachment Wiki添付ファイルの配置先ディレクトリ名。
     */
    public String getDirExpWikiAttachment() {
        return dirExpWikiAttachment;
    }

    /**
     * エクスポートした Wiki添付ファイルの配置先ディレクトリ名を設定します。
     * 
     * @param dirExpWikiAttachment Wiki添付ファイルの配置先ディレクトリ名。
     */
    public void setDirExpWikiAttachment(String dirExpWikiAttachment) {
        this.dirExpWikiAttachment = dirExpWikiAttachment;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }
}
