package com.gauravssnl.bypassrootcheck.pro

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.io.File
import java.io.IOException

class MainModule : XposedModule() {

    companion object {
        private const val TAG = "RootBypass"
    }

    private fun log(text: String) = log(Log.INFO, TAG, text)

    private val myHooker = MyHooker(::log)

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        super.onPackageLoaded(param)
        log("onPackageLoaded :: ${param.packageName}")
        log("param classloader is ::  ${param.defaultClassLoader}")
        log("module apk path is :: ${this.moduleApplicationInfo.sourceDir}")
        log("----------------------------")
        if (param.isFirstPackage) {
            log("First package loaded")
            log("Loading native lib for hooking")
            System.loadLibrary("nativehooks")
            log(" Native lib loaded for hooking root detection & bypass them :)")
            try {
                hookRootDetections(param.defaultClassLoader)
            } catch (e: Exception) {
                log("Failed to hook root detections. Exception :: ${e.stackTrace.contentToString()}")
            }
        } else {
            // Do something with other packages loaded by the app if we need
        }
    }

    private fun hookRootDetections(classLoader: ClassLoader) {
        log("Trying to find RootBeerFresh classes & hook Java methods")
        hookFileExists()
        hookGetSystemProperties(classLoader)
        hookPackageManagerGetPackageInfo(classLoader)
        hookRuntimeExec()
        log("Java & Native Java (JNI / C / C++) code are hooked. All root detection bypassed :)")
    }



    class MyHooker(private val log: (String) -> Unit) : XposedInterface.Hooker {

        override fun intercept(chain: XposedInterface.Chain): Any? {
            val method = chain.executable
            val args = chain.args

            when (method.name) {

                "setLogging" -> {
                    // enable logging
                    args[0] = true

                    return chain.proceed()
                }

                "exists" -> {
                    val file = chain.thisObject as File
                    val fileName = file.absolutePath

                    // Immediately return false if root-related files are checked
                    if (isFileRootAccessRelated(fileName)) {
                        log(
                            "App tried to access root related file :: $fileName, so bypass it"
                        )

                        return false
                    }

                    return chain.proceed()
                }

                "getPackageInfo" -> {
                    val packageName = args[0] as String

                    if (
                        knownRootAppsPackages.contains(packageName) ||
                        knownDangerousAppsPackages.contains(packageName) ||
                        knownRootCloakingPackages.contains(packageName)
                    ) {
                        log(
                            "App tried to read root related Android apps :: " +
                                    "$packageName, so bypass it"
                        )

                        throw PackageManager.NameNotFoundException(packageName)
                    }

                    return chain.proceed()
                }

                "exec" -> {
                    val command = when (val arg = args[0]) {
                        is String -> arg
                        is Array<*> -> arg.joinToString(" ") { it.toString() }
                        else -> arg.toString()
                    }

                    log(
                        "App tried to run Exec command :: $command"
                    )

                    if (
                        command.contains("su") ||
                        command.contains("magisk") ||
                        command.contains("busybox")
                    ) {
                        // Equivalent to returnAndSkip(null)
                        return null
                    }

                    if (
                        command.contains("getprop") ||
                        command.contains("mount")
                    ) {
                        // Equivalent to throwAndSkip(...)
                        throw IOException("IO Error occurred")
                    }

                    return chain.proceed()
                }

                "get" -> {
                    val propName = args[0] as String

                    // Hook SELinux check
                    if (propName == "ro.build.selinux") {
                        return "1"
                    }

                    val result = chain.proceed()

                    // Hook OS Build Tag check
                    if (propName == "ro.build.tags") {
                        return (result as? String)
                            ?.replace("test-keys", "")
                    }

                    return result
                }

                else -> {
                    return chain.proceed()
                }
            }
        }
    }

    @SuppressLint("PrivateApi")
    private fun hookGetSystemProperties(classLoader: ClassLoader) {
        val clazz = classLoader.loadClass("android.os.SystemProperties")
        hookClassMethod(clazz, "get", String::class.java);
        log("Hooked android.os.SystemProperties.get")
    }

    private fun hookFileExists() {
        hookClassMethod(File::class.java, "exists");
        log("Hooked File.exists")
    }

    @SuppressLint("PrivateApi")
    private fun hookPackageManagerGetPackageInfo(classLoader: ClassLoader) {
        val clazz = classLoader.loadClass("android.app.ApplicationPackageManager")
        clazz.declaredMethods.filter { it.name == "getPackageInfo" }.forEach{
            hook(it).intercept(myHooker)
        }
        log("Hooked PackageManagerGetPackageInfo")
    }

    private fun hookRuntimeExec() {
        val clazz = Runtime::class.java
        clazz.declaredMethods.filter { it.name == "exec" }.forEach{
            hook(it).intercept(myHooker)
        }
        log("Hooked Runtime.exec")
    }

    private fun hookClassMethod(clazz: Class<*>, methodName: String, vararg paramsClass: Class<*>) {
        val method = clazz.getDeclaredMethod(methodName, *paramsClass)
        hook(method).intercept(myHooker)
    }

    private fun hookClassMethods(clazz: Class<*>, methodNameList: List<String>,
                                 vararg paramsClass: Class<*>) {
        methodNameList.forEach {
            val method = clazz.getDeclaredMethod(it, *paramsClass)
            hook(method).intercept(myHooker)
        }
    }

}

private fun isFileRootAccessRelated(filePath: String): Boolean {
    var result = false
    for (file in rootRelatedFiles) {
        if (Utils.filePathEqualsOrEndsWith(filePath, file)) {
            result = true
            break
        }
    }
    return result
}