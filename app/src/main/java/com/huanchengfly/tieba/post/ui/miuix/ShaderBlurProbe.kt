package com.huanchengfly.tieba.post.ui.miuix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.huanchengfly.tieba.post.utils.TiebaLog

/**
 * RuntimeShader（AGSL）真实渲染能力探针。
 *
 * `isRuntimeShaderSupported()` 只检查 API 级别（Android 13+）。部分 Adreno 的 Vulkan 驱动在
 * Skia 创建着色器管线时无法编译 AGSL 程序，绘制会被驱动静默跳过（logcat：
 * `AdrenoVK-0: Shader compilation failed for shaderType: 4` + `Pipeline create failed`），
 * 应用层捕获不到任何异常——表现为渐进模糊顶栏在页面重新进入合成（例如从帖子页返回话题页）
 * 后凭空消失，而界面其余部分正常。
 *
 * 探针在离屏 GPU 缓冲上渲染 **miuix-blur 自己的两个着色器程序**（逐字取自 v0.9.4 的
 * `internal/Shaders.kt`：24-tap 渐进高斯核 + 4-child 多级合成器——v1 探针只用了两次采样的
 * 简化程序，在坏驱动上编译通过，拦不住真实故障），读回像素验证着色器真的画上了：
 *
 *  - 两个都画上 → 本设备 AGSL 可用，维持模糊效果；
 *  - 任一只有底色、着色器被跳过 → 判定不可用，持久化，之后每次启动从第一帧起就走平面样式；
 *  - 底色都没画上 / 探针自身异常 → 结论不明，保持现状，绝不因探针失败而误降级。
 *
 * 判定持久化在 SharedPreferences（设备能力，非用户设置），键名带版本号：金丝雀程序升级时
 * 必须换键（`KEY_RENDERABLE` 的版本后缀），否则旧判定会永久跳过重新探测。
 */
object ShaderBlurProbe {

    private const val TAG = "ShaderBlurProbe"
    private const val PREF_NAME = "shader_blur_probe"
    private const val KEY_RENDERABLE = "agsl_renderable_v2" // 缺失 = 尚未探测

    private val _renderable = mutableStateOf(true)

    /** AGSL 是否真实可渲染。未知时按可用处理；探测失败会翻转此状态触发重组降级。 */
    @Stable
    val renderable: Boolean get() = _renderable.value

    /**
     * 判定的人类可读结论（日志页状态栏、导出文件头展示用）。
     * warmUp 后即反映持久化/探测结论，不随日志缓冲的清空丢失。
     */
    @Volatile
    var verdictLabel: String = "unknown (not probed yet)"
        private set

    @Volatile
    private var warmedUp = false

