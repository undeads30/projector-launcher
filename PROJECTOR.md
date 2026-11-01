# Сведения о проекторе

Этот файл — справка для следующих обращений. Сначала читайте его и README.md;
не запрашивайте у владельца повторно уже подтверждённые сведения.

## Источник и дата проверки

**27 октября 2026 года, часовой пояс Asia/Shanghai.** Данные ниже считаны
непосредственно с проектора через ADB с локального компьютера Windows.
Использовались только команды чтения: настройки и приложения устройства
в ходе сбора не изменялись.

Предыдущая запись о недоступности устройства относилась к облачной среде.
При этой проверке подключение по локальной сети успешно установлено.
ADB доступен по TCP, порт `5555`. Адрес может меняться; в командах используйте
актуальный `PROJECTOR_IP`. IP, серийные номера, MAC-адреса, пароли,
ключи ADB и содержимое VPN-профилей в этот файл не включены.

## Последующее обновление — 30 октября 2026

На исходном устройстве выполнен переход 907 → 908. Подтверждены Android 9, сборка 1.8.150, userdebug/dev-keys и root через ADB; SELinux Enforcing. Magisk не устанавливался. Таблица ниже сохранена как исторические сведения от 27 октября. Актуальная инструкция по прошивке и бэкапу: https://github.com/undeads30/cronus-908-root-guide.

## Идентификация и прошивка

| Параметр | Значение с устройства |
| --- | --- |
| Модель, `ro.product.model` | `Cronus` |
| Производитель в прошивке, `ro.product.manufacturer` | `asu` |
| Бренд в прошивке, `ro.product.brand` | `Xiaomi` |
| Product name / device | `t982_ar301` / `t982_ar301` |
| Board / platform | `t3` / `t3` |
| Hardware | `amlogic`; в CPU info — `Amlogic` |
| Android | **9**, API **28** |
| Версия сборки, `ro.build.display.id` | `1.8.150` |
| Дата сборки, `ro.build.date` | `Tue May 24 11:36:12 CST 2022` |
| Заявленный патч безопасности | `2018-08-05` |
| Тип / подпись сборки | `user` / `dev-keys` |
| Ядро Linux | `5.4.125-ab907` |

Build fingerprint:

```text
Xiaomi/t982_ar301/t982_ar301:9/PPR1.180610.011/907:user/dev-keys
```

Значения производителя и бренда — строки прошивки. Они сами по себе
не подтверждают торговую марку или изготовителя корпуса. Точная торговая
модель по маркировке корпуса пока не проверена. Имя `t982_ar301` сохранено
как идентификатор платформы; точная модель SoC отдельно не подтверждена.

## Процессор, графика и совместимость APK

| Параметр | Подтверждённое значение |
| --- | --- |
| Ядра CPU | 4, индексы `0–3` |
| Строка CPU | `ARMv8 Processor rev 0 (v8l)` |
| CPU implementer / part | `0x41` / `0xd05` |
| Максимальная частота по cpufreq | `1920000 kHz` = 1,92 ГГц |
| ABI приложений | `armeabi-v7a,armeabi` |
| 64-битный ABI | `ro.product.cpu.abilist64` пуст |
| GPU | `ARM Mali-G52` |
| OpenGL ES | `3.2`, по SurfaceFlinger и списку features |

**Для установки выбирайте APK для ARMv7 / armeabi-v7a либо universal,
содержащий эту архитектуру.** APK только для `arm64-v8a` не подходит:
процессор сообщает ARMv8, но прошивка объявляет только 32-битные ABI приложений.
Приложение должно поддерживать Android 9 / API 28; одного подходящего ABI
недостаточно для гарантии совместимости.

## Память и экран

| Параметр | Значение |
| --- | --- |
| ОЗУ, доступная ядру (`MemTotal`) | `2031160 kB`, около 1,94 GiB |
| Swap (`SwapTotal`) | `262140 kB`, около 256 MiB |
| Раздел пользовательских данных `/data` | По `df -h`: 11G всего, 991M занято, 10G доступно |
| Общая внутренняя память `/storage/emulated/0` | Использует тот же раздел: 11G всего |
| Размер дисплея по `wm size` | `Physical size: 1920x1080` |
| Плотность по `wm density` | `Physical density: 320` |
| Частота по SurfaceFlinger | Около 60 Гц |

