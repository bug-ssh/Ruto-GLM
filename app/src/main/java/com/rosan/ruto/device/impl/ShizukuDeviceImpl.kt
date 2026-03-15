package com.rosan.ruto.device.impl

import android.content.Context
import com.rosan.installer.ext.service.ShizukuServiceManager
import com.rosan.ruto.device.repo.DeviceRepo
import com.rosan.ruto.service.ActivityManagerService
import com.rosan.ruto.service.DisplayManagerService
import com.rosan.ruto.service.IActivityManager
import com.rosan.ruto.service.IDisplayManager
import com.rosan.ruto.service.IImeManager
import com.rosan.ruto.service.IInputManager
import com.rosan.ruto.service.IPackageManager
import com.rosan.ruto.service.ImeManagerService
import com.rosan.ruto.service.InputManagerService
import com.rosan.ruto.service.PackageManagerService
import kotlinx.coroutines.runBlocking

class ShizukuDeviceImpl(context: Context) : DeviceRepo {
    private val shizuku = ShizukuServiceManager(context)

    override val packageManager: IPackageManager by lazy {
        runBlocking {
            shizuku.serviceBinder(PackageManagerService::class.java) {
                IPackageManager.Stub.asInterface(it)
            }
        }
    }

    override val activityManager: IActivityManager by lazy {
        runBlocking {
            shizuku.serviceBinder(ActivityManagerService::class.java) {
                IActivityManager.Stub.asInterface(it)
            }
        }
    }

    override val displayManager: IDisplayManager by lazy {
        runBlocking {
            shizuku.serviceBinder(DisplayManagerService::class.java) {
                IDisplayManager.Stub.asInterface(it)
            }
        }
    }

    override val inputManager: IInputManager by lazy {
        InputManagerService(shizuku)
    }

    override val imeManager: IImeManager by lazy {
        ImeManagerService(context, shizuku)
    }

    override fun close() {
        shizuku.close()
    }
}
