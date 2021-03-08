package com.miqt.asm.method_hook;

import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {
    String dir;
    String name;
    BufferedWriter writer;

    public Logger(String dir, String name) {
        this.dir = dir;
        this.name = name;
    }

    void init() {
        if (writer != null){
            return;
        }
        try {
            File file = new File(dir);
            if (!file.exists() || !file.isDirectory()) {
                file.mkdirs();
            }
            File logFile = new File(dir, name);
            // 删除旧的日志文件
            try {
                if (file.exists()) {
                    FileUtils.forceDelete(logFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            FileOutputStream stream = new FileOutputStream(logFile, false);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
            writer = new BufferedWriter(outputStreamWriter);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    void release() {
        if (writer == null) {
            return;
        }
        try {
            writer.flush();
            writer.close();
            writer = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String message) {
        try {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void log(Throwable throwable) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            writer.write(sw.toString());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void print(String message) {
        System.out.println(message);
    }
}
