# Android String File Localization 🌍

**Android String File Localization** is a productivity-focused IntelliJ/Android Studio plugin designed to streamline the localization process. It automates the translation of `strings.xml` files while ensuring the output is sanitized and 100% compliant with Android build requirements.

## What It Is

<!-- Plugin description -->

A lightweight but powerful IntelliJ IDEA/Android Studio plugin that identifies missing translations in your Android project and fills them using automated translation services. It handles the "heavy lifting" of creating directories, managing XML tags, and sanitizing text for Android compatibility.
This specific section is a source for the [plugin.xml](/src/main/resources/META-INF/plugin.xml) file which will be extracted by the [Gradle](/build.gradle.kts) during the build process.

<!-- Plugin description end -->


## ✨ Why use this?

Manual translation is slow and error-prone. This plugin solves the most common headaches:

* **No more "Apostrophe" crashes:** Automatically escapes `'` to `\'` so your app actually builds.
* **Invisible Character Removal:** Cleans out `NBSP` (Non-Breaking Spaces) that often hide in web-copied text and break UI layouts.
* **Zero-Loss Merging:** It doesn't overwrite your work; it merges new translations into existing localized files.

---

## 🛠 Features

* **Smart Search:** Quickly find specific keys or English phrases in your resource files.
* **Contextual Previews:** Displays original English text alongside keys for better translation context.
* **Bulk Selection Logic:** * **Smart Select:** Picks only missing translations.
* **Force Update:** Re-translates everything to refresh your language files.


* **Translatable Guard:** Automatically respects and protects strings marked `translatable="false"`.
* **Tools Menu Integration:** Accessible via `Tools > Translate Strings...` or via right-click context menus.

---

## 📖 How to Use

### 1. Launching the Plugin

You can trigger the tool from three locations:

* **The Tools Menu:** `Tools` -> `Translate Strings...`
* **The Project Tree:** Right-click on any `strings.xml`.
* **The Editor:** Right-click inside your code and select the translation action.

### 2. Selection Strategy

1. **Enter Language Code:** Type the ISO code (e.g., `hi` for Hindi, `es` for Spanish).
2. **Filter & Select:** Use the search bar to find keys. Use **Select All** to grab missing strings.
3. **Execute:** Click **OK**. Watch the progress in the bottom-right background task bar.
4. **Completion:** A success balloon notification will confirm how many strings were updated.

---

## 🏗 Installation

### Manual Install

1. Download the latest `plugin.zip` from the [Releases](https://www.google.com/search?q=https://github.com/sundramsinghdev007/Android-String-File-Localization/releases) section.
2. Open Android Studio -> `Settings` -> `Plugins`.
3. Click the ⚙️ (Gear Icon) -> **Install Plugin from Disk...**.
4. Select the `.zip` file and restart.

---

## 💻 Tech Stack

* **Language:** Kotlin
* **Framework:** IntelliJ Platform SDK
* **Target:** Android Studio (Ladybug 2024.2.1+)
* **Architecture:** MVVM-inspired UI with Background Thread (BGT) action updates.

---

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](/LICENSE.txt) file for more details.

---

### Thanks

Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

---