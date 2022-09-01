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
 * BacklogMigTool Core のメッセージ。
 */
public class BMCMessages {
    // [BMC0001] Space Name:
    public static final String BMC0001 = "[BMC0001] Space Name: ";

    // [BMC0002] Project Name:
    public static final String BMC0002 = "[BMC0002] Project Name: ";

    // [BMC0003] Target Export (Prepare):
    public static final String BMC0003 = "[BMC0003] Target Export (Prepare): ";

    // [BMC1101] 期待したコメント数と、実際に取得できたコメント数とが異なる
    public static final String BMC1101 = "[BMC1101] 期待したコメント数と、実際に取得できたコメント数とが異なる";

    // TODO TBD 重要なメッセージをここにまとめていくこと。

    ////////////////////
    // IMPORT

    // [BMC5101] Import: Issue: created.
    public static final String BMC5101 = "[BMC5101] Import: Issue: created.";

    // [BMC5102] Import: Issue: Import cannot proceed because issue(s) already
    // exists in the project. Processing will be aborted. issue count:
    public static final String BMC5102 = "[BMC5102] Import: Issue: Import cannot proceed because issue(s) already exists in the project. Processing will be aborted. issue count: ";

    // [BMC5103] Import: Issue: Warn: Import warn because issue(s) already exists in
    // the project. Processing will be continued (-forceimp). issue count:
    public static final String BMC5103 = "[BMC5103] Import: Issue: Warn: Import warn because issue(s) already exists in the project. Processing will be continued (-forceimp). issue count: ";

    // [BMC5104] Import: Issue: Warn: 非本番モードであるのに、ProjectKey が MIGTEST
    // から始まる名前ではありません。非本番モードでは ProjectKey は MIGTEST で開始するようにしてください.
    public static final String BMC5104 = "[BMC5104] Import: Issue: Warn: 非本番モードであるのに、ProjectKey が MIGTEST から始まる名前ではありません。非本番モードでは ProjectKey は MIGTEST で開始するようにしてください. 処理スキップします.";

    public static final String MBC5105 = "[MBC5105] No comment content occured: 変更コメントに ((移行の結果差分なし)) と加えてリトライ。";

    // ((移行の結果差分なし)) ※このメッセージにはIDを含まない。
    public static final String MBC5106 = "((移行の結果差分なし))";

    // 【削除済み】【欠番】この課題は削除済みのものです。データ移行の都合で作成された空チケット ※このメッセージにはIDを含まない。
    public static final String MBC5107 = "【削除済み】【欠番】この課題は削除済みのものです。データ移行の都合で作成された空チケット";

    // [MBC5111] import: Mapping User: 新旧ユーザをメールアドレスをもとに引き当て:
    public static final String MBC5111 = "[MBC5111] import: Mapping User: 新旧ユーザをメールアドレスをもとに引き当て: ";

    // [MBC5112] import: Mapping User: 新旧ユーザを名前をもとに引き当て:
    public static final String MBC5112 = "[MBC5112] import: Mapping User: 新旧ユーザを名前をもとに引き当て: ";

    // [MBC5113] import: Mapping User: ユーザマッピング状況は以下のようになります。
    public static final String MBC5113 = "[MBC5113] import: Mapping User: ユーザマッピング状況は以下のようになります。";

    // [MBC5114] import: Mapping User: ターゲット Backlog ユーザへの割り当てが未実施のユーザが存在します。未割当数:
    public static final String MBC5114 = "[MBC5114] import: Mapping User: ターゲット Backlog ユーザへの割り当てが未実施のユーザが存在します。未割当数: ";

    // [MBC5115] import: Mapping User: h2 database
    // の「BacklogMappingUser」テーブルを編集してすべてのユーザマッピングを割当済にしてください。
    public static final String MBC5115 = "[MBC5115] import: Mapping User: h2 database の「BacklogMappingUser」テーブルを編集してすべてのユーザマッピングを割当済にしてください。";

    // [MBC5116] マッピング未割当を解消してから Import Phase4 以降を実施してください。";
    public static final String MBC5116 = "[MBC5116] マッピング未割当を解消してから Import Phase4 以降を実施してください。";

    // [MBC5117] マッピングはすべて割当済みです。Import Phase4 以降を実施が可能です。
    public static final String MBC5117 = "[MBC5117] マッピングはすべて割当済みです。Import Phase4 以降を実施が可能です。";

    public static final String BMC5202 = "[BMC5202] Import: Category: Import cannot proceed because category(s) already exists in the project. Processing will be aborted. category count: ";

    // これを生かして、他のものは除去します。
    public static final String BMC5204 = "[BMC5204] Import: Warn: 非本番モードであるのに、ProjectKey が MIGTEST から始まる名前ではありません。非本番モードでは ProjectKey は MIGTEST で開始するようにしてください. 処理スキップします.";

    public static final String BMC5302 = "[BMC5302] Import: Milestone: Import cannot proceed because milestone(s) already exists in the project. Processing will be aborted. milestone count: ";

    public static final String BMC5304 = "[BMC5304] Import: Milestone: Warn: 非本番モードであるのに、ProjectKey が MIGTEST から始まる名前ではありません。非本番モードでは ProjectKey は MIGTEST で開始するようにしてください. 処理スキップします.";

    public static final String BMC5402 = "[BMC5402] Import: Version: Import cannot proceed because version(s) already exists in the project. Processing will be aborted. version count: ";

    public static final String BMC5404 = "[BMC5404] Import: Version: Warn: 非本番モードであるのに、ProjectKey が MIGTEST から始まる名前ではありません。非本番モードでは ProjectKey は MIGTEST で開始するようにしてください. 処理スキップします.";

    // [BMC5501] Backlog API Rate Limit Exceed が出たので待機後ふたたび挑戦します。
    public static final String BMC5501 = "[BMC5501] Backlog API Rate Limit Exceed が出たので待機後ふたたび挑戦します。";

    public static final String BMC5502 = "[BMC5502] Import: IssueType: Import cannot proceed because issueType(s) seems already exists in the project. Processing will be aborted. issueType count: ";

    public static final String BMC5504 = "[BMC5504] Import: IssueType: Warn: 非本番モードであるのに、ProjectKey が MIGTEST から始まる名前ではありません。非本番モードでは ProjectKey は MIGTEST で開始するようにしてください. 処理スキップします.";

    public static final String BMC5602 = "[BMC5602] Import: IssueStatusType: Import cannot proceed because issueType(s) seems already exists in the project. Processing will be aborted. issueStatusType count: ";

    public static final String BMC5604 = "[BMC5604] Import: IssueStatusType: Warn: 非本番モードであるのに、ProjectKey が MIGTEST から始まる名前ではありません。非本番モードでは ProjectKey は MIGTEST で開始するようにしてください. 処理スキップします.";

    public static final String BMC5801 = "[BMC5801] Import: Wiki: created.";

    public static final String BMC5804 = "[BMC5804] Import: Wiki: Warn: 非本番モードであるのに、ProjectKey が MIGTEST から始まる名前ではありません。非本番モードでは ProjectKey は MIGTEST で開始するようにしてください. 処理スキップします.";

    public static final String BMC5901 = "[BMC5901] -forceproduction と -forceimport とを同時に指定することはできません。";
}