    /**
     * 同步读取持久化判定，并按需启动一次后台探测。幂等：首次调用完成读盘，之后是空操作。
     * 在 [com.huanchengfly.tieba.post.App.onCreate] 和 [rememberMiuixBarBackdrop] 各挂一道，
     * 后者保证即便预热被跳过也不会漏。
     */
    fun warmUp(context: Context) {
        if (warmedUp) return
        warmedUp = true
        if (Build.VERSION.SDK_INT < 33) {
            verdictLabel = "n/a (API < 33)"
            return // RuntimeShader 不存在；由调用方的 API 门控兜底
        }

        val appContext = context.applicationContext
        when (appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_RENDERABLE, null)) {
            "yes" -> {
                _renderable.value = true
                verdictLabel = "supported"
                TiebaLog.i(TAG, "cached verdict: supported")
            }

            "no" -> {
                _renderable.value = false
                verdictLabel = "unsupported (flat fallback bars)"
                TiebaLog.i(TAG, "cached verdict: unsupported, using flat bars")
            }

            null -> {
                verdictLabel = "probing..."
                TiebaLog.i(TAG, "no cached verdict, probing on background thread")
                Thread({ probe(appContext) }, TAG).start()
            }
        }
    }

    @RequiresApi(33)
    private fun probe(appContext: Context) {
        val verdict = runCatching { runProbe() }
            .onFailure { TiebaLog.e(TAG, "probe crashed, treated as inconclusive", it) }
            .getOrNull() ?: run {
            verdictLabel = "inconclusive (treated as supported)"
            return
        }
        TiebaLog.i(TAG, "probe verdict: $verdict")
        verdictLabel = when (verdict) {
            Verdict.SUPPORTED -> "supported"
            Verdict.UNSUPPORTED -> "unsupported (flat fallback bars)"
            Verdict.INCONCLUSIVE -> "inconclusive (treated as supported)"
        }
        persist(appContext, verdict == Verdict.SUPPORTED)
    }

    private fun persist(appContext: Context, renderable: Boolean) {
        _renderable.value = renderable
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RENDERABLE, if (renderable) "yes" else "no")
            .apply()
    }

    /** 两个金丝雀必须都画上才判可用；任一被跳过即判不可用。 */
    @RequiresApi(33)
    private fun runProbe(): Verdict {
        val kernelVerdict = runCanary(PROGRESSIVE_KERNEL_AGS) { shader, step, maxCoord ->
            shader.setFloatUniform("in_maxCoord", maxCoord, maxCoord)
            shader.setFloatUniform("in_step", step, 0f)
            shader.setFloatUniform("in_maxRadius", 60f)
            shader.setFloatUniform("in_gradAxis", 0f, 1f)
            shader.setFloatUniform("in_gradBand", 0f, maxCoord)
            shader.setFloatUniform("in_curve", 1f)
            shader.setFloatUniform("in_noise", 0f)
            shader.setInputShader("child", solidGreenShader())
        }
        if (kernelVerdict != Verdict.SUPPORTED) return kernelVerdict

        return runCanary(PROGRESSIVE_COMPOSITE_AGS) { shader, _, maxCoord ->
            shader.setFloatUniform("in_gradAxis", 0f, 1f)
            shader.setFloatUniform("in_gradBand", 0f, maxCoord)
            shader.setFloatUniform("in_curve", 1f)
            shader.setInputShader("clearChild", solidGreenShader())
            shader.setInputShader("blur0", solidGreenShader())
            shader.setInputShader("blur1", solidGreenShader())
            shader.setInputShader("blur2", solidGreenShader())
        }
    }

    /**
     * 渲染一个金丝雀：品红底色上用目标 AGSL 程序盖满整幅，读回中心像素。
     * 着色器输出恒为纯绿（所有 child 都是纯绿子图），与品红底色在色彩空间换算下也不可能混淆。
     */
    @RequiresApi(33)
    private fun runCanary(agsl: String, setupUniforms: (RuntimeShader, Float, Float) -> Unit): Verdict {
        val width = 64
        val height = 64

        // GPU 写入 + CPU 读回：libhwui 的 Vulkan/GL 管线负责绘制，与 View 渲染走同一条
        // 着色器编译路径，所以这里的编译失败就是真机模糊消失的同一个失败。
        val reader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            1,
            HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or
                    HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or
                    HardwareBuffer.USAGE_CPU_READ_OFTEN,
        )
        try {
            val canvas = reader.surface.lockHardwareCanvas()
            try {
                // 底色：无着色器的纯色填充，正常管线一定会画上——它是"绘制路径本身是否工作"的对照
                canvas.drawColor(Color.MAGENTA)

                // 与渐进模糊同构的绘制结构：AGSL 画进 RenderNode，模糊 RenderEffect 挂在节点上
                // （等价于 graphicsLayer.renderEffect）。画布与子图都是纯色，模糊不改变像素，
                // 但模糊链会参与管线编译。
                val node = RenderNode("ShaderBlurProbe")
                node.setPosition(0, 0, width, height)
                node.setRenderEffect(RenderEffect.createBlurEffect(3f, 3f, Shader.TileMode.CLAMP))
                val nodeCanvas = node.beginRecording()
                try {
                    val shader = RuntimeShader(agsl)
                    setupUniforms(shader, 1f, width - 0.5f)
                    nodeCanvas.drawPaint(Paint().apply { this.shader = shader })
                } finally {
                    node.endRecording()
                }
                canvas.drawRenderNode(node)
            } finally {
                reader.surface.unlockCanvasAndPost(canvas)
            }

            val image = acquireImage(reader) ?: return Verdict.INCONCLUSIVE
            val packed = image.use { frame ->
                val plane = frame.planes[0]
                plane.buffer.let { buffer ->
                    buffer.rewind()
                    // RGBA_8888 小端打包为 ABGR；行距可能带填充，用 rowStride 取中心像素
                    buffer.asIntBuffer().get((height / 2) * (plane.rowStride / 4) + width / 2)
                }
            }

            val r = packed and 0xFF
            val g = packed shr 8 and 0xFF
            val b = packed shr 16 and 0xFF
            val a = packed shr 24 and 0xFF
            return when {
                // 连底色都没画上：绘制路径层面异常，与 AGSL 编译无关，不下结论
                a < 0x20 && r + g + b == 0 -> Verdict.INCONCLUSIVE

                // 着色器输出是纯绿，无论线性/sRGB 色彩空间都不可能接近品红底色；
                // 反之着色器被驱动跳过时这里必然还是品红
                g > 200 && r < 200 -> Verdict.SUPPORTED

                else -> Verdict.UNSUPPORTED
            }
        } finally {
            reader.close()
        }
    }

    private fun solidGreenShader(): BitmapShader =
        BitmapShader(
            Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) },
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP,
        )

    /**
     * 等待 GPU 完成写入并取回缓冲。acquireLatestImage 在 post 后通常立即可用，
     * 个别驱动需要一小段时间；最多等 2.5s，超时按结论不明处理。
     */
    private fun acquireImage(reader: ImageReader) = try {
        var image: android.media.Image? = null
        repeat(25) {
            if (image == null) image = reader.acquireLatestImage()
            if (image == null) Thread.sleep(100)
        }
        image
    } catch (_: InterruptedException) {
        null
    }

    private enum class Verdict { SUPPORTED, UNSUPPORTED, INCONCLUSIVE }

    /*
     * 以下两段 AGSL 逐字取自 miuix-blur v0.9.4（commit 39c40f99）的
     * miuix-blur/src/commonMain/kotlin/top/yukonga/miuix/kmp/blur/internal/Shaders.kt
     * （Apache-2.0）：`buildProgressiveBlurShader(maxTap = 24)` 与
     * `PROGRESSIVE_COMPOSITE_SHADER`。它们就是渐进模糊栏真实编译的程序——探针的使命是
     * 在同一块驱动上编译同一段代码，因此不做任何简化。
     */

    /** 渐进模糊主核：逐像素半径的一维高斯，24-tap 循环（12 对合并采样）+ 早退分支。 */
    private const val PROGRESSIVE_KERNEL_AGS =
        """
        uniform shader child;
        uniform float2 in_maxCoord;
        uniform float2 in_step;
        uniform float in_maxRadius;
        uniform float2 in_gradAxis;
        uniform float2 in_gradBand;
        uniform float in_curve;
        uniform float in_noise;

        float progRand(float2 co) {
            return fract(sin(dot(co, float2(12.9898, 78.233))) * 43758.5453);
        }

        half4 main(float2 xy) {
            float p = dot(xy, in_gradAxis);
            float mw = 1.0;
            float raw = clamp((p - in_gradBand.x) / (in_gradBand.y - in_gradBand.x), 0.0, 1.0);
            float intensity = 1.0 - pow(raw * raw * (3.0 - 2.0 * raw), in_curve);
            float radius = in_maxRadius * intensity;
            if (radius <= 0.0) {
                half4 s = child.eval(clamp(xy, float2(0.5), in_maxCoord));
                if (s.a > 0.0039) {
                    return half4(s.rgb / s.a, 1.0) * half(mw);
                }
                return s * half(mw);
            }
            float inv2s2 = -1.0 / (2.0 * (radius * 0.5 + 0.5) * (radius * 0.5 + 0.5));
            float2 jitter = ((progRand(xy) - 0.5) * in_noise) * in_step;
            half4 color = child.eval(clamp(xy + jitter, float2(0.5), in_maxCoord));
            float total = 1.0;
            for (int j = 0; j < 12; j++) {
                float a = float(2 * j + 1);
                float ra = clamp(radius - a + 1.0, 0.0, 1.0);
                if (ra <= 0.0) continue;
                float b = a + 1.0;
                float wa = exp(a * a * inv2s2) * ra;
                float wb = exp(b * b * inv2s2) * clamp(radius - b + 1.0, 0.0, 1.0);
                float wsum = wa + wb;
                float2 o = ((a * wa + b * wb) / wsum) * in_step;
                color += (child.eval(clamp(xy + o + jitter, float2(0.5), in_maxCoord)) +
                    child.eval(clamp(xy - o + jitter, float2(0.5), in_maxCoord))) * half(wsum);
                total += 2.0 * wsum;
            }
            color = color / half(max(total, 0.0001));
            if (color.a > 0.0039) {
                return half4(color.rgb / color.a, 1.0) * half(mw);
            }
            return color * half(mw);
        }
        """

    /** 渐进模糊合成器：多级模糊栈 + 锐利末端沿渐变带线性混合（4 个 child 采样 + 分支）。 */
    private const val PROGRESSIVE_COMPOSITE_AGS =
        """
        uniform shader clearChild;
        uniform shader blur0;
        uniform shader blur1;
        uniform shader blur2;
        uniform float2 in_gradAxis;
        uniform float2 in_gradBand;
        uniform float in_curve;

        half4 main(float2 xy) {
            float p = dot(xy, in_gradAxis);
            float raw = clamp((p - in_gradBand.x) / (in_gradBand.y - in_gradBand.x), 0.0, 1.0);
            raw = pow(raw * raw * (3.0 - 2.0 * raw), in_curve);
            float pos = raw * 3.0;
            half4 color;
            if (pos < 1.0) {
                float f = pos * pos * (3.0 - 2.0 * pos);
                color = mix(blur0.eval(xy), blur1.eval(xy), f);
            } else if (pos < 2.0) {
                float f = pos - 1.0;
                f = f * f * (3.0 - 2.0 * f);
                color = mix(blur1.eval(xy), blur2.eval(xy), f);
            } else {
                float f = pos - 2.0;
                f = f * f * (3.0 - 2.0 * f);
                color = mix(blur2.eval(xy), clearChild.eval(xy), f);
            }
            if (color.a > 0.0039) {
                return half4(color.rgb / color.a, 1.0);
            }
            return color;
        }
        """
}
