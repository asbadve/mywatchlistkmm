# Compose Multiplatform Application

## Before running!
- check your system with [KDoctor](https://github.com/Kotlin/kdoctor)
- install JDK 17 on your machine
- add `local.properties` file to the project root and set a path to Android SDK there
- **Set up the TMDB API Key**: The application uses The Movie Database (TMDB) API. You need to obtain an API key and make it available to the build system:
  1. Register and request an API key at [The Movie Database (TMDB)](https://www.themoviedb.org/).
  2. Add the key to your global Gradle properties file (usually located at `~/.gradle/gradle.properties` on macOS/Linux or `%USERPROFILE%\.gradle\gradle.properties` on Windows) to prevent accidentally committing it to Git:
     ```properties
     MY_WATCH_LIST_TMDB_API_KEY=your_api_key_here
     ```
     *(Alternatively, you can temporarily add it to the project's root `gradle.properties` file, but be careful not to commit it.)*

### Android
To run the application on android device/emulator:
- open project in Android Studio and run imported android run configuration

To build the application bundle:
- run `./gradlew :composeApp:assembleDebug`
- find `.apk` file in `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

### Desktop
Run the desktop application: `./gradlew :composeApp:run`

### iOS
To run the application on iPhone device/simulator:
- Open `iosApp/iosApp.xcproject` in Xcode and run standard configuration
- Or use [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio

### Browser
Run the browser application: `./gradlew :composeApp:jsBrowserDevelopmentRun`

demo
[![Watch the video](https://i.stack.imgur.com/Vp2cE.png)](https://youtu.be/mwLZfRtcDw8)

## Screenshots & Layouts

Here are the side-by-side Light Mode and Dark Mode captures for each build variant.

### 1. Desktop Target

#### Desktop (Compact Size)
| Light Mode | Dark Mode |
| :---: | :---: |
| ![Compact Light](./screenshots/desktop_compact_light.png)<br>[View Image](./screenshots/desktop_compact_light.png) | ![Compact Dark](./screenshots/desktop_compact_dark.png)<br>[View Image](./screenshots/desktop_compact_dark.png) |

#### Desktop (Medium/Normal Size)
| Light Mode | Dark Mode |
| :---: | :---: |
| ![Normal Light](./screenshots/desktop_normal_light.png)<br>[View Image](./screenshots/desktop_normal_light.png) | ![Normal Dark](./screenshots/desktop_normal_dark.png)<br>[View Image](./screenshots/desktop_normal_dark.png) |

#### Desktop (Expanded Size - Maximized)
| Light Mode | Dark Mode |
| :---: | :---: |
| ![Expanded Light](./screenshots/desktop_expanded_light.png)<br>[View Image](./screenshots/desktop_expanded_light.png) | ![Expanded Dark](./screenshots/desktop_expanded_dark.png)<br>[View Image](./screenshots/desktop_expanded_dark.png) |

### 2. Android Target

#### Android (Folded - Compact)
| Light Mode | Dark Mode |
| :---: | :---: |
| ![Android Folded Light](./screenshots/android_folded_light.png)<br>[View Image](./screenshots/android_folded_light.png) | ![Android Folded Dark](./screenshots/android_folded_dark.png)<br>[View Image](./screenshots/android_folded_dark.png) |

#### Android (Unfolded - Tablet)
| Light Mode | Dark Mode |
| :---: | :---: |
| ![Android Unfolded Light](./screenshots/android_unfolded_light.png)<br>[View Image](./screenshots/android_unfolded_light.png) | ![Android Unfolded Dark](./screenshots/android_unfolded_dark.png)<br>[View Image](./screenshots/android_unfolded_dark.png) |

### 3. iOS Target

#### iOS (iPhone 16 - Compact)
| Light Mode | Dark Mode |
| :---: | :---: |
| ![iOS iPhone Light](./screenshots/ios_iphone_light.png)<br>[View Image](./screenshots/ios_iphone_light.png) | ![iOS iPhone Dark](./screenshots/ios_iphone_dark.png)<br>[View Image](./screenshots/ios_iphone_dark.png) |

#### iOS (iPhone 16e - Compact / Small Device)
| Light Mode | Dark Mode |
| :---: | :---: |
| ![iOS iPhone 16e Light](./screenshots/ios_iphone_16e_light.png)<br>[View Image](./screenshots/ios_iphone_16e_light.png) | ![iOS iPhone 16e Dark](./screenshots/ios_iphone_16e_dark.png)<br>[View Image](./screenshots/ios_iphone_16e_dark.png) |

#### iOS (iPad Pro 13-inch - Tablet/Expanded)
| Light Mode | Dark Mode |
| :---: | :---: |
| ![iOS iPad Light](./screenshots/ios_ipad_light.png)<br>[View Image](./screenshots/ios_ipad_light.png) | ![iOS iPad Dark](./screenshots/ios_ipad_dark.png)<br>[View Image](./screenshots/ios_ipad_dark.png) |

### 4. Browser (Web) Target

#### Web (Browser App)
| Light Mode | Dark Mode |
| :---: | :---: |
| ![Web Light](./screenshots/web_light.png)<br>[View Image](./screenshots/web_light.png) | ![Web Dark](./screenshots/web_dark.png)<br>[View Image](./screenshots/web_dark.png) |

## Upcoming Features (Based on TMDB OpenAPI Spec)

- [ ] **Integrated Search Feature**:
  - Connect the Top Bar's search bar to a functional search results screen that aggregates movies, TV shows, and people using `/3/search/multi` (with keystroke debouncing).
- [ ] **Account Favorites & Watchlist**:
  - Add favorite/watchlist support (using guest sessions or account IDs) in the "My Fav" tab with sub-tabs for "Favorites" and "Watchlist".
  - Add a "Favorite" (heart) button on media cards to mark/unmark items.
- [ ] **Media Detailed Views (Movies & TV Shows)**:
  - Create a premium detailed screen Composable featuring a large backdrop banner image, basic metadata, horizontal cast list, trailers, and recommended media.
- [ ] **Genre-based Discovery Screen**:
  - Let users filter and explore movies/shows by genres, release year, language, or popularity sorting options using `/3/discover` endpoints.

