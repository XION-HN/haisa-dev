package com.haisa.dev

import android.app.Application
import android.util.Log
import com.haisa.sdk.HaisaEnvironment

/**
 * Haisa Dev 应用入口
 * 
 * 初始化 SDK 和应用级配置
 */
class HaisaApp : Application() {

    companion object {
        const val TAG = "HaisaApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Haisa Dev 启动中...")

        // 初始化 Haisa SDK
        val haisa = HaisaEnvironment.getInstance(this)
        
        // 预加载核心基础模块 (env-base)
        // 注意: env-base 是基础运行库，其他所有模块都依赖它
        Thread {
            try {
                if (!haisa.isEnvironmentReady("env-base")) {
                    Log.w(TAG, "env-base 未就绪，部分功能可能受限")
                } else {
                    Log.d(TAG, "核心环境已就绪")
                }
            } catch (e: Exception) {
                Log.e(TAG, "环境检测失败", e)
            }
        }.start()
    }
}

