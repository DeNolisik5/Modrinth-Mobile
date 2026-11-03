# Modrinth Client

Нативный Android-клиент для [Modrinth](https://modrinth.com) в стиле **Material 3 Expressive**.

## Что умеет

- 🔍 Поиск модов через официальный API (`api.modrinth.com/v2`)
- 🖼️ Показывает иконку, название, описание, автора и количество скачиваний
- 🎯 Фильтр по версии Minecraft (1.16.5 … 1.21.8)
- 📦 Список версий мода: номер, загрузчики, размер файла
- ⬇️ Скачивание `.jar` прямо в `Downloads/ModrinthClient` на телефоне
- 📊 Прогресс загрузки в реальном времени

## Дизайн

- `MaterialExpressiveTheme` из `androidx.compose.material3:material3:1.4.0`
- Динамические цвета Material You (Android 12+) с запасной зелёной палитрой
- Крупные скругления (28–36 dp), плакатная типографика, пружинные анимации

## Сборка

Проект собирается на GitHub Actions — локальный Android SDK не нужен.

```
git push origin main      # соберёт APK и приложит артефакт
git push origin v1.0.0    # дополнительно создаст GitHub Release
```

## Установка

Скачай APK из раздела **Releases** и открой на телефоне (нужно разрешить установку
из неизвестных источников). APK подписан debug-ключом — этого достаточно для
сайдлоада.

## Разрешения

- `INTERNET` — запросы к API и загрузка файлов
- `WRITE_EXTERNAL_STORAGE` — только для Android 8–9; на Android 10+ файлы
  пишутся через `MediaStore` вообще без разрешений

## Стек

Kotlin · Jetpack Compose · Material 3 Expressive · OkHttp · Coil · org.json