Свободная память и место на диске относятся к моменту проверки.
Общий физический объём накопителя не установлен: чтение
`/sys/class/block/mmcblk0/size` вернуло `Permission denied`.
Размер `/data` не равен полной ёмкости накопителя.
Разрешение Android-дисплея не подтверждает нативное разрешение матрицы
или оптические характеристики проектора.

## Возможности, объявленные Android

Команда `pm list features` сообщает:

- TV-интерфейс `android.software.leanback` и `android.software.live_tv`.
- Wi-Fi и Wi-Fi Direct.
- Bluetooth и Bluetooth LE.
- Ethernet.
- USB host и USB accessory.
- HDMI CEC.
- Аудиовыход, WebView, поддержку внешней камеры.

Это заявленные возможности прошивки, а не результаты проверки каждого
физического интерфейса. Версии Wi-Fi/Bluetooth, наличие конкретных разъёмов
и работа внешней камеры отдельно не проверялись.

В `dumpsys tv_input` зарегистрированы HDMI1, HDMI2, HDMI3 и AV1
из пакета `com.droidlogic.tvinput`. Наличие записей не доказывает
количество разъёмов на корпусе.

## Установленные приложения проекта

| Приложение | Пакет | Версия | versionCode | minSdk / targetSdk |
| --- | --- | --- | --- | --- |
| Лаунчер | `com.gathin.tvhome` | `2.0` | `2` | `26 / 34` |
| Закрытие уведомления HDMI | `ru.projector.hdmiautoclose` | `1.0` | `1` | `26 / 34` |
| v2rayNG | `com.v2ray.ang` | `1.8.39` | `4000585` | `21 / 34` |

Обработчик HOME: `com.gathin.tvhome/.MainActivity`.

В настройках специальных возможностей включены:

- `com.gathin.tvhome/.FilmVpnService`
- `ru.projector.hdmiautoclose/.HdmiPromptService`

Доступ к уведомлениям выдан
`com.gathin.tvhome/com.gathin.tvhome.VpnStatusService`.
Это состояние разрешений; непрерывная работа служб в ходе сбора не проверялась.

По README проекта лаунчер использует профили VPN HK / RU.
Содержимое профилей и параметры серверов не считывались.
Резервные APK: `dist/LauncherTV.apk` и `dist/HDMI-Autoclose.apk`.
Они предназначены для обновления с сохранением данных при совпадающей подписи;
это резервные копии приложений, а не прошивка устройства.

## Файловый менеджер

На устройстве найден пакет штатного файлового менеджера:
`com.asu.filemanager`.

Ранее предложенный вариант — **X-plore File Manager**, с управлением пультом.
В рамках этой проверки сторонний файловый менеджер не устанавливался,
его удобство и работа на этом экземпляре проектора не проверялись.
При подборе версии учитывайте Android 9 и ABI `armeabi-v7a`.

## Как повторить проверку

С компьютера в локальной сети проектора:

```sh
adb connect PROJECTOR_IP:5555
adb -s PROJECTOR_IP:5555 shell getprop ro.product.model
adb -s PROJECTOR_IP:5555 shell getprop ro.product.manufacturer
adb -s PROJECTOR_IP:5555 shell getprop ro.product.brand
adb -s PROJECTOR_IP:5555 shell getprop ro.product.device
adb -s PROJECTOR_IP:5555 shell getprop ro.board.platform
adb -s PROJECTOR_IP:5555 shell getprop ro.build.version.release
adb -s PROJECTOR_IP:5555 shell getprop ro.build.version.sdk
adb -s PROJECTOR_IP:5555 shell getprop ro.product.cpu.abilist
adb -s PROJECTOR_IP:5555 shell getprop ro.build.display.id
adb -s PROJECTOR_IP:5555 shell cat /proc/meminfo
adb -s PROJECTOR_IP:5555 shell df -h /data /storage/emulated/0
adb -s PROJECTOR_IP:5555 shell wm size
adb -s PROJECTOR_IP:5555 shell wm density
adb -s PROJECTOR_IP:5555 shell uname -r
adb -s PROJECTOR_IP:5555 shell pm list features
```

Дополнительные источники при сборе: `/proc/cpuinfo` (без публикации Serial),
`/sys/devices/system/cpu/present`,
`/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq`,
`dumpsys SurfaceFlinger`, `dumpsys package`, `dumpsys tv_input`,
выборочное чтение настроек включённых служб.

Неизвестными остаются точная торговая модель по шильдику, точный SoC,
полная ёмкость накопителя, нативная матрица, яркость, оптика и версии
радиоинтерфейсов. Эти характеристики нельзя достоверно вывести из
считанных строк прошивки.
