package com.miqt.asm.hook_method_plugin;

import com.miqt.asm.method_hook.BasePlugin;

import org.objectweb.asm.ClassReader;

public class MyPlugin extends BasePlugin<ConfigExtension> {
    @Override
    public ConfigExtension getExtension() {
        return new ConfigExtension();
    }

    @Override
    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        getLogger().log("\t" + reader.getClassName());
        return classBytes;
    }

    @Override
    public byte[] transformJar(byte[] classBytes, String jarName) {
        ClassReader reader = new ClassReader(classBytes);
        getLogger().log("\t" + reader.getClassName());
        return classBytes;
    }
}
