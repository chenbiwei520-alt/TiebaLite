package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.util.Log
import com.huanchengfly.tieba.post.utils.FileUtil.createFileInCacheDir
import com.huanchengfly.tieba.post.utils.FileUtil.ensureParents
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 运行日志记录器：把本进程的 logcat 收进内存环形缓冲，出问题时可以在
 * 「设置 → 更多 → 运行日志」里一键导出成文件。
 *
 * 为什么读 logcat 而不是自己包一层日志 API：项目里 150 多处日志都直接调
 * [android.util.Log]，逐个改成自己的封装既容易漏、也会把原来打屏的调用栈丢掉；
 * logcat 是唯一能把它们（连同 Compose 运行时报错、网络库、原生崩溃）一次收齐的地方。
 * 应用没有 READ_LOGS，但 Android 4.1 起 logd 允许进程读自己 UID 的日志，所以从这里
 * 起 [ProcessBuilder] 跑 `logcat` 只拿得到本应用自己的行。
 *
 * 记录本身是进程级的：离开「运行日志」页面它还在继续，复现问题前不用一直停在这一页。
 * */
object TiebaLog {

    /** 环形缓冲上限。一屏 logcat 大约 100 行，四千行够放几十次开关照片。 */
    private const val MaxLines = 4000

    /** 崩溃落盘时只留尾部，免得写文件太慢把系统卡死。 */
    private const val CrashTailLines = 1500

    private val lock = Any()

    private val buffer = ArrayDeque<String>()

    @Volatile
    private var reader: Process? = null

    @Volatile
    private var readerThread: Thread? = null

    @Volatile
    var isRecording: Boolean = false
        private set

    /** 幂等：已经在读就直接返回，重复调用不会起第二个 logcat。 */
    fun start() {
        if (readerThread?.isAlive == true) return
        isRecording = true
        val thread = Thread({
            var process: Process? = null
            try {
                process = ProcessBuilder("logcat", "-v", "threadtime")
                    .redirectErrorStream(true)
                    .start()
                reader = process
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line -> append(line) }
                }
                append("[${LOG_TAG}] logcat 已退出")
            } catch (t: Throwable) {
                append("[${LOG_TAG}] 无法读取 logcat：${t.message}")
            } finally {
                // 只有「当前这一条」读线程退出时才清标记：stop() 之后马上 start() 时，
                // 旧线程的收尾不能把新线程的记录状态改成暂停。
                if (readerThread === Thread.currentThread()) {
                    isRecording = false
                    reader = null
                }
                runCatching { process?.destroy() }
            }
        }, "TiebaLog-Reader")
        thread.isDaemon = true
        thread.priority = Thread.MIN_PRIORITY
        readerThread = thread
        thread.start()
    }

    fun stop() {
        val process = reader
        reader = null
        isRecording = false
        runCatching { process?.destroy() }
    }

    fun clear() {
        synchronized(lock) { buffer.clear() }
    }

    fun count(): Int = synchronized(lock) { buffer.size }

    /** 全量快照（导出用）。 */
    fun snapshot(): List<String> = synchronized(lock) { buffer.toList() }

    /** 最近 [max] 行（界面看尾巴用）。 */
    fun tail(max: Int): List<String> = synchronized(lock) {
        if (buffer.size <= max) buffer.toList() else buffer.drop(buffer.size - max)
    }

    /**
     * 自己的标记日志。
     *
     * 这些行无论 logcat 能不能读都会直接进缓冲（也会照常打屏），导出文件里因此一定会
     * 出现「打开看图 — 换了列表 — 对齐到第几页」这样的顺序，不依赖 logcat 是否被系统拦住。
     * */
    fun d(tag: String, message: String) = marker(Log.DEBUG, tag, message)

    fun i(tag: String, message: String) = marker(Log.INFO, tag, message)

    fun w(tag: String, message: String) = marker(Log.WARN, tag, message)

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        marker(Log.ERROR, tag, if (throwable == null) message else "$message: ${throwable.stackTraceToString()}")
    }

    private fun marker(priority: Int, tag: String, message: String) {
        runCatching { Log.println(priority, tag, message) }
        append("${stamp()} TRACE $tag: $message")
    }

    /**
     * 崩溃时同步落盘：缓冲在进程退出时会一起消失，先把尾巴写出来，
     * 下次进「运行日志」还能从缓存的崩溃文件里找到它。
     * */
    fun dumpCrashSync(context: Context, throwable: Throwable?): File? = runCatching {
        val file = context.createFileInCacheDir("crash_tiebalog_${stampFileName()}.txt")
        file.ensureParents()
        file.bufferedWriter().use { writer ->
            writer.appendLine("=== TiebaLog 崩溃现场 ${stamp()} ===")
            writer.appendLine(CrashLogUtil.getDebugInfo())
            throwable?.let { writer.appendLine(it.stackTraceToString()) }
            writer.appendLine("--- 最近 $CrashTailLines 行 ---")
            tail(CrashTailLines).forEach { writer.appendLine(it) }
        }
        file
    }.getOrNull()

    private fun append(line: String) {
        val entry = if (line.length > 500) line.take(500) + "…" else line
        synchronized(lock) {
            buffer.addLast(entry)
            while (buffer.size > MaxLines) {
                buffer.removeFirst()
            }
        }
    }

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val fileFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    private fun stamp(): String = synchronized(timeFormat) { timeFormat.format(Date()) }

    private fun stampFileName(): String = synchronized(fileFormat) { fileFormat.format(Date()) }

    private const val LOG_TAG = "TiebaLog"
}
