# KotMiksMenus

Модульный плагин GUI-меню для **Paper / Purpur 1.21+** (Java 21, Gradle Kotlin DSL).
Без NMS и ProtocolLib. Сайт сервера: https://donat.kotmir.tech

## Сборка (Windows)

```powershell
.\gradlew.bat build
```

Готовый файл: `build\libs\KotMiksMenus-1.0.0.jar` → в папку `plugins`.
Нужен установленный JDK 21. Gradle Wrapper (8.14.3) включён в проект.

## Зависимости (softdepend)

| Плагин | Зачем |
|---|---|
| Vault (+ любая экономика) | монеты `◉` |
| PlaceholderAPI | `%kotmiksmenus_rubies%`, `%kotmiksmenus_title%`, `%kotmiksmenus_bp_coins%`, `%kotmiksmenus_bp_level%` |
| PlayerPoints | необязательно: `rubies.provider: playerpoints` в `config.yml` |

## Команды

| Команда | Что делает | Право |
|---|---|---|
| `/shop` | магазин (6 вкладок) | `kotmiksmenus.shop` |
| `/social` | привязка Discord / Telegram | `kotmiksmenus.social` |
| `/daily` | ежедневная награда | `kotmiksmenus.daily` |
| `/kills` (`/killrewards`) | награды за убийства | `kotmiksmenus.kills` |
| `/donate` | донат-меню | `kotmiksmenus.donate` |
| `/titles` (`/title`) | титулы | `kotmiksmenus.titles` |
| `/base` (`/bases`) | базы; `home`, `friend add/remove`, `accept/deny`, `leave`, `visit` | `kotmiksmenus.base` |
| `/killeffects` | эффекты убийства | `kotmiksmenus.killeffects` |
| `/bp` (`/battlepass`) | батл-пасс | `kotmiksmenus.bp` |
| `/quests` (`/quest`) | начальные квесты | `kotmiksmenus.quests` |
| `/kmenus reload` | перезагрузка всех конфигов | `kotmiksmenus.admin` |
| `/kmenus givecoin <игрок> <кол-во>` | выдать соц. коины | `kotmiksmenus.admin` |
| `/rubies <give\|take\|set\|balance> <игрок> [кол-во]` | рубины | `kotmiksmenus.admin` |
| `/titles give <игрок> <id>` | выдать титул | `kotmiksmenus.admin` |
| `/bp coins <give\|take\|set\|balance> <игрок> [кол-во]` | батл-коины | `kotmiksmenus.admin` |

Дополнительно: `kotmiksmenus.bases.bypass` (op) — обход защиты баз.

## Конфиги

`config.yml` (сообщения, соцсети, `/daily`, `/kills`, кастомные головы) · `shop.yml` · `donate.yml` · `titles.yml` ·
`bases.yml` · `killeffects.yml` · `battlepass.yml` · `quests.yml`. Данные игроков — `data.yml`.
Все тексты — MiniMessage. После правки — `/kmenus reload`.

### Что нужно заполнить вручную

1. **Ссылки соцсетей** — `config.yml` → `social.*.steps.*.value` (сейчас пустые, в чате появится «ссылка не настроена»).
   В командах привязки по умолчанию стоит `/link {player}` — замените на свою.
2. **Кастомные головы** — `config.yml` → `heads`: вставьте Base64 (поле *Value* на minecraft-heads.com).
   Пока значения пустые, везде показывается обычная голова.
3. **Команды кейсов/китов** — по умолчанию `crates give <ник> common 1`, `crates givekey <ник> coin 1`, `kit give <ник> nether` — подставьте синтаксис вашего плагина кейсов/китов.
4. **Создание базы** — `bases.yml` → `create-commands` (например, команда WorldGuard). Встроенная защита территории
   включена по умолчанию (`protection.enabled`), её можно выключить.

## Важно знать

- Предметы PvP-вкладки (явная пыль, трапки, стан, магическая палочка, лив с паутины) выдаются с меткой PDC `special`
  (`reveal_dust`, `small_trap`, `big_trap`, `ender_trap`, `stun`, `magic_wand`, `web_escape`).
  **Сама механика этих предметов в плагин не входит** — меню только продаёт их.
- Удочка из магазина с тегом `keep_on_death` не выпадает при смерти и возвращается после респавна (это реализовано).
- Квесты и задания батл-пасса засчитывают только действия в выживании (в креативе прогресс не идёт).
- Квесты выполняются автоматически: награда выдаётся сразу при завершении всех задач активного квеста.
- Уровни батл-пасса забираются по порядку, стоимость в батл-коинах списывается.
