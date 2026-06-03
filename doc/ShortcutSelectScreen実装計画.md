# ShortcutSelectScreen 実装計画

作成: 2026-06-03

---

## 概要

旧プロジェクトの `ShortcutAddScreen` + `SlotEditScreen` を1画面に統合する。  
選択結果は `ShortcutRepository` へ書き込み、`HomeViewModel` は `layoutState` StateFlow 経由で自動反映される。

---

## 変更ファイル一覧

| # | ファイル | 種別 |
|---|---|---|
| 1 | `ui/components/ContactTypeDialog.kt` | 新規 |
| 2 | `feature/launcher/shortcutselect/ShortcutSelectViewModel.kt` | 追記 |
| 3 | `feature/launcher/shortcutselect/ShortcutSelectViewModelFactory.kt` | 追記 |
| 4 | `MainActivity.kt` | 小修正 |
| 5 | `feature/launcher/shortcutselect/ShortcutSelectScreen.kt` | 全面置換 |

---

## 各ファイルの変更内容

### 1. `ui/components/ContactTypeDialog.kt`（新規）

旧プロジェクト `ui/components/ContactShortcutDialog.kt` を移植。  
「電話 / SMS / Link通話 / LinkSMS」選択ダイアログ。

```kotlin
fun ContactTypeDialog(
    contactName: String,
    onSelectPhone: () -> Unit,
    onSelectSms: () -> Unit,
    onSelectLinkPhone: (() -> Unit)? = null,
    onSelectLinkSms: (() -> Unit)? = null,
    onDismiss: () -> Unit
)
```

---

### 2. `ShortcutSelectViewModel.kt`（追記）

#### コンストラクタ追加

```kotlin
class ShortcutSelectViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val shortcutHelper: ShortcutHelper,
    private val premiumManager: PremiumManager,   // ← 追加
    val targetPageIndex: Int,
    val targetRow: Int,
    val targetColumn: Int
)
```

#### 追加プロパティ

```kotlin
// Repository の StateFlow をそのまま公開（画面が collectAsState できるように）
val layoutState: StateFlow<LayoutState> = shortcutRepository.layoutState

// PremiumManager をラップ（platform 層なので feature→feature 依存なし）
val isPremium: Boolean get() = premiumManager.isPremiumActive()
```

#### 追加メソッド

| メソッド | 内容 |
|---|---|
| `clearSlot()` | 現スロットの配置を削除。`shouldDeleteOnRemove` に従いショートカット本体も削除 |
| `changeColumns(columns: Int)` | 行の分割数変更。はみ出た列・Link系ショートカットを削除 |
| `changeTextOnly(textOnly: Boolean)` | 行のテキストのみモード切替 |
| `changeColors(backgroundColor?, textColor?)` | 現スロットの Placement の背景色・文字色を更新 |
| `deleteRow()` | 行の全 Placement 削除 + RowConfig 削除 |
| `deleteUnplacedShortcut(shortcut)` | 未配置ショートカットを `deleteShortcut()` で完全削除 |

---

### 3. `ShortcutSelectViewModelFactory.kt`（追記）

```kotlin
class ShortcutSelectViewModelFactory(
    private val shortcutRepository: ShortcutRepository,
    private val shortcutHelper: ShortcutHelper,
    private val premiumManager: PremiumManager,   // ← 追加
    private val targetPageIndex: Int,
    private val targetRow: Int,
    private val targetColumn: Int
)
```

---

### 4. `MainActivity.kt`（小修正）

`MainLauncherScreen` 内で `premiumManager` を1回だけ生成し、  
`ShortcutSelectViewModelFactory` に渡す。

```kotlin
// MainLauncherScreen の remember 群に追加（settingsRepository は既存）
val premiumManager = remember { DefaultPremiumManager(context, settingsRepository) }

// ShortcutSelect の when 分岐
factory = ShortcutSelectViewModelFactory(
    shortcutRepository = shortcutRepository,
    shortcutHelper = shortcutHelper,
    premiumManager = premiumManager,   // ← 追加
    targetPageIndex = dest.pageIndex,
    targetRow = dest.row,
    targetColumn = dest.column
)
```

`ShortcutSelectScreen` の呼び出しシグネチャは変更なし。

