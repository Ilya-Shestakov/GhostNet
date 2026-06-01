# GhostNet 🛡️
> Защищенная мобильная экосистема для обмена медиаконтентом с поддержкой автономного режима.

[![Java](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow.svg)](https://firebase.google.com/)
[![Security](https://img.shields.io/badge/Security-AES--GCM-blue.svg)](#)

---

## Демонстрация
<p align="center">
  <img src="app/media_git/video_2026-06-01_13-51-26.mp4" width="300" title="Demo GhostNet">
</p>

---

## О проекте
**GhostNet** — это результат 8-месячной разработки, направленной на создание безопасной среды для общения. Проект объединяет функционал социальной сети и мессенджера, работая по принципу **Offline-first**: все данные доступны пользователю даже при отсутствии сети благодаря сложной системе синхронизации.

**Основная цель:** обеспечить приватность данных и безопастное общение.

---

№# Технологический стек
- **Frontend:** Java (Android SDK), MVVM Architecture.
- **Local Database:** Room (SQLite) — реализация офлайн-очереди и кэширования.
- **Backend:** Firebase (Authentication, Firestore, Realtime Database, Storage).
- **Image/Media:** Glide (оптимизированная подгрузка), MapView (геоинтеграция), Cloudinary (хранилище тяжелых данных).
- **Security:** Java Cryptography Architecture (JCA).

---

## Информационная безопасность (Deep Dive)
Как призер ВСОШ по ИБ и участник Bug Bounty программ, я уделил особое внимание защите данных:
- **End-to-End Encryption (E2EE):** Сообщения шифруются на устройстве перед отправкой.
- **Алгоритм:** Используется **AES-GCM (128-bit tag)**. Выбор пал на GCM, так как он обеспечивает не только конфиденциальность, но и проверку целостности данных (Authenticated Encryption).
- **Защита от перехвата:** Архитектура спроектирована с учетом минимизации рисков манипуляции состояниями (State Manipulation) и перехвата данных в транзите.

---

## Архитектурные решения
- **Hybrid Data System:** Реализован механизм прозрачного переключения между локальным кэшем (Room) и облаком (Firebase Persistence).
- **Real-time Engine:** Использование динамических слушателей Firebase для реализации статусов «печатает...» и «online» с минимальной задержкой.
- **Media Handling:** Система обработки «тяжелых» файлов с индикацией прогресса загрузки и автоматической очисткой кэша.

---

## Планы по развитию (Roadmap)
- [ ] Миграция серверной части на инфраструктуру **Yandex Cloud**.
- [ ] Внедрение биометрической аутентификации для доступа к приложению.
- [ ] Реализация групповых зашифрованных чатов.

---

## Автор
**Шестаков Илья** 
- **Bug Bounty:** [Профиль на Standoff 365](https://standoff365.com/profile/shvi/)
- **VK:** [@ilya_5340](https://vk.com/ilya_5340)
- **Email:** ilya.shestakov08@gmail.com

---

*Проект разработан специально для конкурса ITMO.STARS*

---
