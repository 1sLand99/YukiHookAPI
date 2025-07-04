/*
 * YukiHookAPI - An efficient Hook API and Xposed Module solution built in Kotlin.
 * Copyright (C) 2019 HighCapable
 * https://github.com/HighCapable/YukiHookAPI
 *
 * Apache License Version 2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is created by fankes on 2022/9/12.
 */
@file:Suppress("MemberVisibilityCanBePrivate", "DEPRECATION", "TYPEALIAS_EXPANSION_DEPRECATION")

package com.highcapable.yukihookapi.hook.core.finder.classes.rules

import com.highcapable.yukihookapi.hook.bean.VariousClass
import com.highcapable.yukihookapi.hook.core.finder.classes.rules.base.BaseRules
import com.highcapable.yukihookapi.hook.core.finder.classes.rules.result.MemberRulesResult
import com.highcapable.yukihookapi.hook.core.finder.members.data.ConstructorRulesData
import com.highcapable.yukihookapi.hook.core.finder.type.factory.CountConditions
import com.highcapable.yukihookapi.hook.core.finder.type.factory.ModifierConditions
import com.highcapable.yukihookapi.hook.core.finder.type.factory.ObjectsConditions
import com.highcapable.yukihookapi.hook.core.finder.ReflectionMigration
import com.highcapable.yukihookapi.hook.type.defined.UndefinedType
import com.highcapable.yukihookapi.hook.type.defined.VagueType
import java.lang.reflect.Constructor

/**
 * [Constructor] 查找条件实现类
 * @param rulesData 当前查找条件规则数据
 */
@Deprecated(ReflectionMigration.KAVAREF_INFO)
class ConstructorRules internal constructor(private val rulesData: ConstructorRulesData) : BaseRules() {

    /**
     * 设置 [Constructor] 参数个数
     *
     * 你可以不使用 [param] 指定参数类型而是仅使用此变量指定参数个数
     *
     * 若参数个数小于零则忽略并使用 [param]
     * @return [Int]
     */
    @Deprecated(ReflectionMigration.KAVAREF_INFO)
    var paramCount
        get() = rulesData.paramCount
        set(value) {
            rulesData.paramCount = value
        }

    /**
     * 设置 [Constructor] 标识符筛选条件
     *
     * - 可不设置筛选条件
     * @param conditions 条件方法体
     */
    @Deprecated(ReflectionMigration.KAVAREF_INFO)
    fun modifiers(conditions: ModifierConditions) {
        rulesData.modifiers = conditions
    }

    /** 设置 [Constructor] 空参数、无参数 */
    @Deprecated(ReflectionMigration.KAVAREF_INFO)
    fun emptyParam() {
        rulesData.paramCount = 0
    }

    /**
     * 设置 [Constructor] 参数
     *
     * 如果同时使用了 [paramCount] 则 [paramType] 的数量必须与 [paramCount] 完全匹配
     *
     * 如果 [Constructor] 中存在一些无意义又很长的类型 - 你可以使用 [VagueType] 来替代它
     *
     * 例如下面这个参数结构 ↓
     *
     * ```java
     * Foo(String var1, boolean var2, com.demo.Test var3, int var4)
     * ```
     *
     * 此时就可以简单地写作 ↓
     *
     * ```kotlin
     * param(StringType, BooleanType, VagueType, IntType)
     * ```
     *
     * - 无参 [Constructor] 请使用 [emptyParam] 设置查找条件
     *
     * - 有参 [Constructor] 必须使用此方法设定参数或使用 [paramCount] 指定个数
     * @param paramType 参数类型数组 - 只能是 [Class]、[String]、[VariousClass]
     */
    @Deprecated(ReflectionMigration.KAVAREF_INFO)
    fun param(vararg paramType: Any) {
        if (paramType.isEmpty()) error("paramTypes is empty, please use emptyParam() instead")
        rulesData.paramTypes =
            mutableListOf<Class<*>>().apply { paramType.forEach { add(it.compat(tag = "Constructor") ?: UndefinedType) } }.toTypedArray()
    }

    /**
     * 设置 [Constructor] 参数条件
     *
     * 使用示例如下 ↓
     *
     * ```kotlin
     * param { it[1] == StringClass || it[2].name == "java.lang.String" }
     * ```
     *
     * - 无参 [Constructor] 请使用 [emptyParam] 设置查找条件
     *
     * - 有参 [Constructor] 必须使用此方法设定参数或使用 [paramCount] 指定个数
     * @param conditions 条件方法体
     */
    @Deprecated(ReflectionMigration.KAVAREF_INFO)
    fun param(conditions: ObjectsConditions) {
        rulesData.paramTypesConditions = conditions
    }

    /**
     * 设置 [Constructor] 参数个数范围
     *
     * 你可以不使用 [param] 指定参数类型而是仅使用此方法指定参数个数范围
     *
     * 使用示例如下 ↓
     *
     * ```kotlin
     * paramCount(1..5)
     * ```
     * @param numRange 个数范围
     */
    @Deprecated(ReflectionMigration.KAVAREF_INFO)
    fun paramCount(numRange: IntRange) {
        rulesData.paramCountRange = numRange
    }

    /**
     * 设置 [Constructor] 参数个数条件
     *
     * 你可以不使用 [param] 指定参数类型而是仅使用此方法指定参数个数条件
     *
     * 使用示例如下 ↓
     *
     * ```kotlin
     * paramCount { it >= 5 || it.isZero() }
     * ```
     * @param conditions 条件方法体
     */
    @Deprecated(ReflectionMigration.KAVAREF_INFO)
    fun paramCount(conditions: CountConditions) {
        rulesData.paramCountConditions = conditions
    }

    /**
     * 返回结果实现类
     * @return [MemberRulesResult]
     */
    internal fun build() = MemberRulesResult(rulesData)
}