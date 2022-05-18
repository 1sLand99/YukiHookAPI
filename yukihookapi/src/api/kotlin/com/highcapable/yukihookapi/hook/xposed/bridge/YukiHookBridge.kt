/*
 * YukiHookAPI - An efficient Kotlin version of the Xposed Hook API.
 * Copyright (C) 2019-2022 HighCapable
 * https://github.com/fankes/YukiHookAPI
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * This file is Created by fankes on 2022/4/3.
 */
@file:Suppress("unused", "StaticFieldLeak")

package com.highcapable.yukihookapi.hook.xposed.bridge

import android.content.pm.ApplicationInfo
import android.content.res.Resources
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.YukiGenerateApi
import com.highcapable.yukihookapi.hook.factory.hasClass
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.param.type.HookEntryType
import com.highcapable.yukihookapi.hook.param.wrapper.HookParamWrapper
import com.highcapable.yukihookapi.hook.param.wrapper.PackageParamWrapper
import com.highcapable.yukihookapi.hook.xposed.YukiHookModuleStatus
import com.highcapable.yukihookapi.hook.xposed.bridge.dummy.YukiModuleResources
import com.highcapable.yukihookapi.hook.xposed.bridge.dummy.YukiResources
import com.highcapable.yukihookapi.hook.xposed.bridge.inject.YukiHookBridge_Injector
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method

/**
 * 这是一个对接 Xposed Hook 入口与 [XposedBridge] 的装载类实现桥
 *
 * 实现与 [IXposedHookZygoteInit]、[IXposedHookLoadPackage]、[IXposedHookInitPackageResources] 接口通讯
 *
 * - ❗装载代码将自动生成 - 请勿修改或移动以及重命名此类的任何方法与变量
 */
@YukiGenerateApi
object YukiHookBridge {

    /** Android 系统框架名称 */
    @PublishedApi
    internal const val SYSTEM_FRAMEWORK_NAME = "android"

    /** Xposed 是否装载完成 */
    private var isXposedInitialized = false

    /** 已在 [PackageParam] 中被装载的 APP 包名 */
    private val loadedPackageNames = HashSet<String>()

    /** 当前 [PackageParamWrapper] 实例数组 */
    private var packageParamWrappers = HashMap<String, PackageParamWrapper>()

    /** 当前 [PackageParam] 方法体回调 */
    internal var packageParamCallback: (PackageParam.() -> Unit)? = null

    /** 当前 Xposed 模块自身 APK 路径 */
    internal var moduleAppFilePath = ""

    /** 当前 Xposed 模块自身 [Resources] */
    internal var moduleAppResources: YukiModuleResources? = null

    /**
     * 获取当前 Xposed 模块自身动态 [Resources]
     * @return [YukiModuleResources] or null
     */
    internal val dynamicModuleAppResources get() = runCatching { YukiModuleResources.createInstance(moduleAppFilePath) }.getOrNull()

    /**
     * 自动生成的 Xposed 模块构建版本号
     * @return [String]
     */
    internal val moduleGeneratedVersion get() = YukiHookBridge_Injector.getModuleGeneratedVersion()

    /**
     * 模块是否装载了 Xposed 回调方法
     *
     * - ❗装载代码将自动生成 - 请勿手动修改 - 会引发未知异常
     * @return [Boolean]
     */
    @YukiGenerateApi
    val isXposedCallbackSetUp
        get() = isXposedInitialized.not() && packageParamCallback != null

    /**
     * 预设的 Xposed 模块包名
     *
     * - ❗装载代码将自动生成 - 请勿手动修改 - 会引发未知异常
     */
    @YukiGenerateApi
    var modulePackageName = ""

    /**
     * 获取当前 Hook 框架的名称
     *
     * 从 [XposedBridge] 获取 TAG
     * @return [String] 无法获取会返回 unknown - [hasXposedBridge] 不存在会返回 invalid
     */
    internal val executorName
        get() = runCatching {
            (Hooker.findField(XposedBridge::class.java, name = "TAG").get(null) as? String?)
                ?.replace(oldValue = "Bridge", newValue = "")?.replace(oldValue = "-", newValue = "")?.trim() ?: "unknown"
        }.getOrNull() ?: "invalid"

    /**
     * 获取当前 Hook 框架的版本
     *
     * 获取 [XposedBridge.getXposedVersion]
     * @return [Int] 无法获取会返回 -1
     */
    internal val executorVersion get() = runCatching { XposedBridge.getXposedVersion() }.getOrNull() ?: -1

