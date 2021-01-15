package com.miqt.asm.method_hook;


import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;


public class MyTransForm extends BaseTransForm {
    public MyTransForm(Project project) {
        super(project);
    }

    @Override
    byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        return classBytes;
    }

    @Override
    byte[] transformJar(byte[] classBytes, String jarName) {
        ClassReader reader = new ClassReader(classBytes);
        return classBytes;
    }
}
