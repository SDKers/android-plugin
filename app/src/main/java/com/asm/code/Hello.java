package com.asm.code;

import android.util.Log;

import com.miqt.pluginlib.tools.IMethodHookHandler;

public class Hello implements IMethodHookHandler {
    public static void xxx(){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread.getAllStackTraces();
        Thread.getAllStackTraces();
        Thread.getAllStackTraces();
        Thread.getAllStackTraces();
        Thread.getAllStackTraces();
        Thread.getAllStackTraces();
        Thread.getAllStackTraces();
        Thread.getAllStackTraces();
        Thread.getAllStackTraces();
        Thread.getAllStackTraces();
    }

    @Override
    public void onMethodEnter(Object thisObj, String className, String methodName, String argsType, String returnType, Object... args) {
        Log.d("MethodHookHandler",methodName);

    }

    @Override
    public void onMethodReturn(Object returnObj, Object thisObj, String className, String methodName, String argsType, String returnType, Object... args) {
        Log.d("MethodHookHandler",methodName);
    }
}
