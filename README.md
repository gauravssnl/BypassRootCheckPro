# Bypass Root Check Pro

Bypass Root Check Pro Xposed Module with Java and Native (C/C++) Hooks based on Modern Xposed API by LSPosed. This module bypasses root checks the pro way. This module not only hooks Java logic, but also hooks Native / System calls logic (logic written in C/C++/ Native Java code).

# Features

1. Bypass all  [RootBeerFresh](https://github.com/KimChangYoun/rootbeerFresh) detections.
2. Bypass all [RootBeer](https://github.com/scottyab/rootbeer) detections 
3. Bypass SELinux test to avoid root detection.
4. Bypass R/W System check to avoid root detection.
5. Bypass check for su, magisk & busybox binaries.
6. Bypass check for root related Android apps.
7. Bypass check OS Build Test Keys presence.
8. Bypass check for props related to root.
9. Bypass check for root cloaking apps.
10. Bypass check for Unix Domain Socket used by Magisk.
11. System Native Hooks in C/C++ apart from Java hooks to make root hiding possible.

Screenshots have been provided at the end of this doc as a proof. Please have a look.

# Requirements
1. Rooted Android phone
2. LSposed 

# Usage

Install this Xposed module in LSposed and activate the module. While activating the module, please select the desired app which perform root access check & you want to bypass.

To test our module, we can install RootBeer Fresh sample application.

# Implementation

###  Native Hooks

The below Native System calls have been hooked to avoid root detection

    1. fopen()
    2. stat()
    3. lstat()


For more details & updated logic, refer to the C/C++ hooks code present in the repo.

### Java Hooks

The below Java classes methods have been hooked to avoid root detection

    1. java.io.File.exists()
    2. android.app.ApplicationPackageManager.getPackageInfo()
    3. Runtime.exec()

For more details & updated logic, refer to the Kotlin/Java hooks code present in the repo.

# Author

Made with ❤️ by @gauravssnl

Pull Requests & contributions are welcome. If you like my work, give this repo a star and share.

# Screenshots

Before Module Activation 

![ScreenShot](https://github.com/gauravssnl/BypassRootCheckPro/blob/main/screenshots/before.png)

After Module Activation

![ScreenShot](https://github.com/gauravssnl/BypassRootCheckPro/blob/main/screenshots/after.png)