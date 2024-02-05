package com.gauravssnl.bypassrootcheck.pro

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker

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
                log("Failed to hook root detections.")
            }
        } else {
            // Do something with other packages loaded by the app if we need
        }
    }

    private fun hookRootDetections(classLoader: ClassLoader) {
        log("Trying to find RootBeerFresh classes & hook Java methods")
        val fileClass = classLoader.loadClass("java.io.File")
        hookClassMethod(fileClass, "exists");

        val className1 = "com.scottyab.rootbeer.RootBeer";
        val className2 = "com.kimchangyoun.rootbeerFresh.RootBeer";
        val className3 = "com.scottyab.rootbeer.util.Utils";
        val className4 = "com.kimchangyoun.rootbeerFresh.util.Utils";

        val rootBeerClass = try {
            classLoader.loadClass(className1);

        } catch (e: ClassNotFoundException) {
            log("Failed to find the class $className1. Try to find the class $className2")
            classLoader.loadClass(className2);
        }

        val methodNameList1 = listOf("isRooted", "isRootedWithoutBusyBoxCheck",
            "detectTestKeys", "isRootedWithBusyBoxCheck", "detectRootManagementApps",
            "detectPotentiallyDangerousApps", "detectRootCloakingApps", "checkForDangerousProps",
            "checkForRWPaths", "checkSuExists", "checkForNativeLibraryReadAccess")
        hookClassMethods(rootBeerClass, methodNameList1);
        val methodNameList2 = listOf("detectRootManagementApps", "detectPotentiallyDangerousApps", "detectRootCloakingApps")
        hookClassMethods(rootBeerClass, methodNameList2, arrayOf<String>()::class.java)
        hookClassMethod(rootBeerClass, "checkForBinary", String::class.java)
        hookClassMethod(rootBeerClass, "setLogging", Boolean::class.javaPrimitiveType!!)
        hookClassMethod(rootBeerClass, "isAnyPackageFromListInstalled", List::class.java)

        val rootBeerUtilClass = try {
            classLoader.loadClass(className3);

        } catch (e: ClassNotFoundException) {
            log("Failed to find the class $className3. Try to find the class $className4")
            classLoader.loadClass(className4);
        }
        hookClassMethod(rootBeerUtilClass, "isSelinuxFlagInEnabled")


        log("Java & Native Java (JNI / C / C++) code are hooked. All root detection bypassed :)")
    }
    @XposedHooker
    class MyHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: BeforeHookCallback): MyHooker {
//                module.log("Inside beforeInvocation()")
                when (callback.member.name) {
                    "setLogging" -> callback.args[0] = true // enable logging
                    "checkForBinary" -> callback.returnAndSkip(false)
                    "isRooted" -> callback.returnAndSkip(false)
                    "isRootedWithoutBusyBoxCheck" -> callback.returnAndSkip(false)
                    "isRootedWithBusyBoxCheck" -> callback.returnAndSkip(false)
                    "detectTestKeys" -> callback.returnAndSkip(false)
                    "detectRootManagementApps" -> callback.returnAndSkip(false)
                    "detectPotentiallyDangerousApps" -> callback.returnAndSkip(false)
                    "detectRootCloakingApps" -> callback.returnAndSkip(false)
                    "isAnyPackageFromListInstalled" -> callback.returnAndSkip(false)
                    "checkForDangerousProps" -> callback.returnAndSkip(false)
                    "checkForRWPaths" -> callback.returnAndSkip(false)
                    "checkSuExists" -> callback.returnAndSkip(false)
                    "checkForNativeLibraryReadAccess" -> callback.returnAndSkip(true)
                    "isSelinuxFlagInEnabled" -> callback.returnAndSkip(true)
                    "exists" -> {
                        if (callback.args.isNotEmpty()) {
                            val fileName = callback.args[0] as String
                            // we immediately return false if root related files are checked
                            if (fileName.contains("magisk") || fileName.contains("su")) callback.returnAndSkip(
                                false
                            )
                        }
                    }
                }
                return MyHooker()
            }

            @JvmStatic
            @AfterInvocation
            fun afterInvocation(callback: AfterHookCallback, param: MyHooker) {
//                module.log("Inside afterInvocation()")
            }
        }
    }

    private fun hookClassMethod(clazz: Class<*>, methodName: String, vararg paramsClass: Class<*>) {
        val method = clazz.getDeclaredMethod(methodName, *paramsClass)
        hook(method, MyHooker::class.java)
    }

    private fun hookClassMethods(clazz: Class<*>, methodNameList: List<String>, vararg paramsClass: Class<*>) {
        methodNameList.forEach {
            val method = clazz.getDeclaredMethod(it, *paramsClass)
            hook(method, MyHooker::class.java)
        }
    }
}