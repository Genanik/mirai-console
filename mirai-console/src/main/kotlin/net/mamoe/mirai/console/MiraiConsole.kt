/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.console

import kotlinx.io.charsets.Charset
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.DefaultCommands
import net.mamoe.mirai.console.plugins.PluginManager
import net.mamoe.mirai.console.utils.MiraiConsoleUI
import net.mamoe.mirai.utils.SimpleLogger.LogPriority
import net.mamoe.mirai.utils.WeakRef
import java.io.ByteArrayOutputStream
import java.io.PrintStream


object MiraiConsole {
    /**
     * 发布的版本名
     */
    const val build = "Pkmon"
    lateinit var version: String
        internal set

    /**
     * 获取从Console登陆上的Bot, Bots
     * */
    @Suppress("DEPRECATION")
    @Deprecated("use Bot.instances from mirai-core", replaceWith = ReplaceWith("Bot.instances", "net.mamoe.mirai.Bot"))
    val bots: List<WeakRef<Bot>>
        get() = Bot.instances

    fun getBotOrNull(uin: Long): Bot? {
        return Bot.botInstances.firstOrNull { it.id == uin }
    }

    class BotNotFoundException(uin: Long) : Exception("Bot $uin Not Found")

    fun getBotOrThrow(uin: Long): Bot {
        return Bot.botInstances.firstOrNull { it.id == uin } ?: throw BotNotFoundException(uin)
    }

    /**
     * 与前端交互所使用的Logger
     */
    internal var logger = MiraiConsoleLogger

    /**
     * Console运行路径
     */
    lateinit var path: String
        internal set

    /**
     * Console前端接口
     */
    lateinit var frontEnd: MiraiConsoleUI
        internal set


    private var started = false


    @Deprecated("for binary compatibility", level = DeprecationLevel.HIDDEN)
    @Suppress("FunctionName")
    @JvmSynthetic
    @JvmStatic
    fun /* synthetic */`start$default`(
        miraiConsole: MiraiConsole,
        miraiConsoleUI: MiraiConsoleUI?,
        string: String?,
        string2: String?,
        n: Int,
        @Suppress("UNUSED_PARAMETER") `object`: Any?
    ) {
        @Suppress("NAME_SHADOWING")
        var string = string

        @Suppress("NAME_SHADOWING")
        var string2 = string2
        if (n and 2 != 0) {
            string = "0.0.0"
        }
        if (n and 4 != 0) {
            string2 = "0.0.0"
        }
        miraiConsole.start(miraiConsoleUI!!, string!!, string2!!)
    }

    /**
     * 启动Console
     */
    @JvmOverloads
    fun start(
        frontEnd: MiraiConsoleUI,
        coreVersion: String = "0.0.0",
        consoleVersion: String = "0.0.0",
        path:String = System.getProperty("user.dir")
    ) {
        if (started) {
            return
        }
        started = true
        this.path = path
        /* 初始化前端 */
        this.version = consoleVersion
        this.frontEnd = frontEnd
        this.frontEnd.pushVersion(consoleVersion, build, coreVersion)
        logger("Mirai-console now running under $path")
        logger("Get news in github: https://github.com/mamoe/mirai")
        logger("Mirai为开源项目，请自觉遵守开源项目协议")
        logger("Powered by Mamoe Technologies and contributors")

        /* 依次启用功能 */
        DefaultCommands()
        PluginManager.loadPlugins()
        CommandManager.start()

        /* 通知启动完成 */
        logger("Mirai-console 启动完成")
        logger("\"login qqnumber qqpassword \" to login a bot")
        logger("\"login qq号 qq密码 \" 来登录一个BOT")

        /* 尝试从系统配置自动登录 */
        DefaultCommands.tryLoginAuto()
    }

    /**
     * 关闭 Console
     */
    fun stop() {
        PluginManager.disablePlugins()
        CommandManager.cancel()
        try {
            Bot.botInstances.forEach {
                it.close()
            }
        } catch (ignored: Exception) {
        }
    }
}


internal object MiraiConsoleLogger {
    operator fun invoke(any: Any?) {
        invoke(
            "[Mirai ${MiraiConsole.version} ${MiraiConsole.build}]",
            0L,
            any
        )
    }

    operator fun invoke(e: Throwable?) {
        invoke(
            "[Mirai ${MiraiConsole.version} ${MiraiConsole.build}]",
            0L,
            e
        )
    }

    operator fun invoke(priority: LogPriority, identityStr: String, identity: Long, any: Any? = null) {
        if (any != null) {
            MiraiConsole.frontEnd.pushLog(priority, identityStr, identity, "$any")
        }
    }

    operator fun invoke(priority: LogPriority, identityStr: String, identity: Long, e: Throwable? = null) {
        if (e != null) {
            MiraiConsole.frontEnd.pushLog(priority, identityStr, identity, e.stacktraceString)
        }
    }

    // 设置默认的pushLog输出为 INFO 类型
    operator fun invoke(identityStr: String, identity: Long, any: Any? = null) {
        if (any != null) {
            MiraiConsole.frontEnd.pushLog(LogPriority.INFO, identityStr, identity, "$any")
        }
    }

    operator fun invoke(identityStr: String, identity: Long, e: Throwable? = null) {
        if (e != null) {
            MiraiConsole.frontEnd.pushLog(LogPriority.INFO, identityStr, identity, e.stacktraceString)
        }
    }
}

internal val Throwable.stacktraceString: String
    get() =
        ByteArrayOutputStream().apply {
            printStackTrace(PrintStream(this))
        }.use { it.toByteArray().encodeToString() }


@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.encodeToString(charset: Charset = Charsets.UTF_8): String =
    kotlinx.io.core.String(this, charset = charset)