    /**
     * 是否存在 [XposedBridge]
     * @return [Boolean]
     */
    internal val hasXposedBridge get() = executorVersion >= 0

    /**
     * 自动忽略 MIUI 系统可能出现的日志收集注入实例
     * @param packageName 当前包名
     * @return [Boolean] 是否存在
     */
    private fun isMiuiCatcherPatch(packageName: String?) =
        (packageName == "com.miui.contentcatcher" || packageName == "com.miui.catcherpatch") && ("android.miui.R").hasClass

    /**
     * 当前包名是否已在指定的 [HookEntryType] 被装载
     * @param packageName 包名
     * @param type 当前 Hook 类型
     * @return [Boolean] 是否已被装载
     */
    private fun isPackageLoaded(packageName: String, type: HookEntryType): Boolean {
        if (loadedPackageNames.contains("$packageName:$type")) return true
        loadedPackageNames.add("$packageName:$type")
        return false
    }

    /**
     * 创建、修改 [PackageParamWrapper]
     * @param type 当前正在进行的 Hook 类型
     * @param packageName 包名
     * @param processName 当前进程名
     * @param appClassLoader APP [ClassLoader]
     * @param appInfo APP [ApplicationInfo]
     * @param appResources APP [YukiResources]
     * @return [PackageParamWrapper] or null
     */
    private fun assignWrapper(
        type: HookEntryType,
        packageName: String?,
        processName: String? = "",
        appClassLoader: ClassLoader? = null,
        appInfo: ApplicationInfo? = null,
        appResources: YukiResources? = null
    ) = run {
        if (packageParamWrappers[packageName] == null)
            PackageParamWrapper(
                type = type,
                packageName = packageName ?: SYSTEM_FRAMEWORK_NAME,
                processName = processName ?: SYSTEM_FRAMEWORK_NAME,
                appClassLoader = appClassLoader ?: XposedBridge.BOOTCLASSLOADER,
                appInfo = appInfo,
                appResources = appResources
            ).also { packageParamWrappers[packageName ?: SYSTEM_FRAMEWORK_NAME] = it }
        else packageParamWrappers[packageName]?.also {
            it.type = type
            if (packageName?.isNotBlank() == true) it.packageName = packageName
            if (processName?.isNotBlank() == true) it.processName = processName
            if (appClassLoader != null) it.appClassLoader = appClassLoader
            if (appInfo != null) it.appInfo = appInfo
            if (appResources != null) it.appResources = appResources
        }
    }

    /** 刷新当前 Xposed 模块自身 [Resources] */
    internal fun refreshModuleAppResources() {
        dynamicModuleAppResources?.let { moduleAppResources = it }
    }

    /**
     * Hook 模块自身激活状态和 Resources Hook 支持状态
     *
     * - ❗装载代码将自动生成 - 你不应该手动使用此方法装载 Xposed 模块事件
     * @param classLoader 模块的 [ClassLoader]
     * @param isHookResourcesStatus 是否 Hook Resources 支持状态
     */
    @YukiGenerateApi
    fun hookModuleAppStatus(classLoader: ClassLoader?, isHookResourcesStatus: Boolean = false) {
        Hooker.findClass(classLoader, YukiHookModuleStatus::class.java).also { statusClass ->
            if (isHookResourcesStatus.not()) {
                Hooker.hookMethod(Hooker.findMethod(statusClass, YukiHookModuleStatus.IS_ACTIVE_METHOD_NAME),
                    object : Hooker.YukiMemberReplacement() {
                        override fun replaceHookedMember(wrapper: HookParamWrapper) = true
                    })
                Hooker.hookMethod(Hooker.findMethod(statusClass, YukiHookModuleStatus.GET_XPOSED_TAG_METHOD_NAME),
                    object : Hooker.YukiMemberReplacement() {
                        override fun replaceHookedMember(wrapper: HookParamWrapper) = executorName
                    })
                Hooker.hookMethod(Hooker.findMethod(statusClass, YukiHookModuleStatus.GET_XPOSED_VERSION_METHOD_NAME),
                    object : Hooker.YukiMemberReplacement() {
                        override fun replaceHookedMember(wrapper: HookParamWrapper) = executorVersion
                    })
            } else
                Hooker.hookMethod(Hooker.findMethod(statusClass, YukiHookModuleStatus.HAS_RESOURCES_HOOK_METHOD_NAME),
                    object : Hooker.YukiMemberReplacement() {
                        override fun replaceHookedMember(wrapper: HookParamWrapper) = true
                    })
        }
    }

