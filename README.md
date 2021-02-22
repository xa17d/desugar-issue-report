# Desugar Issue Report

The goal of this project is to reproduce an issue with [desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring) when desugared API is used in an external dependency.
The issue was reported on [Google's Issue Tracker #179800061](https://issuetracker.google.com/issues/179800061).

## Setup

The dependency setup looks like this:
```
   ┌─────────┐
   │   app   │
   └─────────┘
        │  internal project dependency
        ▼
   ┌─────────┐
   │ my-lib  │
   └─────────┘
        │  maven dependency
        ▼
┌──────────────┐
│ external-lib │     ◄─── uses java.time internally
└──────────────┘
```

In our real-world use case `:external-lib` is a library in another repository we don't have control over.
It's included via maven.
To make this example self-contained, I added `:external-lib` to this repository, included the compiled `.aar` file in the `localmaven` folder and also added `maven { url "$rootProject.projectDir/localmaven" }` to the `repositories` setup.
Instead of `project(…)`-dependency, `:external-lib` is included as external maven dependency.

## Problem Description

If `java.time` is used in an external dependency (included via maven), that is included in an android library, the desugared `java.time` class is **not** included in the final APK.
Therefore, the app crashes with a `NoClassDefFoundError`.

I also investigated all `desugar_*_keep_rules` folders in `:app`, but none of them contain the used and desugared `java.time` class.
However, decompiling the APK shows that the `java.time` class usage was properly desugared to `Lj$/time/…`.

**Reproducing the issue in this example:**

`MyUtil` is a custom class in `:my-lib`, which uses `ExternalUtil` which is defined in `:external-lib` and included via external maven dependency.
`ExternalUtil` uses `java.time` internally.

`MainActivity` within `:app` uses class `MyUtil`, which again uses `ExternalUtil` which is included via external maven dependency, and `ExternalUtil` uses `java.time` internally.

The app crashes on startup for _release_ builds:
```bash
./gradlew :app:assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
adb shell am start -a android.intent.action.MAIN -n at.xa1.example.issuereport/at.xa1.example.issuereport.MainActivity
```

I also created an [Instrumentation Test](app/src/androidTest/java/at/xa1/example/issuereport/ExternalDependencyCanUseJavaTime.kt) to reproduce the issue. It can be run with:
```bash
./gradlew :app:connectedReleaseAndroidTest
```

Exception (same in both, the app and the instrumentation test, only the calling stacktrace is different of course):
```
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: at.xa1.example.issuereport, PID: 23051
    java.lang.NoClassDefFoundError: Failed resolution of: Lj$/time/Duration;
        at at.xa1.example.issuereport.externallib.ExternalUtil.useSomeJavaTimeStuffInternally(ExternalUtil.kt:7)
        at at.xa1.example.issuereport.mylib.MyUtil.useSomeJavaTimeStuffInternally(MyUtil.kt:7)
        at at.xa1.example.issuereport.MainActivity.onCreate(MainActivity.kt:12)
        at android.app.Activity.performCreate(Activity.java:8000)
        at android.app.Activity.performCreate(Activity.java:7984)
        at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1309)
        at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3404)
        at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3595)
        at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:85)
        at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:135)
        at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:95)
        at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2066)
        at android.os.Handler.dispatchMessage(Handler.java:106)
        at android.os.Looper.loop(Looper.java:223)
        at android.app.ActivityThread.main(ActivityThread.java:7660)
        at java.lang.reflect.Method.invoke(Native Method)
        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:592)
        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)
     Caused by: java.lang.ClassNotFoundException: j$.time.Duration
        at at.xa1.example.issuereport.externallib.ExternalUtil.useSomeJavaTimeStuffInternally(ExternalUtil.kt:7)
        at at.xa1.example.issuereport.mylib.MyUtil.useSomeJavaTimeStuffInternally(MyUtil.kt:7)
        at at.xa1.example.issuereport.MainActivity.onCreate(MainActivity.kt:12)
        at android.app.Activity.performCreate(Activity.java:8000)
        at android.app.Activity.performCreate(Activity.java:7984)
        at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1309)
        at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3404)
        at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3595)
        at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:85)
        at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:135)
        at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:95)
        at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2066)
        at android.os.Handler.dispatchMessage(Handler.java:106)
        at android.os.Looper.loop(Looper.java:223)
        at android.app.ActivityThread.main(ActivityThread.java:7660)
        at java.lang.reflect.Method.invoke(Native Method)
        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:592)
        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)
```

### Expected Behavior

It is expected that the app doesn't crash, because `Lj$/time/Duration` is available at runtime.

**Note #1:** Everything works as expected with a _debug_ build:
```
adb uninstall at.xa1.example.issuereport

./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -a android.intent.action.MAIN -n at.xa1.example.issuereport/at.xa1.example.issuereport.MainActivity
```
The app doesn't crash!

**Note #2:** Everything works as expected (even for _release_ builds) when `implementation "at.xa1.example:external-lib:1.0.0"` is additionally added to `app/build.gradle`. (as posted on branch: [workaround/app-dependency](https://github.com/xa17d/desugar-issue-report/compare/workaround/app-dependency))

**Note #3:** Everything works as expected (even for _release_ builds) when the dependency to `"at.xa1.example:external-lib:1.0.0"` is converted from `implementation` to `api` in `my-lib/build.gradle`. (as posted on branch: [workaround/lib-api-dependency](https://github.com/xa17d/desugar-issue-report/compare/workaround/lib-api-dependency))
