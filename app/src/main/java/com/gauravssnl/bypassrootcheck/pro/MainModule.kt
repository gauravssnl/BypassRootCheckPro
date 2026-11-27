package com.gauravssnl.bypassrootcheck.pro

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.io.File
import java.io.IOException

private const val TAG = "BypassRootCheckPro"

private lateinit var module: MainModule

/**
 * Module entry point for the modern libxposed API (API 101+).
 *
 * API 102 removed the annotation based hooker API (@XposedHooker / @BeforeInvocation /
 * @AfterInvocation). Hooks are now registered through
 * [io.github.libxposed.api.XposedInterface.HookBuilder.intercept] and implemented as an
 * interceptor chain, so before/after logic lives in a single [Hooker.intercept] method.
 *
 * Hot reload stays disabled (autoHotReload=false): the module installs native hooks and
 * loads libnativehooks.so, which cannot be safely unloaded during a hot reload.
 */
class MainModule : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        module = this
        log(Log.INFO, TAG, "MainModule at :: ${param.processName}")
        log(Log.INFO, TAG, "framework :: $frameworkName $frameworkVersion (API $apiVersion)")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        log(Log.INFO, TAG, "onPackageLoaded :: ${param.packageName}")
        log(Log.INFO, TAG, "param classloader is ::  ${param.defaultClassLoader}")
        log(Log.INFO, TAG, "module apk path is :: ${moduleApplicationInfo.sourceDir}")
        log(Log.INFO, TAG, "----------------------------")
        if (param.isFirstPackage) {
            log(Log.INFO, TAG, "First package loaded")
            log(Log.INFO, TAG, "Loading native lib for hooking")
            try {
                System.loadLibrary("nativehooks")
                log(Log.INFO, TAG, " Native lib loaded for hooking root detection & bypass them :)")
                hookRootDetections(param.defaultClassLoader)
            } catch (e: Exception) {
                log(Log.ERROR, TAG, "Failed to hook root detections. Exception :: $e", e)
            }
        } else {
            // Do something with other packages loaded by the app if we need
        }
    }

    private fun hookRootDetections(classLoader: ClassLoader) {
        log(Log.INFO, TAG, "Trying to find RootBeerFresh classes & hook Java methods")
        hookFileExists()
        hookGetSystemProperties(classLoader)
        hookPackageManagerGetPackageInfo(classLoader)
        hookRuntimeExec()
        log(Log.INFO, TAG, "Java & Native Java (JNI / C / C++) code are hooked. All root detection bypassed :)")
    }

    /**
     * File.exists(): report "not found" for root related paths.
     */
    private object FileExistsHooker : Hooker {
        override fun intercept(chain: Chain): Any? {
            val file = chain.thisObject as? File ?: return chain.proceed()
            val fileName = file.absolutePath
            // we immediately return false if root related files are checked
            if (isFileRootAccessRelated(fileName)) {
                module.log(Log.INFO, TAG, "App tried to access root related file :: $fileName , so bypass it")
                return false
            }
            return chain.proceed()
        }
    }

    /**
     * SystemProperties.get(): before/after logic of the old @BeforeInvocation/@AfterInvocation pair.
     */
    private object SystemPropertiesHooker : Hooker {
        override fun intercept(chain: Chain): Any? {
            val propName = chain.getArg(0) as? String ?: return chain.proceed()
            // Hook SELinux check
            if (propName == "ro.build.selinux") {
                return "1"
            }
            val result = chain.proceed()
            // Hook OS Build Tag check
            if (propName == "ro.build.tags") {
                return (result as? String)?.replace("test-keys", "")
            }
            return result
        }
    }

    /**
     * PackageManager.getPackageInfo(): pretend that root related apps are not installed.
     */
    private object PackageInfoHooker : Hooker {
        override fun intercept(chain: Chain): Any? {
            val packageName = chain.getArg(0) as? String ?: return chain.proceed()
            if (knownRootAppsPackages.contains(packageName)
                || knownDangerousAppsPackages.contains(packageName)
                || knownRootCloakingPackages.contains(packageName)
            ) {
                module.log(Log.INFO, TAG, "App tried to read root related Android apps :: $packageName , so bypass it")
                throw PackageManager.NameNotFoundException(packageName)
            }
            return chain.proceed()
        }
    }

    /**
     * Runtime.exec(): hide su/magisk/busybox commands and break getprop/mount calls.
     */
    private object RuntimeExecHooker : Hooker {
        override fun intercept(chain: Chain): Any? {
            // Args can be either String or Array; so we need to handle
            val command: String = when (val arg = chain.getArg(0)) {
                is String -> arg
                is Array<*> -> arg.joinToString(" ") { it.toString() }
                else -> return chain.proceed()
            }
            module.log(Log.INFO, TAG, "App tried to run Exec command  :: $command")
            if (command.contains("su") || command.contains("magisk")
                || command.contains("busybox")
            ) {
                return null
            }
            if (command.contains("getprop") || command.contains("mount")) {
                throw IOException("IO Error occurred")
            }
            return chain.proceed()
        }
    }

    @SuppressLint("PrivateApi")
    private fun hookGetSystemProperties(classLoader: ClassLoader) {
        val clazz = classLoader.loadClass("android.os.SystemProperties")
        val method = clazz.getDeclaredMethod("get", String::class.java)
        // No exceptions are thrown by this hooker, so the default PROTECTIVE mode is enough.
        hook(method).intercept(SystemPropertiesHooker)
        log(Log.INFO, TAG, "Hooked android.os.SystemProperties.get")
    }

    private fun hookFileExists() {
        val method = File::class.java.getDeclaredMethod("exists")
        hook(method).intercept(FileExistsHooker)
        log(Log.INFO, TAG, "Hooked File.exists")
    }

    @SuppressLint("PrivateApi")
    private fun hookPackageManagerGetPackageInfo(classLoader: ClassLoader) {
        val clazz = classLoader.loadClass("android.app.ApplicationPackageManager")
        clazz.declaredMethods.filter { it.name == "getPackageInfo" }.forEach {
            // The hooker throws NameNotFoundException on purpose, and the default PROTECTIVE
            // exception mode would swallow it, so switch this hook to PASSTHROUGH.
            hook(it)
                .setExceptionMode(ExceptionMode.PASSTHROUGH)
                .intercept(PackageInfoHooker)
        }
        log(Log.INFO, TAG, "Hooked PackageManagerGetPackageInfo")
    }

    private fun hookRuntimeExec() {
        Runtime::class.java.declaredMethods.filter { it.name == "exec" }.forEach {
            // Same as above: IOException must reach the caller.
            hook(it)
                .setExceptionMode(ExceptionMode.PASSTHROUGH)
                .intercept(RuntimeExecHooker)
        }
        log(Log.INFO, TAG, "Hooked Runtime.exec")
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
