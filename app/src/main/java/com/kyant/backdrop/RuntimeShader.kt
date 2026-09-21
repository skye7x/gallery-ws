package com.kyant.backdrop

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import org.intellij.lang.annotations.Language

sealed interface RuntimeShader {
    fun setFloatUniform(name: String, value: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float, value4: Float)
    fun setFloatUniform(name: String, values: FloatArray)
    fun setIntUniform(name: String, value: Int)
    fun setIntUniform(name: String, value1: Int, value2: Int)
    fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int)
    fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int)
    fun setIntUniform(name: String, values: IntArray)
    fun setColorUniform(name: String, color: Color)
}

fun RuntimeShader(@Language("AGSL") shaderString: String): RuntimeShader {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
            val shader = android.graphics.RuntimeShader(shaderString)
            AndroidRuntimeShader(shader)
        } catch (_: Throwable) {
            NoOpRuntimeShader
        }
    } else {
        NoOpRuntimeShader
    }
}

fun RuntimeShader.asComposeShader(): Shader? {
    return (this as? AndroidRuntimeShader)?.shader
}

fun RuntimeShader.asAndroidRuntimeShader(): android.graphics.RuntimeShader? {
    return (this as? AndroidRuntimeShader)?.shader
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class AndroidRuntimeShader(val shader: android.graphics.RuntimeShader) : RuntimeShader {
    override fun setFloatUniform(name: String, value: Float) {
        shader.setFloatUniform(name, value)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float) {
        shader.setFloatUniform(name, value1, value2)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) {
        shader.setFloatUniform(name, value1, value2, value3)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float, value4: Float) {
        shader.setFloatUniform(name, value1, value2, value3, value4)
    }

    override fun setFloatUniform(name: String, values: FloatArray) {
        shader.setFloatUniform(name, values)
    }

    override fun setIntUniform(name: String, value: Int) {
        shader.setIntUniform(name, value)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int) {
        shader.setIntUniform(name, value1, value2)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int) {
        shader.setIntUniform(name, value1, value2, value3)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int) {
        shader.setIntUniform(name, value1, value2, value3, value4)
    }

    override fun setIntUniform(name: String, values: IntArray) {
        shader.setIntUniform(name, values)
    }

    override fun setColorUniform(name: String, color: Color) {
        shader.setColorUniform(name, color.toArgb())
    }
}

private object NoOpRuntimeShader : RuntimeShader {
    override fun setFloatUniform(name: String, value: Float) {}
    override fun setFloatUniform(name: String, value1: Float, value2: Float) {}
    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) {}
    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float, value4: Float) {}
    override fun setFloatUniform(name: String, values: FloatArray) {}
    override fun setIntUniform(name: String, value: Int) {}
    override fun setIntUniform(name: String, value1: Int, value2: Int) {}
    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int) {}
    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int) {}
    override fun setIntUniform(name: String, values: IntArray) {}
    override fun setColorUniform(name: String, color: Color) {}
}

sealed interface RuntimeShaderCache {
    fun obtainRuntimeShader(key: String, @Language("AGSL") string: String): RuntimeShader
}

internal class RuntimeShaderCacheImpl : RuntimeShaderCache {
    private val runtimeShaders = mutableMapOf<String, RuntimeShader>()

    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
        return runtimeShaders.getOrPut(key) { RuntimeShader(string) }
    }

    fun clear() {
        runtimeShaders.clear()
    }
}
