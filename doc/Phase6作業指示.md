# Phase 6 作業指示

作成: 2026-06-02

---

## 現在地

- Phase 1〜5 完了・main ブランチにコミット済み
- 次フェーズ: Phase 6 = UI 層新規構築

---

## 方針

旧プロジェクト（`E:\programing\Android\SimpleCustomLauncher`）のコードは**移植しない**。  
参考として読みながらゼロから新規構築する。

---

## 作業前に必ず読むもの

1. `doc/目標アーキテクチャ図.md`（本プロジェクト）
2. `doc/リファクタリング方針.md`（本プロジェクト）
3. 旧プロジェクト `MainActivity.kt` — Intent 処理・ライフサイクルの参考
4. 旧プロジェクト `ui/screens/` 以下の各画面 — 機能の参考

---

## 実装対象と配置先

| ファイル | 配置先 | 備考 |
|---|---|---|
| `MainActivity.kt` | `app/` | 薄い NavHost。NavDestination → Composable の対応表のみ |
| `HomeScreen.kt` | `feature/launcher/home/` | ホーム画面グリッド・編集モード |
| `ShortcutSelectScreen.kt` | `feature/launcher/shortcutselect/` | ショートカット選択・配置 |
| `SettingsScreen.kt` | `feature/screens/settings/` | アプリ設定（旧 AppSettingsScreen） |
| `CalendarFullScreen.kt` | `feature/screens/calendar/` | カレンダー |
| `MemoScreen.kt` | `feature/screens/memo/` | メモ帳 |
| `AllAppsScreen.kt` | `feature/screens/allapps/` | 全アプリ一覧 |
| 共有ダイアログ・部品 | `ui/components/` | LargeConfirmDialog・TermsConsentDialog 等 |

---

## 旧コードとの重要な差分

| 項目 | 旧コード | 新コード |
|---|---|---|
| ナビゲーション状態保持 | `screenState`（sealed class） | `homeViewModel.navDestination`（NavDestination） |
| 設定画面の NavDestination | `NavDestination.AppSettings` | `NavDestination.Settings` |
| SlotEdit 画面 | 存在する | **存在しない**（ShortcutSelect に統合） |
| ViewModel の場所 | `ui/` | `feature/launcher/home/`・`feature/launcher/shortcutselect/` |
| ViewModel 間の連携 | コールバック | Repository の StateFlow 経由（コールバックなし） |
| 画面ルーティング | MainActivity の巨大 when | NavHost（薄い対応表） |

---

## 作業手順

1. 上記の設計書と旧コードを読む
2. **MainActivity（NavHost の骨格）** を最初に実装
   - NavDestination の when 分岐のみ。太らせない
   - BillingManager・AdManager の初期化
   - homeIntent / packageRemoved の検知
   - テーマ・壁紙設定
3. 以降の各画面は **「提示 → ユーザー確認 → 実装」** の順で1画面ずつ進める
4. 各実装後にビルドが通ることを確認してからコミット

---

## 参照ファイル（旧プロジェクト）

```
E:\programing\Android\SimpleCustomLauncher\app\src\main\java\com\coelacanth9\simplecustomlauncher\
├── MainActivity.kt
└── ui\screens\
    ├── HomeScreen.kt
    ├── ShortcutSelectScreen.kt
    ├── AppSettingsScreen.kt
    ├── CalendarFullScreen.kt
    ├── MemoScreen.kt
    └── AllAppsScreen.kt
```
