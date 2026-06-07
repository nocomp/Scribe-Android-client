<p align="center">
  <img src="docs/scribe_multilingue.png" width="640" alt="SCRIBE Mobile — multilingue">
</p>

<h1 align="center">SCRIBE Mobile</h1>

<p align="center">
  <img src="https://img.shields.io/badge/license-AGPL--3.0-blue.svg" alt="License: AGPL-3.0">
  <img src="https://img.shields.io/badge/platform-Android%2010%2B-3DDC84.svg" alt="Android 10+">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF.svg" alt="Kotlin / Compose">
  <img src="https://img.shields.io/badge/i18n-24%20langues-e1000f.svg" alt="24 langues">
</p>

<p align="center">
  🇫🇷 <a href="#-français">Français</a> · 🇬🇧 <a href="#-english">English</a>
</p>

---

## 🇫🇷 Français

**SCRIBE Mobile** est le client Android natif de **SCRIBE**, la plateforme open‑source de
gestion de crise hospitalière. Il permet aux directeurs de crise et aux personnels soignants
de suivre et de piloter une crise **depuis leur mobile**, en mobilité, en astreinte ou sur le terrain.

Le serveur SCRIBE (backend + interface web) : **https://github.com/nocomp/scribe**

### Fonctionnalités
- **Tableau de bord** : indicateurs en temps réel (incidents ouverts, critiques, tâches, transferts, messages non lus, secteurs de soins impactés), tuiles cliquables.
- **Incidents** : déclaration, consultation du détail, validation des **jalons**, changement de **statut** (jusqu'à Résolu).
- **Communiqués** : niveau global, message public, systèmes d'information, prise en charge, FAQ, chronologie.
- **Soins / Capacité** : synthèse de situation (triée par criticité) et **déclaration de capacité** en lits.
- **Transferts** : suivi des transferts en cours (vue non‑nominative) + changement de statut.
- **Brancardage** : missions, prise en charge et **confirmation d'arrivée** côté mobile.
- **Tâches (Kanban)**, **Cellule de crise** (décisions).
- **Messagerie** (rédaction, réponse, transfert, carnet d'adresses) et **Chat** (salons, présence, salons privés).
- **Droits par rôle** : `cellule_crise` / `soignant` / `admin` — chacun ne voit que ses rubriques.
- **Multilingue** : sélection de la langue au login, **24 langues** réutilisant l'i18n du serveur SCRIBE.

### Prérequis
- **Android 10+** (API 29).
- Une **instance SCRIBE** accessible (le client n'embarque aucune URL : elle se saisit à la connexion).

### Compilation
1. Cloner le dépôt et ouvrir le dossier dans **Android Studio** (Ladybug ou plus récent).
2. **Gradle JDK = 17** (`Settings ▸ Build Tools ▸ Gradle ▸ Gradle JDK`).
3. *Sync Project with Gradle Files*, puis **Run** sur un appareil/émulateur Android 10+.
4. APK prêt à installer : voir l'onglet **Releases**.

### Connexion
Au lancement : **adresse de l'instance** (sans `http`, ex. `scribe.mon-hopital.fr` ou `203.0.113.10:8000`),
puis **identifiant** / **mot de passe** (double authentification gérée si activée), et le **choix de la langue**.

### Sécurité (HDS / RGPD)
- JWT chiffré **AES‑256‑GCM** via l'**Android Keystore**.
- `FLAG_SECURE` (pas de capture d'écran, pas d'aperçu dans le sélecteur d'apps).
- Aucun cache disque non chiffré ; logs réseau sans corps de requête/réponse.

### Stack
Kotlin · Jetpack Compose (Material 3, charte **DSFR**) · Hilt · Retrofit/OkHttp/Moshi · DataStore · Android Keystore.

---

## 🇬🇧 English

**SCRIBE Mobile** is the native Android client for **SCRIBE**, the open‑source hospital
crisis‑management platform. It lets crisis directors and care staff monitor and drive a crisis
**from their phone** — on the move, on call, or in the field.

SCRIBE server (backend + web UI): **https://github.com/nocomp/scribe**

### Features
- **Dashboard**: real‑time indicators (open incidents, critical, tasks, transfers, unread messages, impacted care sectors), tappable tiles.
- **Incidents**: declare, view details, validate **milestones**, change **status** (up to Resolved).
- **Bulletins**: global level, public message, IT systems, patient care, FAQ, timeline.
- **Care / Capacity**: situation overview (sorted by criticality) and **bed‑capacity declaration**.
- **Transfers**: ongoing transfers (non‑nominative view) + status change.
- **Porters (brancardage)**: missions, pickup and **arrival confirmation** from mobile.
- **Tasks (Kanban)**, **Crisis cell** (decisions).
- **Messaging** (compose, reply, forward, address book) and **Chat** (rooms, presence, private rooms).
- **Role‑based access**: `cellule_crise` / `soignant` / `admin` — each role only sees its sections.
- **Multilingual**: language selection at login, **24 languages** reusing the SCRIBE server i18n.

### Requirements
- **Android 10+** (API 29).
- A reachable **SCRIBE instance** (no URL is bundled; it is entered at login).

### Build
1. Clone the repo and open the folder in **Android Studio** (Ladybug or newer).
2. **Gradle JDK = 17** (`Settings ▸ Build Tools ▸ Gradle ▸ Gradle JDK`).
3. *Sync Project with Gradle Files*, then **Run** on an Android 10+ device/emulator.
4. Ready‑to‑install APK: see the **Releases** tab.

### Login
At startup: **instance address** (without `http`, e.g. `scribe.my-hospital.org` or `203.0.113.10:8000`),
then **username** / **password** (MFA supported), and the **language** of your choice.

### Security (GDPR / health‑data)
- JWT encrypted with **AES‑256‑GCM** in the **Android Keystore**.
- `FLAG_SECURE` (no screenshots, no preview in the app switcher).
- No unencrypted disk cache; network logs without request/response bodies.

### Stack
Kotlin · Jetpack Compose (Material 3, French State **DSFR** design) · Hilt · Retrofit/OkHttp/Moshi · DataStore · Android Keystore.

---

## Licence / License

**AGPL‑3.0** — voir [`LICENSE`](LICENSE). Comme le serveur SCRIBE.
© Hervé PELLARIN.

> Conçu et développé par **Hervé PELLARIN** — *designed & built by Hervé PELLARIN*.