    /**
     * 标识 Xposed API 装载完成
     *
     * - ❗装载代码将自动生成 - 你不应该手动使用此方法装载 Xposed 模块事件
     */
    @YukiGenerateApi
    fun callXposedInitialized() {
        isXposedInitialized = true
    }

    /**
     * 装载 Xposed API Zygote 回调
     *
     * - ❗装载代码将自动生成 - 你不应该手动使用此方法装载 Xposed 模块事件
     * @param sparam Xposed [IXposedHookZygoteInit.StartupParam]
     */
    @YukiGenerateApi
    fun callXposedZygoteLoaded(sparam: IXposedHookZygoteInit.StartupParam) {
        moduleAppFilePath = sparam.modulePath
        refreshModuleAppResources()
    }

    /**
     * 装载 Xposed API 回调
     *
     * 这里的入口会装载三次
     *
     * - 在 [IXposedHookZygoteInit.initZygote] 时装载
     *
     * - 在 [IXposedHookLoadPackage.handleLoadPackage] 时装载
     *
     * - 在 [IXposedHookInitPackageResources.handleInitPackageResources] 时装载
     *
     * - ❗装载代码将自动生成 - 你不应该手动使用此方法装载 Xposed API 事件
     * @param isZygoteLoaded 是否为 Xposed [IXposedHookZygoteInit.initZygote]
     * @param lpparam Xposed [XC_LoadPackage.LoadPackageParam]
     * @param resparam Xposed [XC_InitPackageResources.InitPackageResourcesParam]
     */
    @YukiGenerateApi
    fun callXposedLoaded(
        isZygoteLoaded: Boolean,
        lpparam: XC_LoadPackage.LoadPackageParam? = null,
        resparam: XC_InitPackageResources.InitPackageResourcesParam? = null
    ) {
        if (isMiuiCatcherPatch(packageName = lpparam?.packageName ?: resparam?.packageName).not()) when {
            isZygoteLoaded -> assignWrapper(HookEntryType.ZYGOTE, SYSTEM_FRAMEWORK_NAME, SYSTEM_FRAMEWORK_NAME)
            lpparam != null ->
                if (isPackageLoaded(lpparam.packageName, HookEntryType.PACKAGE).not())
                    assignWrapper(HookEntryType.PACKAGE, lpparam.packageName, lpparam.processName, lpparam.classLoader, lpparam.appInfo)
                else null
            resparam != null ->
                if (isPackageLoaded(resparam.packageName, HookEntryType.RESOURCES).not())
                    assignWrapper(HookEntryType.RESOURCES, resparam.packageName, appResources = YukiResources.createFromXResources(resparam.res))
                else null
            else -> null
        }?.also { YukiHookAPI.onXposedLoaded(it) }
    }

    /**
     * Hook 核心功能实现实例
     *
     * 对接 [XposedBridge] 实现 Hook 功能
     */
    internal object Hooker {

        /** 默认 Hook 回调优先级 */
        internal const val PRIORITY_DEFAULT = 50

        /** 延迟回调 Hook 方法结果 */
        internal const val PRIORITY_LOWEST = -10000

        /** 更快回调 Hook 方法结果 */
        internal const val PRIORITY_HIGHEST = 10000

        /**
         * 查找 [Class]
         * @param classLoader 当前 [ClassLoader]
         * @param baseClass 当前类
         * @return [Field]
         * @throws IllegalStateException 如果 [ClassLoader] 为空
         */
        internal fun findClass(classLoader: ClassLoader?, baseClass: Class<*>) =
            classLoader?.loadClass(baseClass.name) ?: error("ClassLoader is null")

        /**
         * 查找变量
         * @param baseClass 所在类
         * @param name 变量名称
         * @return [Field]
         * @throws NoSuchFieldError 如果找不到变量
         */
        internal fun findField(baseClass: Class<*>, name: String) = baseClass.getDeclaredField(name).apply { isAccessible = true }

        /**
         * 查找方法
         * @param baseClass 所在类
         * @param name 方法名称
         * @param paramTypes 方法参数
         * @return [Method]
         * @throws NoSuchMethodError 如果找不到方法
         */
        internal fun findMethod(baseClass: Class<*>, name: String, vararg paramTypes: Class<*>) =
            baseClass.getDeclaredMethod(name, *paramTypes).apply { isAccessible = true }

