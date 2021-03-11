package com.miqt.plugin.hookmethod;

import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.miqt.asm.method_hook.BasePlugin;

import org.apache.http.util.TextUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

public class HookMethodPlugin extends BasePlugin<HookMethodExtension> {
    @Override
    public HookMethodExtension initExtension() {
        return new HookMethodExtension();
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        super.transform(transformInvocation);
    }

    @Override
    public byte[] transform(byte[] classBytes, File classFile) {
        String name = classFile.getName();
        if (!TextUtils.isEmpty(getExtension().impl) &&
                classFile.getAbsolutePath().contains(getExtension().impl.replace(".", File.separator))) {
            return classBytes;
        }
        if (name.endsWith(".class") && !name.startsWith("R$") &&
                !"R.class".equals(name) && !"BuildConfig.class".equals(name)) {
            getLogger().log("[class]" + classFile.getName());
            return processClass(classBytes);
        }
        return classBytes;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public byte[] transformJar(byte[] classBytes, File jarFile, JarEntry entry) {
        //如果是impl类，直接跳过
        if (!TextUtils.isEmpty(getExtension().impl) &&
                entry.getName().contains(getExtension().impl.replace(".", "/"))) {
            return classBytes;
        }
        getLogger().log("[jar class]" + jarFile.getName() + ":" + entry.getName());
        //跳过自己的类库
        if(entry.getName().contains("com/miqt/pluginlib/")){
            //如果有impl，替换处理实现类
            if (!TextUtils.isEmpty(getExtension().impl)
                    && entry.getName().equals("com/miqt/pluginlib/tools/MethodHookHandler.class")) {
                try {
                    getLogger().log("[dump impl]" + jarFile.getName() + ":" + entry.getName());
                    return DumpClazz.dump(getExtension().impl);
                } catch (Exception e) {
                    getLogger().log(e);
                    return classBytes;
                }
            }
            return classBytes;
        }
        //注解+正则判断是否插桩
        return processClass(classBytes);
    }

    private byte[] processClass(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        MethodHookVisitor cv = new MethodHookVisitor(cw, this);
        cr.accept(cv, EXPAND_FRAMES);
        return cw.toByteArray();
    }

    @Override
    public String getName() {
        return "hook-method-plugin";
    }
}
