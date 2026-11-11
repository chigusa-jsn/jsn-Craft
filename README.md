# jsn-Craft

MMO風の職業システムを追加する Minecraft MOD（NeoForge 1.21.1）。

## 目標（全体像）

- 1人が持てるのは **生活ジョブ1つ + 戦闘ジョブ1つ**。いつでも転職できる
- 生活ジョブ10種・戦闘ジョブ10種、それぞれの上位職10種ずつ、合体職10種ほど（合計約50）
- 頭の上に **肩書 / 戦闘ジョブとレベル / 生活ジョブとレベル** の3段表示

## いまできること（v0.1）

- 職業を選ぶ・転職する（前の職業のレベルは残る）
- 行動で経験値が入り、レベルが上がる
- 上位職の転職条件（例：木こり Lv.20 で「森の匠」に転職できる）
- 頭上の3段表示（周りのプレイヤーにも見える）
- データはセーブされ、死んでも消えない

サンプル職業：木こり（生活）、森の匠（生活・上位職）、戦士（戦闘）

## 動かし方

1. **IntelliJ IDEA Community** をインストール
2. IntelliJ で「開く」→ このフォルダ（`build.gradle` がある場所）を選ぶ
3. 初回は依存ファイルのダウンロードに数分かかるので待つ
4. 右側の Gradle パネル → `Tasks > mod development > runClient` をダブルクリック

コマンドで動かす場合：`./gradlew runClient`（Windows は `gradlew.bat runClient`）

## コマンド

| コマンド | 内容 |
|---|---|
| `/job` | 自分の職業とレベルを見る |
| `/job list` | 職業の一覧 |
| `/job change lumberjack` | 転職（Tabキーで候補が出る） |
| `/job title @s 森の守護者` | 肩書を付ける（OP） |
| `/job title @s clear` | 肩書を外す（OP） |
| `/job addxp life 500` | 経験値を足す（テスト用・OP） |
| `/job board`（または **Kキー**） | 職業メニュー（ステータス・戦闘ボード・生活ボード） |
| `/job resetboard combat` | スキルボードをリセット（テスト用・OP） |

## スキルボード

職業レベルが1上がるごとにスキルポイント（SP）が1もらえる。SPは職業ごとで、その職業のボードにだけ使える。
能力が上がるのは、今就いている職業のボードだけ（転職するとお休み、戻ると復活）。

ボードは職業JSONの `board` に書く。ツリー型も盤面型も同じ書き方。

```json
{ "id": "power1", "x": -2, "y": 1, "cost": 1, "links": ["root"],
  "name": "鋭刃 I", "icon": "minecraft:stone_sword",
  "effects": [ { "attribute": "minecraft:generic.attack_damage", "amount": 0.5 } ] }
```

| 項目 | 意味 |
|---|---|
| `x`, `y` | 盤面上の位置 |
| `start` | `true` なら最初から解放できる |
| `links` | つながっているマス。どれかを解放済みなら、このマスも解放できる |
| `cost` | 必要なSP |
| `effects` | 上がる能力。`operation` を `add_multiplied_total` にすると「○%アップ」 |

自分の頭上表示は **F5（三人称視点）** で確認できる。

## 職業の増やし方

`src/main/resources/data/jsn_craft/jsn_jobs/` に JSON ファイルを1つ足すだけ。Java は書かなくていい。

```json
{
  "name": { "text": "採掘師", "color": "gray" },
  "type": "life",
  "tier": "basic",
  "max_level": 50,
  "requires": [],
  "xp": [
    { "action": "break_block", "target": "minecraft:diamond_ore", "xp": 30 },
    { "action": "break_block", "target": "#minecraft:base_stone_overworld", "xp": 1 }
  ]
}
```

| 項目 | 意味 |
|---|---|
| `name` | 表示名。色も付けられる |
| `type` | `life`（生活） / `combat`（戦闘） |
| `tier` | `basic`（基本職） / `advanced`（上位職） / `hybrid`（合体職） |
| `max_level` | 最大レベル |
| `requires` | 転職条件。合体職なら2つ書く |
| `xp` | 経験値のもらえる行動。上から順に見て、最初に当てはまったものだけ使う |

`action` は今のところ `break_block`（ブロックを壊す）と `kill_entity`（モブを倒す）。
`target` は ID（`minecraft:stone`）、タグ（`#minecraft:logs`）、何でも（`*`）。

## 依頼（クエスト）

Kキーのメニューの「依頼」タブで、受ける・報告する・あきらめるができる。同時に受けられるのは5件まで。
依頼でもらった肩書は `/job mytitle <肩書>` で付け替えられる（`/job mytitle clear` で外す）。

依頼は `src/main/resources/data/jsn_craft/jsn_quests/` に JSON を1つ足すだけ。
サーバーで運営が追加するときは、データパックの `data/<名前>/jsn_quests/` に置いて `/reload` すれば、止めずに増やせる。

```json
{
  "title": { "text": "【特別依頼】収穫祭" },
  "description": { "text": "カボチャを集めよう" },
  "category": "special",
  "repeat": "once",
  "start": "2026-11-01T00:00",
  "end": "2026-12-31T23:59",
  "objectives": [
    { "type": "deliver_item", "target": "minecraft:pumpkin", "count": 16 }
  ],
  "rewards": {
    "job_xp": [ { "job": "life", "amount": 300 } ],
    "items": [ { "id": "minecraft:golden_apple", "count": 1 } ],
    "title": "収穫祭の英雄"
  }
}
```

| 項目 | 意味 |
|---|---|
| `category` | `normal`（通常） / `job`（職業依頼） / `special`（運営の特別依頼） |
| `required_job`, `min_level` | 職業依頼の条件。その職業に就いていて、レベルが足りている人だけ |
| `repeat` | `once`（1回だけ） / `daily`（1日1回） / `always`（何度でも） |
| `start`, `end` | 受けられる期間。サーバーのPCの時刻で判定。書かなければいつでも |
| `objectives` | 目標。`break_block`（壊す） / `kill_entity`（倒す） / `deliver_item`（納品） / `reach_level`（職業レベル） |
| `label` | 目標の説明文（省略すると自動） |
| `rewards` | `job_xp`（`life` / `combat` / 職業ID）、`items`、`title`（肩書） |

## 受付嬢NPC

右クリックすると、依頼タブが開いた状態でメニューが出る。攻撃されても倒れず、その場から動かない。

- 置く：クリエイティブの「スポーンエッグ」タブにある「受付嬢のスポーンエッグ」、または `/summon jsn_craft:receptionist`
- 片付ける：クリエイティブでスニークしながら殴る、または `/kill`
- 見た目：`src/main/resources/assets/jsn_craft/textures/entity/receptionist.png`（アレックス型・64×64）を差し替える
