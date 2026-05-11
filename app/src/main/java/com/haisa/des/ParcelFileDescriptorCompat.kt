package com.haisa.des

import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile

object ParcelFileDescriptorCompat {

    fun openPtmx(): ParcelFileDescriptor? {
        var raf: RandomAccessFile? = null
        return try {
            raf = RandomAccessFile("/dev/ptmx", "rw")
            val fd = raf.fd
            val intFd = getFileDescriptorInt(fd)
            if (intFd < 0) {
                raf.close()
                return null
            }
            val pfd = ParcelFileDescriptor.adoptFd(intFd)
            val ptsName = getPtsName(fd)
            grantAccess(ptsName)
            raf = null
            pfd
        } catch (e: Exception) {
            null
        } finally {
            try { raf?.close() } catch (_: Exception) {}
        }
    }

    private fun getFileDescriptorInt(fd: FileDescriptor): Int {
        return try {
            val descriptorField = FileDescriptor::class.java.getDeclaredField("descriptor")
            descriptorField.isAccessible = true
            descriptorField.getInt(fd)
        } catch (e: Exception) {
            -1
        }
    }

    fun getInputStream(pfd: ParcelFileDescriptor): InputStream {
        return FileInputStream(pfd.fileDescriptor)
    }

    fun getOutputStream(pfd: ParcelFileDescriptor): OutputStream {
        return FileOutputStream(pfd.fileDescriptor)
    }

    private fun getPtsName(fd: FileDescriptor): String {
        return try {
            val clazz = Class.forName("android.system.Os")
            val method = clazz.getMethod("ptsname", FileDescriptor::class.java)
            method.invoke(null, fd) as String
        } catch (e: Exception) {
            try {
                val clazz = Class.forName("libcore.io.Libcore")
                val osField = clazz.getDeclaredField("os")
                val os = osField.get(null)
                val method = os.javaClass.getMethod("ptsname", FileDescriptor::class.java)
                method.invoke(os, fd) as String
            } catch (e2: Exception) {
                "/dev/pts/0"
            }
        }
    }

    private fun grantAccess(ptsName: String) {
        try {
            val clazz = Class.forName("android.system.Os")
            val method = clazz.getMethod("chmod", String::class.java, Int::class.javaPrimitiveType)
            method.invoke(null, ptsName, 0x1A4)
        } catch (e: Exception) {
            try {
                Runtime.getRuntime().exec(arrayOf("chmod", "0644", ptsName)).waitFor()
            } catch (e2: Exception) {
                // best effort
            }
        }
    }
}
