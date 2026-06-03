# リファクタリング Phase7 作業指示

作成: 2026-06-04

## 目的

`アーキテクチャ設計方針.md` に基づき、UseCase 層を新設して各層の責務を正しく分離する。

---

## 実装ステップ

### Step 1: ShortcutRepository — 準備

**追加**
- `isUpdating: AtomicBoolean` フィールド追加
- `update {}` メソッド追加
- `notifyChange()` に `if (isUpdating.get()) return` チェック追加

**削除（デッドコード）**
- `getShortcutsByRow()`
- `getShortcutsByRowForPage()`

---

### Step 2: ShortcutReceiverActivity — バグ修正

`saveShortcutInfo()` が `pin_shortcuts` SharedPreferences に直接書いているのを
`repository.savePinShortcutInfo()` を使うよう修正して削除。

---

### Step 3: UseCase 6ファイル作成

パッケージ: `com.coelacanth9.simplecustomlauncher.usecase`

#### ApplyDefaultLayoutUseCase
- `DefaultLayoutConfig.kt` の内容（`ItemDef` / `itemMapping` / `defaultLayout`）を同梱
- 移動元: `ShortcutRepository.applyDefaultLayout()` / `createShortcutFromDef()` / `resetToDefault()` / `getFixedHeightForRow()`
- 依存: `ShortcutRepository`, `Context`

#### AddShortcutUseCase
- 移動元: `ShortcutRepository.addShortcutToFirstEmpty()`
- `repository.update {}` で `saveShortcut` + `savePlacement` をバッチ化
- 依存: `ShortcutRepository`

#### RowUseCase
- 移動元: `HomeViewModel.deleteRow()` / `insertRowAt()` / `changeRowColumns()`
- 各操作を `repository.update {}` でラップ
- 依存: `ShortcutRepository`

#### DeletePageUseCase
- 移動元: `HomeViewModel.deletePage()` + `ShortcutRepository.clearPageLayout()` の内容
- `repository.update {}` で配置・行・ショートカット削除をバッチ化
- 依存: `ShortcutRepository`, `SettingsRepository`

#### CleanupUseCase
- 移動元: `HomeViewModel.cleanupOrphanedPinShortcuts()` / `cleanupUninstalledPackages()` / `onPackageRemoved()`
- LauncherApps 操作（unpin）は `ShortcutHelper` に新規メソッドとして追加し、UseCase は ShortcutHelper を呼ぶ
- 依存: `ShortcutRepository`, `ShortcutHelper`

**ShortcutHelper への追加メソッド:**
- `unpinShortcuts(packageName: String, idsToRemove: Set<String>)`
- `getUninstalledPackages(packageNames: Set<String>): Set<String>`

#### DeleteShortcutUseCase
- 移動元: `HomeViewModel.deleteUnplacedShortcut()`
- LauncherApps 操作（unpin）は `ShortcutHelper` を呼ぶ
- 依存: `ShortcutRepository`, `ShortcutHelper`

---

### Step 4: ShortcutRepository — 移動済み関数を削除

- `applyDefaultLayout()`
- `createShortcutFromDef()`
- `resetToDefault()`
- `getFixedHeightForRow()`
- `addShortcutToFirstEmpty()`
- `clearPageLayout()`

---

### Step 5: HomeViewModel — UseCase 呼び出しに置き換え

コンストラクタに UseCase を追加:

```kotlin
class HomeViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    private val premiumManager: PremiumManager,
    private val applyDefaultLayoutUseCase: ApplyDefaultLayoutUseCase,
    private val addShortcutUseCase: AddShortcutUseCase,      // ShortcutSelectViewModel でも使用
    private val rowUseCase: RowUseCase,
    private val deletePageUseCase: DeletePageUseCase,
    private val cleanupUseCase: CleanupUseCase,
    private val deleteShortcutUseCase: DeleteShortcutUseCase,
    private val billingManager: BillingManager? = null,
    private val adManager: AdManager? = null
)
```

置き換え対象:

| 現在の呼び出し | 置き換え後 |
|---|---|
| `shortcutRepository.resetToDefault()` | `applyDefaultLayoutUseCase.resetToDefault()` |
| `deleteRow(...)` | `rowUseCase.deleteRow(...)` |
| `insertRowAt(...)` | `rowUseCase.insertRowAt(...)` |
| `changeRowColumns(...)` | `rowUseCase.changeRowColumns(...)` |
| `deletePage(...)` | `deletePageUseCase.execute(...)` |
| `cleanupOrphanedPinShortcuts(...)` | `cleanupUseCase.cleanupOrphanedPinShortcuts(...)` |
| `cleanupUninstalledPackages(...)` | `cleanupUseCase.cleanupUninstalledPackages(...)` |
| `onPackageRemoved(...)` | `cleanupUseCase.onPackageRemoved(...)` |
| `deleteUnplacedShortcut(...)` | `deleteShortcutUseCase.execute(...)` |

---

### Step 6: MainActivity — UseCase の生成を追加

`viewModelFactory` 内で UseCase をインスタンス化して HomeViewModel に渡す。

```kotlin
HomeViewModel(
    shortcutRepository = shortcutRepository,
    settingsRepository = settingsRepo,
    calendarRepository = CalendarRepository(context),
    premiumManager = DefaultPremiumManager(context, settingsRepo),
    applyDefaultLayoutUseCase = ApplyDefaultLayoutUseCase(shortcutRepository, context),
    addShortcutUseCase = AddShortcutUseCase(shortcutRepository),
    rowUseCase = RowUseCase(shortcutRepository),
    deletePageUseCase = DeletePageUseCase(shortcutRepository, settingsRepo),
    cleanupUseCase = CleanupUseCase(shortcutRepository, ShortcutHelper(context)),
    deleteShortcutUseCase = DeleteShortcutUseCase(shortcutRepository, ShortcutHelper(context)),
    billingManager = billingManager,
    adManager = adManager
)
```

また `MainActivity.onCreate()` の初回起動処理も置き換え:

```kotlin
// 変更前
if (shortcutRepository.isFirstLaunch()) {
    shortcutRepository.applyDefaultLayout()
}

// 変更後
if (shortcutRepository.isFirstLaunch()) {
    ApplyDefaultLayoutUseCase(shortcutRepository, this).applyDefaultLayout()
}
```

---

### Step 7: DefaultLayoutConfig.kt 削除

`data/DefaultLayoutConfig.kt` を削除。
内容は `usecase/ApplyDefaultLayoutUseCase.kt` に同梱済み。

---

## 完了条件

- ビルドが通ること
- `ShortcutRepository` にビジネスロジックが残っていないこと
- `HomeViewModel` にビジネスロジックが残っていないこと
- `usecase/` パッケージに 6 ファイルが存在すること