        /**
         * Hook 方法
         *
         * 对接 [XposedBridge.hookMethod]
         * @param hookMethod 需要 Hook 的方法、构造方法
         * @param callback 回调
         * @return [Member] or null
         */
        internal fun hookMethod(hookMethod: Member?, callback: YukiHookCallback) =
            XposedBridge.hookMethod(hookMethod, compatCallback(callback))?.hookedMethod

        /**
         * Hook 当前 [hookClass] 所有 [methodName] 的方法
         *
         * 对接 [XposedBridge.hookAllMethods]
         * @param hookClass 当前 Hook 的 [Class]
         * @param methodName 方法名
         * @param callback 回调
         * @return [HashSet] 成功 Hook 的方法数组
         */
        internal fun hookAllMethods(hookClass: Class<*>?, methodName: String, callback: YukiHookCallback) = HashSet<Member>().also {
            XposedBridge.hookAllMethods(hookClass, methodName, compatCallback(callback)).takeIf { it.isNotEmpty() }
                ?.forEach { e -> it.add(e.hookedMethod) }
        }

        /**
         * Hook 当前 [hookClass] 所有构造方法
         *
         * 对接 [XposedBridge.hookAllConstructors]
         * @param hookClass 当前 Hook 的 [Class]
         * @param callback 回调
         * @return [HashSet] 成功 Hook 的构造方法数组
         */
        internal fun hookAllConstructors(hookClass: Class<*>?, callback: YukiHookCallback) = HashSet<Member>().also {
            XposedBridge.hookAllConstructors(hookClass, compatCallback(callback)).takeIf { it.isNotEmpty() }
                ?.forEach { e -> it.add(e.hookedMethod) }
        }

        /**
         * 兼容对接 Hook 回调接口
         * @param callback [YukiHookCallback] 接口
         * @return [XC_MethodHook] 原始接口
         */
        private fun compatCallback(callback: YukiHookCallback) = when (callback) {
            is YukiMemberHook -> object : XC_MethodHook(callback.priority) {

                /** 创建 Hook 前 [HookParamWrapper] */
                val beforeHookWrapper = HookParamWrapper()

                /** 创建 Hook 后 [HookParamWrapper] */
                val afterHookWrapper = HookParamWrapper()

                override fun beforeHookedMethod(param: MethodHookParam?) {
                    if (param == null) return
                    callback.beforeHookedMember(beforeHookWrapper.assign(param))
                }

                override fun afterHookedMethod(param: MethodHookParam?) {
                    if (param == null) return
                    callback.afterHookedMember(afterHookWrapper.assign(param))
                }
            }
            is YukiMemberReplacement -> object : XC_MethodReplacement(callback.priority) {

                /** 创建替换 Hook [HookParamWrapper] */
                val replaceHookWrapper = HookParamWrapper()

                override fun replaceHookedMethod(param: MethodHookParam?): Any? {
                    if (param == null) return null
                    return callback.replaceHookedMember(replaceHookWrapper.assign(param))
                }
            }
            else -> error("Invalid YukiHookCallback type")
        }

        /**
         * Hook 方法回调接口
         * @param priority Hook 优先级 - 默认 [PRIORITY_DEFAULT]
         */
        internal abstract class YukiMemberHook(override val priority: Int = PRIORITY_DEFAULT) : YukiHookCallback(priority) {

            /**
             * 在方法执行之前注入
             * @param wrapper 包装实例
             */
            open fun beforeHookedMember(wrapper: HookParamWrapper) {}

            /**
             * 在方法执行之后注入
             * @param wrapper 包装实例
             */
            open fun afterHookedMember(wrapper: HookParamWrapper) {}
        }

        /**
         * Hook 替换方法回调接口
         * @param priority Hook 优先级- 默认 [PRIORITY_DEFAULT]
         */
        internal abstract class YukiMemberReplacement(override val priority: Int = PRIORITY_DEFAULT) : YukiHookCallback(priority) {

            /**
             * 拦截替换为指定结果
             * @param wrapper 包装实例
             * @return [Any] or null
             */
            abstract fun replaceHookedMember(wrapper: HookParamWrapper): Any?
        }

        /**
         * Hook 回调接口父类
         * @param priority Hook 优先级
         */
        internal abstract class YukiHookCallback(open val priority: Int)
    }
}