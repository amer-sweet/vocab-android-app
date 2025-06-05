# Android Vocabulary App with Google Drive Sync

This project contains the source code for an Android application designed to help users store vocabulary words with definitions, example sentences, and attached photos. The app also features synchronization with the user's Google Drive using the AppData folder for secure backup.

## Feature

*   Add, edit, and delete vocabulary words.
*   Attach photos to words (from camera or gallery).
*   View words in a searchable list.
*   Review words using Flashcards.
*   Test knowledge with a multiple-choice Quiz.
*   Securely synchronize data (words and photos) with Google Drive (AppData folder).
*   Offline access to locally stored data.

## Project Structures

The code is organized into standard Android project directories:

*   `/`: Contains top-level build files (`build.gradle.kts`, `settings.gradle.kts`), Gradle wrapper scripts (`gradlew`, `gradlew.bat`), properties (`gradle.properties`), and this README.
*   `/app/`: The main application module.
    *   `build.gradle.kts`: App-level build configuration.
    *   `/src/main/kotlin/com/example/vocabapp/`: Contains the Kotlin source code.
        *   `adapter/`: RecyclerView adapters.
        *   `data/`: Room database entities, DAO, and database class.
        *   `repository/`: Data repository.
        *   `sync/`: Google Drive synchronization logic.
        *   `ui/`: Fragments for different screens.
        *   `utils/`: Helper classes (File Storage, Google Sign-In, Drive API).
        *   `viewmodel/`: ViewModels for UI logic.
    *   `/src/main/res/`: Contains resources (layouts, values, etc.).
*   `/gradle/wrapper/`: Contains Gradle wrapper configuration (`gradle-wrapper.properties`). **Note:** The `gradle-wrapper.jar` file is intentionally omitted as it's typically downloaded automatically by the wrapper script or handled by Android Studio upon import.

## Setup Instructions

1.  **Android Studio:** Import the project into Android Studio (latest stable version recommended). Unzip the provided `vocab_app_android_source_v2.zip` file first.
2.  **Gradle Sync:** Android Studio should automatically detect the Gradle files and prompt you to sync the project. This will download the correct Gradle distribution (specified in `gradle/wrapper/gradle-wrapper.properties`) and resolve all dependencies listed in `app/build.gradle.kts`.
3.  **Dependencies:** The necessary dependencies are already listed in `app/build.gradle.kts`. Ensure your Android Studio has internet access to download them during the first sync.
4.  **Google Cloud Console Setup (CRUCIAL for Drive Sync):**
    *   Go to the [Google Cloud Console](https://console.cloud.google.com/).
    *   Create a new project or select an existing one.
    *   Enable the **Google Drive API** for your project (APIs & Services -> Library).
    *   Go to APIs & Services -> Credentials.
    *   Create an **OAuth 2.0 Client ID** for an **Android** application.
    *   You will need to provide the **Package name** (`com.example.vocabapp`) and the **SHA-1 signing-certificate fingerprint** of your debug and release keys.
        *   To get the SHA-1 fingerprint for your debug key, you can use the command: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android` (or use the Gradle signing report task in Android Studio).
    *   Configure the **OAuth consent screen**. Set the user type to **External** (unless you are within a Google Workspace organization). Add the required scopes: `email`, `profile`, and `https://www.googleapis.com/auth/drive.appdata`.
    *   **Important:** Add your Google account email address as a **Test user** during development if your consent screen is in the "Testing" publishing status. Otherwise, Sign-In will fail.
5.  **Build and Run:** Build the project in Android Studio (Build -> Make Project) and run it on an emulator or a physical device (Run -> Run 'app').

## Manual Testing Steps

(Same as previous README version - includes adding words, photos, searching, editing, flashcards, quiz, sign-in, sync, cross-device check, offline mode, error cases)

1.  **Add Words:** Launch the app, tap the '+' FAB, enter word details, attach a photo (test both gallery and camera if implemented), and save.
2.  **View List:** Verify the new word appears in the list. Check if the photo indicator shows.
3.  **Search:** Use the search bar to filter the word list.
4.  **Edit Word:** Tap a word in the list, modify its details (including changing/removing the photo), and save. Verify changes in the list.
5.  **Flashcards:** Navigate to the flashcard section. Test flipping, next/previous navigation. Verify photos appear on the back.
6.  **Quiz:** Navigate to the quiz section. Answer questions, submit, check feedback, and navigate through the quiz. Test the final score and restart functionality.
7.  **Google Sign-In:** Find the Sign-In option (likely in a settings menu or on first launch). Sign in with your test Google account.
8.  **Sync:** Trigger a manual sync (if a button exists) or observe automatic sync. Check for success/error messages.
9.  **Cross-Device/Data Check (Advanced):**
    *   Install the app on a second device (or clear app data on the first device).
    *   Sign in with the same Google account.
    *   Trigger sync. Verify that words and photos added on the first device appear.
    *   Add/edit/delete a word on the second device, sync, then sync on the first device and verify changes are reflected.
    *   (Optional) Use tools like `rclone` configured for `drive.appdata` scope to inspect the `vocabulary_data.json` and image files in the hidden AppData folder on Google Drive.
10. **Offline Mode:** Disable network connectivity. Add/edit/delete words. Verify the app functions correctly. Re-enable network and trigger sync. Verify offline changes are uploaded.
11. **Error Cases:** Test invalid input, network errors during sync, sign-in cancellation, insufficient Drive permissions (if possible).

## Notes

*   This code provides the core structure and logic. You will need to integrate navigation using Jetpack Navigation Component (add the dependency and create a navigation graph XML in `res/navigation/`).
*   Error handling and UI refinement (e.g., loading indicators, detailed error messages) can be further improved.
*   The `WordDetailFragment` was not explicitly created in this sequence but would be needed to display full word details.
*   Background synchronization (e.g., using WorkManager) is marked as optional and not implemented here.
*   Remember to handle runtime permissions for Camera and Storage if targeting newer Android versions.
