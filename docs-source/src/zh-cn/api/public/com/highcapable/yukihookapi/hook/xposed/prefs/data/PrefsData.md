---
pageClass: code-page
---

::: warning

由于维护成本，`YukiHookAPI` 从 `1.3.0` 版本开始将不再会对此文档进行更新且在 `2.0.0` 版本切换为 Dokka 插件自动生成的 API 文档。

:::

# PrefsData <span class="symbol">- class</span>

```kotlin:no-line-numbers
data class PrefsData<T>(var key: String, var value: T) : Serializable
```

**变更记录**

`v1.0.67` `新增`

`v1.1.5` `修改`

实现了 `Serializable` 接口

**功能描述**

> 键值对存储构造类。

这个类是对 `YukiHookPrefsBridge` 的一个扩展用法。

**功能示例**

建立一个模板类定义模块与宿主需要使用的键值数据。

> 示例如下

```kotlin
object DataConst {

    val TEST_KV_DATA_1 = PrefsData("test_data_1", "defalut value")
    val TEST_KV_DATA_2 = PrefsData("test_data_2", false)
    val TEST_KV_DATA_3 = PrefsData("test_data_3", 0)
}
```

键值数据定义后，你就可以方便地在模块和宿主中调用所需要的数据。

> 模块示例如下

```kotlin
// 读取
val data = prefs().get(DataConst.TEST_KV_DATA_1)
// 写入
prefs().edit { put(DataConst.TEST_KV_DATA_1, "written value") }
```

> 宿主示例如下

```kotlin
// 读取 String
val dataString = prefs.get(DataConst.TEST_KV_DATA_1)
// 读取 Boolean
val dataBoolean = prefs.get(DataConst.TEST_KV_DATA_2)
```

你依然可以不使用模板定义的默认值，随时修改你的默认值。

> 示例如下

```kotlin
// 读取 - 此时 data 取到的默认值将会是 2 - 并不是模板提供的 0
val data = prefs.get(DataConst.TEST_KV_DATA_3, 2)
```