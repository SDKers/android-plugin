package com.miqt.asm.hook_method_plugin;

import com.miqt.asm.method_hook.BasePlugin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Consumer;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

public class MyPlugin extends BasePlugin<ConfigExtension> {
    @Override
    public ConfigExtension initExtension() {
        return new ConfigExtension();
    }

    @Override
    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        getLogger().log(reader.getClassName());
        ClassNode node = new ClassNode();
        reader.accept(node, EXPAND_FRAMES);
        node.methods.forEach(new Consumer<MethodNode>() {
            @Override
            public void accept(MethodNode methodNode) {
                getLogger().log("\t[MethodNode]" + methodNode.name);
            }
        });
        node.fields.forEach(new Consumer<FieldNode>() {
            @Override
            public void accept(FieldNode fieldNode) {
                getLogger().log("\t[FieldNode]" + fieldNode.name);
            }
        });
        return classBytes;
    }

    @Override
    public byte[] transformJar(byte[] classBytes, String jarName) {
        ClassReader reader = new ClassReader(classBytes);
        getLogger().log(reader.getClassName());
        ClassNode node = new ClassNode();
        reader.accept(node, EXPAND_FRAMES);
        node.methods.forEach(new Consumer<MethodNode>() {
            @Override
            public void accept(MethodNode methodNode) {
                getLogger().log("\t[MethodNode]" + methodNode.name);
            }
        });
        node.fields.forEach(new Consumer<FieldNode>() {
            @Override
            public void accept(FieldNode fieldNode) {
                getLogger().log("\t[FieldNode]" + fieldNode.name);
            }
        });
        return classBytes;
    }

    @Override
    public String getName() {
        return "demo-plugin";
    }
}
