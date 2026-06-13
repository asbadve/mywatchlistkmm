# Compose Multiplatform Application

## Before running!
- check your system with [KDoctor](https://github.com/Kotlin/kdoctor)
- install JDK 8 on your machine
- add `local.properties` file to the project root and set a path to Android SDK there

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

#### iOS (iPad Pro 13-inch - Tablet/Expanded)
| Light Mode | Dark Mode |
| :---: | :---: |
| ![iOS iPad Light](./screenshots/ios_ipad_light.png)<br>[View Image](./screenshots/ios_ipad_light.png) | ![iOS iPad Dark](./screenshots/ios_ipad_dark.png)<br>[View Image](./screenshots/ios_ipad_dark.png) |
