package com.rosan.ruto

import android.app.Application
import com.rosan.ruto.di.init.appModules
import com.rosan.ruto.service.KeepAliveService
import com.rosan.ruto.util.CrashLogger
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import rikka.shizuku.Shizuku
import rikka.sui.Sui

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 最优先安装全局崩溃日志捕获
        CrashLogger.install(this)

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModules)
        }
        Sui.init(packageName)

        // Start KeepAliveService
        KeepAliveService.start(this)
    }
}
