package com.gauravssnl.bypassrootcheck.pro

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.io.File
import java.io.IOException

private lateinit var module: MainModule

class MainModule(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {
    init {
        log("MainModule at :: " + param.processName)
        module = this
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        super.onPackageLoaded(param)
        log("onPackageLoaded :: ${param.packageName}")
        log("param classloader is ::  ${param.classLoader}")
        log("module apk path is :: ${this.applicationInfo.sourceDir}")
        log("----------------------------")
        if (param.isFirstPackage) {
            log("First package loaded")
            log("Loading native lib for hooking")
            System.loadLibrary("nativehooks")
            log(" Native lib loaded for hooking root detection & bypass them :)")
            try {
                hookRootDetections(param.classLoader)
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

    @Suppress("unused")
    @XposedHooker
    class MyHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback): MyHooker {
                when (callback.member.name) {
                    "setLogging" -> callback.args[0] = true // enable logging
                    "exists" -> {
                            val file = callback.thisObject as File
                            val fileName = file.absolutePath
                            // we immediately return false if root related files are checked
                            if (isFileRootAccessRelated(fileName)) {
                                module.log("App tried to access root related file :: $fileName , so bypass it")
                                callback.returnAndSkip(false)
                            }
                    }
                    "getPackageInfo" -> {
                        val packageName = callback.args[0] as String
                        if(knownRootAppsPackages.contains(packageName)
                            || knownDangerousAppsPackages.contains(packageName)
                            || knownRootCloakingPackages.contains(packageName)) {
                            module.log("App tried to read root related Android apps :: $packageName , so bypass it")
                            callback.throwAndSkip(PackageManager.NameNotFoundException(packageName))
                        }
                    }
                    "exec" -> {
                        // Args can be either String or Array; so we need to handle
                        val command: String = try {
                            callback.args[0] as String
                        } catch (e: ClassCastException) {
                            val firstArg = callback.args[0] as Array<*>
                            firstArg.joinToString(" ") { it.toString() }
                        }
                        module.log("App tried to run Exec command  :: $command")
                        if(command.contains("su") || command.contains("magisk")
                            || command.contains("busybox") )
                            callback.returnAndSkip(null);
                        if(command.contains("getprop") || command.contains("mount"))
                            callback.throwAndSkip(IOException("IO Error occurred"));
                    }
                    "get" -> {
                        val propName =  callback.args[0] as String
                        // Hook SELinux check
                        if ("ro.build.selinux" == propName) {
                            callback.returnAndSkip("1")
                        }
                    }
                }
                return MyHooker()
            }

            @JvmStatic
            @AfterInvocation
            fun afterInvocation(callback: AfterHookCallback, param: MyHooker) {
                when(callback.member.name) {
                    "get" -> {
                        val propName =  callback.args[0] as String
                        // Hook OS Build Tag check
                        if ("ro.build.tags" == propName) {
                            val res = callback.result as String
                            callback.result = res.replace("test-keys", "")
                        }
                    }
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
            hook(it, MyHooker::class.java)
        }
        log("Hooked PackageManagerGetPackageInfo")
    }

    private fun hookRuntimeExec() {
        val clazz = Runtime::class.java
        clazz.declaredMethods.filter { it.name == "exec" }.forEach{
            hook(it, MyHooker::class.java)
        }
        log("Hooked Runtime.exec")
    }

    private fun hookClassMethod(clazz: Class<*>, methodName: String, vararg paramsClass: Class<*>) {
        val method = clazz.getDeclaredMethod(methodName, *paramsClass)
        hook(method, MyHooker::class.java)
    }

    private fun hookClassMethods(clazz: Class<*>, methodNameList: List<String>,
                                 vararg paramsClass: Class<*>) {
        methodNameList.forEach {
            val method = clazz.getDeclaredMethod(it, *paramsClass)
            hook(method, MyHooker::class.java)
        }
    }

}

private fun isFileRootAccessRelated(fileName: String): Boolean {
    return fileName.contains("magisk") || fileName.contains("su")
            || fileName.contains("busybox") || fileName.contains("Superuser")
            || fileName.contains("daemonsu") || fileName.contains("SuperSU")
            || fileName.contains("/data/adb/.boot_count")
}