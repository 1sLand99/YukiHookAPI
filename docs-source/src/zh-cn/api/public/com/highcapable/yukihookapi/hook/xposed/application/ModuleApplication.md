---
pageClass: code-page
---

::: warning

由于维护成本，`YukiHookAPI` 从 `1.3.0` 版本开始将不再会对此文档进行更新且在 `2.0.0` 版本切换为 Dokka 插件自动生成的 API 文档。

:::

# ModuleApplication <span class="symbol">- class</span>

```kotlin:no-line-numbers
open class ModuleApplication: Application()
```

**变更记录**

`v1.0.77` `新增`

**功能描述**

> 这是对使用 `YukiHookAPI` Xposed 模块实现中的一个扩展功能。

在你的 Xposed 模块的 `Application` 中继承此类。

或在 `AndroidManifest.xml` 的 `application` 标签中指定此类。

目前可实现功能如下

- 全局共享模块中静态的 `appContext`

- 在模块与宿主中装载 `YukiHookAPI.Config` 以确保 `YukiHookAPI.Configs.debugTag` 不需要重复定义

- 在模块与宿主中使用 `YukiHookDataChannel` 进行通讯

- 在模块中使用系统隐藏 API，核心技术引用了开源项目 [FreeReflection](https://github.com/tiann/FreeReflection)

- 在模块中使用 `YukiHookAPI.Status.isTaiChiModuleActive` 判断太极、无极激活状态

**功能示例**

将此类继承到你的自定义 `Application` 上。

> 示例如下

```kotlin
package com.demo

class MyApplication: ModuleApplication() {

    override fun onCreate() {
        super.onCreate()
    }
}
```

在 `AndroidManifest.xml` 的 `application` 标签中指定自定义的 `Application`。

> 示例如下

```xml
<application
    android:name="com.demo.MyApplication"
    ...>
```

如果你不需要自定义 `Application` 可以直接将 `ModuleApplication` 设置到 `AndroidManifest.xml` 的 `application` 标签中。

> 示例如下

```xml
<application
    android:name="com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication"
    ...>
```

## appContext <span class="symbol">- field</span>

```kotlin:no-line-numbers
val appContext: ModuleApplication
```

**变更记录**

`v1.0.77` `新增`

**功能描述**

> 获取全局静态 `Application` 实例。