---

### 5. `ShortcutSelectScreen.kt`（全面置換）

#### 画面シグネチャ（変更なし）

```kotlin
fun ShortcutSelectScreen(
    viewModel: ShortcutSelectViewModel,
    onBack: () -> Unit
)
```

#### 派生ステート（`layoutState.collectAsState()` から導出）

```kotlin
val layoutState by viewModel.layoutState.collectAsState()

val currentShortcut     // 現スロットの ShortcutItem?（null = 空スロット）
val currentPlacement    // 現スロットの ShortcutPlacement?（背景色等）
val currentRowConfig    // RowConfig?（columns, textOnly）
val unplacedShortcuts   // 配置されていない ShortcutItem リスト
val currentColumns      // = currentRowConfig?.columns ?: 2
val currentTextOnly     // = currentRowConfig?.textOnly ?: false
val currentBackgroundColor / currentTextColor  // = currentPlacement?.backgroundColor 等
val hasLinkShortcutsInRow  // 行内に Link 系ショートカットがあるか
```

#### 画面遷移ステート

```kotlin
sealed class SelectScreenState {
    object Main : SelectScreenState()
    object AppList : SelectScreenState()
    data class AppShortcuts(val app: AppInfo) : SelectScreenState()
}
```

TopAppBar の戻るボタンで `AppShortcuts → AppList → Main → onBack()` の順に遷移。

#### メインコンテンツ（LazyColumn）

1. **▶ アプリ一覧から選ぶ**（NavigationCard → AppList 遷移）
2. **連絡先から追加**（NavigationCard → 連絡先ピッカー起動）
3. **セクション: アプリ内機能**  
   カレンダー / メモ帳 / 電話 / アプリ一覧 / 日付 / 時刻 / 端末設定
4. **楽天Link機能**（楽天Linkインストール時 かつ 列数 < 3 のみ）  
   LinkSMS一覧 / Link通話一覧 / 電話（Link）
5. **一時保管**（未配置ショートカット一覧、削除ボタン付き）
6. ─── 区切り線 ───
7. **表示モード切替**（アイコン+ラベル ↔ ラベルのみ）
8. **背景色・文字色**（プレミアム機能 → 未購入時はロック + InfoDialog）
9. **分割数変更**（ActionCard → ColumnsDialog）
10. **スロットを空にする**（`currentShortcut != null` の場合のみ表示）
11. **行を削除**

#### ダイアログ一覧

| ダイアログ | トリガー |
|---|---|
| `ContactTypeDialog` | 連絡先ピッカー後（電話/SMS/Link通話/LinkSMS 選択） |
| 端末設定選択ダイアログ | "端末設定" タップ |
| 分割数変更ダイアログ | ActionCard "分割数変更" タップ |
| Link 削除警告ダイアログ | 3列変更時に Link 系ショートカットが存在 |
| 未配置削除確認 | ShortcutCard の削除ボタン |
| スロットを空にする確認 | ActionCard "スロットを空にする" タップ |
| 行削除確認（`RowDeleteConfirmDialog`） | ActionCard "行を削除" タップ |
| プレミアム機能 `InfoDialog` | ColorSetCard タップ（未購入時） |
| Link SMS エラーダイアログ | LinkSMS ショートカット作成失敗時 |

#### プレミアム処理方針

- `viewModel.isPremium` で判定
- 未購入時：`InfoDialog("色の変更はプレミアム機能です。\n設定画面から購入できます。")` を表示するのみ
- このスクリーン内での購入・広告フロー = **なし**（Settings 画面で行う）
- `PremiumManager` は `platform/billing` 層なので feature → feature 依存は発生しない

---

## アーキテクチャ上の注意点

| 項目 | 方針 |
|---|---|
| feature → feature 依存 | なし。`PremiumManager` は `platform` 層 |
| ViewModel 間通信 | Repository StateFlow 経由（直接参照なし） |
| 購入フロー | SettingsScreen に委譲（ShortcutSelect は読み取り専用） |
| 画面シグネチャ | `(viewModel: ShortcutSelectViewModel, onBack: () -> Unit)` で固定 |
| リアクティブ更新 | `layoutState.collectAsState()` → `remember(layoutState)` で派生 |
