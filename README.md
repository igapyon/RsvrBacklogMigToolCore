# RsvrBacklogMigTool Core

`RsvrBacklogMigTool Core` は、Backlog のデータをマイグレーションする際に役立つツールを提供するために必要となるコア機能を実装したものです。ライブラリの形式となっているため、これ単体では特に役立つ独立した機能を提供するものではありません。
`RsvrBacklogMigTool Core` を組み込んだ他のプロダクトを利用するか、あるいは自作のアプリで `RsvrBacklogMigTool Core` を組み込んで使用するなどが想定されます。

- RsvrBacklogMigTool は Java で実装されており、ソースコードからのビルド実行などには Java、Maven が環境として必要です。
- Backlog API のバージョンの都合により、Java は 1.8 を使用することが必要です。

## RsvrBacklogMigTool の特徴

`RsvrBacklogMigTool` は以下の点が特徴と考えます。

- エクスポートとインポートを別々に実施できます。
- データ移行作業をフェーズにわけて実施します。いくつかのフェーズは意図的にスキップすることも可能です。
- ソースコードが提供されるため、プログラミング技術のあるひとは書き変えて自分の都合に合わせたツールに作り替えることが可能です。

## RsvrBacklogMigTool Core に含まれるもの

RsvrBacklogMigTool Core には以下の機能が含まれます。

- Backlog からデータをエクスポートする機能
    - エクスポートしたデータは h2 database およびファイルシステムに格納
    - Issue (課題) 、ファイル、Wiki のエクスポートに対応
- Backlog にデータをインポートする機能
    - エクスポート済みデータを新規Backlogプロジェクトにインポートします。
    - 新規Backlogプロジェクト向けのみでインポートすることが可能です
    - Issue (課題)、Wiki、IssueType、Category、Milestone、Version などのインポートに対応

## Phase (処理のフェーズ)

`RsvrBacklogMigTool` は Backlog データのマイグレーションを以下のフェーズに分割して実施する前提で作られています。

- Phase1: User や Category、IssueType といったマスター類
- Phase2: Userなどマッピング表が必要なものについての確認. Phase4以降の実施の前には Phase2を都度実施を推奨
- Phase3: ファイル (ファイルはインポートの実装は含まない)
- Phase4: Wiki
- Phase5: 課題
- Phase6: 後処理 (課題の親の付け替え等)
- Phase7: その他のカスタム処理（カスタム属性の調整などを想定）。なお現在は該当実装は存在せず、プレイスホルダーとなっています。

基本的には、エクスポートを Phase1, 2, 3, 4, 5, 6 の順で実施し、その後に インポートを Phase1, 2, 4, 5, 6 の順で処理を進めていくことを基本的な流れと前提して作られています。

## RsvrBacklogMigTool Core の使用方法

`backlogmigtool.core` プロジェクトを入手して `mvn clean install` を実施することにより、ライブラリをローカルマシンにインストールすることが可能です。

```
mvn clean install
```

※`backlogmigtool.core` はそれ単体では特に役立つ機能を提供するものではない点に注意。

# 制約

## EXPORT

- 主要なデータのみエクスポートを行います。(完全なものではない点に注意してください)

## IMPORT

- 作った直後の Backlog プロジェクトに対してのみインポートが可能です。すでに課題が存在するプロジェクトにはインポートできません。
- デフォルトの非本番環境モードでは MIGTEST から始まるプロジェクト名のプロジェクトにのみインポートが可能です。移行テストを実施して手順や内容を確認できてからはじめて本番環境モードでインポートを実施してください

# TODO

- コメントの更新日などを後付けで設定可能か調べたい。（少し探したものの、、、おそらくAPIが存在しない）
- 課題タイプ、色も欲しい
- Wikiについてタグに対応してほしい。
- IMPORT: Category, Milestone, Version の項目値を除去する操作系統が、IMPORTの Backlog API 経由にてうまく反映されない。そもそもAPIの呼び方が正しくないのかもしれないが。。。

