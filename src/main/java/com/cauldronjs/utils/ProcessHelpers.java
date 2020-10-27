package com.cauldronjs.utils;

import java.lang.reflect.Field;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

public class ProcessHelpers {
    private static long getPrivateLong(Object source, String fieldName) {
        Class<?> clazz = source.getClass();
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getLong(source);
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    public static long getPid(Process process) {
        Class<?> clazz = process.getClass();
        if (clazz.getName().equals("java.lang.UNIXProcess")) {
            return getPrivateLong(process, "pid");
        } else if (clazz.getName().equals("java.lang.ProcessImpl")) {
            long handleL = getPrivateLong(process, "handle");
            Kernel32 kernel = Kernel32.INSTANCE;
            WinNT.HANDLE handle = new WinNT.HANDLE();
            handle.setPointer(Pointer.createConstant(handleL));
            return kernel.GetProcessId(handle);
        } else {
            return -1;
        }
    }
}
