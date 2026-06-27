GhostNet 🛡️

    GhostNet — это высокопроизводительная мобильная экосистема для обмена данными, спроектированная с упором на криптографическую защиту и концепцию Offline-first.

![alt text](https://img.shields.io/badge/Language-Java-orange.svg?style=flat-square&logo=java)


![alt text](https://img.shields.io/badge/Platform-Android-green.svg?style=flat-square&logo=android)


![alt text](https://img.shields.io/badge/Backend-Firebase-yellow.svg?style=flat-square&logo=firebase)


![alt text](https://img.shields.io/badge/Security-AES--GCM-blue.svg?style=flat-square)


![alt text](https://img.shields.io/badge/License-MIT-lightgrey.svg?style=flat-square)

📱 Демонстрация
<p align="center">
<img src="app/media_git/demo_preview.gif" width="300" title="GhostNet Interface">
<br>
<i>(Если гифка тяжелая, можно оставить ссылку на видео ниже)</i>
<br>
<a href="app/media_git/video_2026-06-01_13-51-26.mp4">🎬 Смотреть полное видео демонстрации</a>
</p>
🚀 О проекте

Проект разрабатывался в течение 8 месяцев как ответ на современные вызовы приватности. В отличие от стандартных мессенджеров, GhostNet объединяет функционал социальной сети и инструмента для безопасной передачи чувствительной информации.
Ключевые особенности:

    Offline-first Architecture: Полная работоспособность при отсутствии сети. Интеллектуальная синхронизация данных при восстановлении соединения.

    Hybrid Storage: Оптимизированное сочетание локальной БД (Room) и облачной инфраструктуры.

    Media Optimization: Ленивая загрузка контента и кэширование, минимизирующее потребление трафика.

🛠 Технологический стек
Слой	Технологии
Mobile Core	Java (Android SDK), MVVM Architecture
Local Data	Room Persistence Library (SQLite), SharedPreferences
Backend / Cloud	Firebase (Auth, Firestore, RTDB, Storage), Cloudinary API
Media & UI	Glide, MapView SDK, Custom UI Components
Security	Java Cryptography Architecture (JCA), Android Keystore
🔐 Информационная безопасность (Deep Dive)

Как исследователь в области ИБ (Bug Bounty hunter и призер ВСОШ), я интегрировал в проект механизмы защиты, соответствующие современным стандартам:

    End-to-End Encryption (E2EE): Весь контент шифруется непосредственно на клиенте.

    Режим шифрования AES-GCM (128-bit tag):

        Выбран режим AEAD (Authenticated Encryption with Associated Data).

        Гарантирует не только конфиденциальность, но и целостность сообщений (защита от атак типа Bit-flipping и Padding Oracle).

    Hardware-backed Security: Приватные ключи генерируются и хранятся внутри Android Keystore, что исключает их извлечение даже при компрометации ОС.

    Zero-Knowledge Approach: Серверная часть выступает лишь ретранслятором, не имея доступа к ключам дешифровки.

🏗 Архитектура системы

    Data Layer: Реализован паттерн Repository для управления потоками данных между Firebase и Room.

    Real-time Engine: Использование Event-listeners для мгновенного обновления статусов («печатает...», «в сети») с оптимизацией количества запросов.

    Security Layer: Выделенный модуль для криптографических операций, изолированный от UI-логики.

🗺 Дорожная карта (Roadmap)

    Cloud Migration: Переход на Yandex Cloud (Object Storage & Managed PostgreSQL) для повышения масштабируемости.

    Biometrics: Интеграция BiometricPrompt для дополнительной защиты входа.

    Group Privacy: Реализация протокола группового обмена ключами (на базе Double Ratchet или аналогичных).

👨‍💻 Автор

Шестаков Илья — Android Developer & Security Researcher

    Bug Bounty: Profile on Standoff 365

    VK: @ilya_5340

    LinkedIn: [Ваша ссылка, если есть]

<p align="center">
<i>Специально для конкурса <b>ITMO.STARS</b> ⭐️</i>
</p>
