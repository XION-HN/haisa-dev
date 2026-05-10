package com.haisa.dev

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
        return try {
            val ptmx = RandomAccessFile("/dev/ptmx", "rw")
            val fd = ptmx.fd
            val pfd = ParcelFileDescriptor.adoptFd(fd.toInt())
            val ptsName = getPtsName(fd)
            grantAccess(ptsName)
            pfd
        } catch (e: Exception) {
            null
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
