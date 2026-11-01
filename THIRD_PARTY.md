# Third-party components

The root MIT license does not replace upstream component licenses.

| Component | Source | License |
| --- | --- | --- |
| Outfit | launcher/FONT-LICENSE-OFL.txt | SIL OFL 1.1 |
| Vosk Android 0.3.75 | https://github.com/alphacep/vosk-api | Apache-2.0, licenses/VOSK-COPYING |
| vosk-model-small-ru-0.22 | https://alphacephei.com/vosk/models | Apache-2.0 as listed in upstream catalog |
| JNA Android 5.18.1 | https://github.com/java-native-access/jna | Dual LGPL-2.1-or-later / Apache-2.0; Apache-2.0 chosen for this distribution. See licenses/JNA-LICENSE and licenses/JNA-APACHE-2.0 |
| AndroidX RecyclerView 1.3.2 / DynamicAnimation 1.0.0 | AndroidX | Apache-2.0, resolved by Gradle |
| JUnit 4.13.2 | Test-only | EPL-1.0 |

Vosk and JNA AARs are pinned local dependencies downloaded from release assets by launcher/download-dependencies.ps1. The model is also a separate source dependency download and is bundled inside the ready APK. Checksums are in DEPENDENCIES.sha256. Upstream notices embedded in artifacts are preserved. The speech model archive is unmodified. Server profiles and signing keys are not bundled.
