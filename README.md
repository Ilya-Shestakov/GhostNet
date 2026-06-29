GhostNet

    Autonomous Secure Ecosystem. Мобильная платформа, включающая в себя функции защищенного мессенджера с архитектурой E2EE и Offline-first синхронизацией.

![alt text](https://img.shields.io/badge/Stack-Java%20%7C%20MVVM%20%7C%20Room%20%7C%20Firebase-blue?style=for-the-badge)


![alt text](https://img.shields.io/badge/Security-AES--GCM%20%7C%20Android%20Keystore-red?style=for-the-badge)


![alt text](https://img.shields.io/badge/Logic-Offline--First-green?style=for-the-badge)


Техническая архитектура (Core Model)
1. Offline-First & Sync Engine

Сердце системы — паттерн Repository, который выступает арбитром между локальной БД и облаком:

    Локальный кэш (Room): Хранит не только текст, но и метаданные медиаконтента.

    Система синхронизации: При восстановлении сети GhostNet использует фоновые сервисы для «разгрузки» очереди локальных изменений в Firebase.

    Оптимизация трафика: Используется Glide с кастомными моделями кэширования и Cloudinary API для работы с Raw-медиа, что позволяет экономить до 40% трафика при повторных запросах.

2. Криптографическая модель (Military-Grade)

В GhostNet реализована защита на трех уровнях JCA (Java Cryptography Architecture):

    E2EE (End-to-End): Сообщения и посты шифруются ключами, которые никогда не покидают устройство.

    AES-GCM (Authenticated Encryption): Использование 128-битного проверочного тега исключает атаки на целостность данных в транзите.

    Hardware-Backed Protection: Ключи генерируются внутри Android Keystore System (аппаратный уровень защиты). Даже при наличии Root-прав на устройстве, извлечение приватного ключа невозможно.

3. Media & Realtime Infrastructure

    Firebase Realtime DB: Служит только для передачи легковесных сигнальных данных (статусы, события печати).

    Cloudinary: Используется как CDN для распределенной доставки тяжелого контента.

    MVVM + Clean Architecture: Логика шифрования и работы с сетью полностью отделена от UI, что позволяет легко масштабировать проект (например, заменить Firebase на кастомный бэкенд на Yandex Cloud).

Технологический стек (Deep Dive)

    UI/UX: Material Design 3, адаптивные Layouts для работы с медиа-сетками.

    Database: Room (SQLite) с поддержкой сложных миграций и реляционных связей.

    Network: Retrofit/Firebase SDK, Real-time listeners.

    Security: JCA, SecretKeySpec, GCMParameterSpec, Android KeyStore.

Roadmap & Scalability

    GhostAI Node: Планируемая интеграция распределенных вычислений на устройствах.

    Advanced Geo-Social: Интеграция MapView для создания защищенных локальных сообществ.

    Backend Migration: Перенос бизнес-логики в Yandex Cloud (Managed PostgreSQL + Object Storage).


Проект GhostNet является индивидуальной разработкой автора Coffein (Шестаков И.Д.).
