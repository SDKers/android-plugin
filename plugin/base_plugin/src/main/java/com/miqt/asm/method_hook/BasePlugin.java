package com.miqt.asm.method_hook;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.google.common.collect.Sets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static com.android.build.api.transform.Status.CHANGED;


public abstract class BasePlugin<E extends Extension> extends Transform implements Plugin<Project> {

    private Project project;
    private boolean isApp = true;

    private Logger logger;
    private E extension;

    @Override
    public void apply(@NotNull Project project) {
        this.project = project;
        logger = new Logger(project.getBuildDir().getAbsolutePath() + "/plugin/", getName() + ".log");
        BaseExtension android = (BaseExtension) project.getExtensions().getByName("android");
        if (android instanceof AppExtension) {
            isApp = true;
        } else if (android instanceof LibraryExtension) {
            isApp = false;
        }

        E e = initExtension();

        project.getExtensions().create(e.getExtensionName(), e.getClass());
        extension = (E) project.getExtensions().getByType(e.getClass());
        android.registerTransform(this);
    }

    public abstract E initExtension();

    public E getExtension() {
        return extension;
    }

    public Project getProject() {
        return project;
    }

    public boolean isApp() {
        return isApp;
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        if (!isApp) {
            return Sets.immutableEnumSet(
                    QualifiedContent.Scope.PROJECT);
        } else {
            return TransformManager.SCOPE_FULL_PROJECT;
        }
    }

    @Override
    public boolean isIncremental() {
        return true;
    }


    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        logger.init();
        try {
            beginTransform(transformInvocation);
        } catch (Throwable e) {
            e.printStackTrace();
            logger.log(e);
        }
        try {
            doTransform(transformInvocation);
        } catch (Throwable e) {
            e.printStackTrace();
            logger.log(e);
        }
        try {
            afterTransform(transformInvocation);
        } catch (Throwable e) {
            e.printStackTrace();
            logger.log(e);
        }
        logger.release();
    }

    protected void afterTransform(TransformInvocation transformInvocation) {
    }


    protected void beginTransform(TransformInvocation transformInvocation) {
    }


    private void doTransform(TransformInvocation transformInvocation) {
        if (!getExtension().enable) {
            logger.log(getName() + " not enable!");
            return;
        }
        if (getExtension().justDebug) {
            String vn = transformInvocation.getContext().getVariantName();
            if (!"debug".equals(vn)) {
                logger.log("Current build is " + vn + " type,[justDebug] not work in this.");
                return;
            }
        }
        try {
            super.transform(transformInvocation);
            boolean isIncremental = transformInvocation.isIncremental();
            logger.log("----------------------------------------------------------------");
            logger.log("ProjectName: " + transformInvocation.getContext().getProjectName());
            logger.log("ProjectPath: " + transformInvocation.getContext().getPath());
            logger.log("BuildType  : " + transformInvocation.getContext().getVariantName());
            logger.log("Incremental: " + isIncremental);
            logger.log("extension  : " + extension.toString());
            logger.log("Time       : " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
            logger.log("----------------------------------------------------------------");

            //如果非增量，则清空旧的输出内容
            if (!isIncremental) {
                transformInvocation.getOutputProvider().deleteAll();
            }
            Collection<TransformInput> inputs = transformInvocation.getInputs();
            inputs.forEach(transformInput -> {
                transformInput.getDirectoryInputs().forEach(directoryInput -> {
                    eachDir(transformInvocation, isIncremental, directoryInput);
                });
                transformInput.getJarInputs().forEach(jarInput -> {
                    eachJar(transformInvocation, isIncremental, jarInput);
                });
            });
        } catch (Throwable e) {
            e.printStackTrace();
            logger.log(e);
        }
    }

    private void eachJar(TransformInvocation transformInvocation, boolean isIncremental, JarInput jarInput) {
        try {
            String jarName = jarInput.getName();
            File file = jarInput.getFile();
//            File temDir = transformInvocation.getContext().getTemporaryDir();
            File dest = transformInvocation.getOutputProvider().getContentLocation(
                    jarInput.getFile().getAbsolutePath(),
                    jarInput.getContentTypes(),
                    jarInput.getScopes(),
                    Format.JAR);
            Status status;
            if (isIncremental) {
                status = jarInput.getStatus();
            } else {
                status = Status.CHANGED;
            }
            logger.log("[JarInput]" + file.getAbsolutePath() + " status:" + status);
            //根据是否变化决定是否更新
            switch (status) {
                case NOTCHANGED:
                    break;
                case REMOVED:
                    if (dest.exists()) {
                        FileUtils.forceDelete(dest);
                    }
                    break;
                case ADDED:
                case CHANGED:
                    try {
                        FileUtils.touch(dest);
                    } catch (Throwable e) {
                        File pr = dest.getParentFile();
                        if (!pr.exists()) {
                            pr.mkdirs();
                        }
                    }
                    //不遍历jar，则直接退出
                    if (!getExtension().injectJar) {
                        FileUtils.copyFile(file, dest);
                        break;
                    }
                    weaveSingleJarToFile(file, dest);
                    break;
            }
        } catch (IOException e) {
            logger.log(e);
        }
    }

    private void weaveSingleJarToFile(File file, File dest) throws IOException {
        if (dest.exists()) {
            FileUtils.forceDelete(dest);
        }
        JarFile jarFile = new JarFile(file);
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(dest));
        Enumeration<JarEntry> enumeration = jarFile.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry entry = enumeration.nextElement();
            String name = entry.getName();
            JarEntry outJarEntry = new JarEntry(name);

            jarOutputStream.putNextEntry(outJarEntry);
            byte[] modifiedClassBytes = null;
            byte[] sourceClassBytes = IOUtils.toByteArray(jarFile.getInputStream(entry));
            if (name.endsWith(".class")) {
                try {
                    modifiedClassBytes = transformJar(sourceClassBytes, file, entry);
                } catch (Throwable e) {
                    e.printStackTrace();
                    modifiedClassBytes = sourceClassBytes;
                }
            }

            if (modifiedClassBytes == null) {
                modifiedClassBytes = sourceClassBytes;
            }
            jarOutputStream.write(modifiedClassBytes);
            jarOutputStream.flush();
            jarOutputStream.closeEntry();

        }
        jarOutputStream.close();
        jarFile.close();
    }

    private void eachDir(TransformInvocation transformInvocation, boolean isIncremental, DirectoryInput directoryInput) {
        try {
            File dest = transformInvocation.getOutputProvider().getContentLocation(
                    directoryInput.getName(),
                    directoryInput.getContentTypes(),
                    directoryInput.getScopes(),
                    Format.DIRECTORY);
            FileUtils.forceMkdir(dest);
            BiConsumer<File, Status> biConsumer = (file, status) -> {
                try {
                    String srcDirPath = directoryInput.getFile().getAbsolutePath();
                    String destDirPath = dest.getAbsolutePath();
                    String destFilePath = file.getAbsolutePath().replace(srcDirPath, destDirPath);
                    File destFile = new File(destFilePath);
                    logger.log("[DirectoryInput]" + file.getAbsolutePath() + " status:" + status.name());
                    switch (status) {
                        case NOTCHANGED:
                            break;
                        case REMOVED:
                            if (destFile.exists()) {
                                FileUtils.forceDelete(destFile);
                            }
                            break;
                        case ADDED:
                        case CHANGED:
                            try {
                                FileUtils.touch(destFile);
                            } catch (Throwable e) {
                                File pr = destFile.getParentFile();
                                if (!pr.exists()) {
                                    pr.mkdirs();
                                }
                            }
                            weaveSingleClassToFile(file, destFile);
                            break;

                    }
                } catch (IOException e) {
                    logger.log(e);
                }
            };
            //当前是否是增量编译
            if (isIncremental) {
                directoryInput.getChangedFiles().forEach(biConsumer);
            } else {
                com.android.utils.FileUtils.getAllFiles(directoryInput.getFile()).forEach(file -> {
                    biConsumer.accept(file, CHANGED);
                });
            }
        } catch (IOException e) {
            logger.log(e);
        }
    }

    public abstract byte[] transform(byte[] classBytes, File classFile);

    public abstract byte[] transformJar(byte[] classBytes, File jarFile, JarEntry entry);

    private static final String FILE_SEP = File.separator;

    public final void weaveSingleClassToFile(File inputFile, File outputFile) throws IOException {
        if (inputFile.getName().endsWith(".class")) {
            FileUtils.touch(outputFile);
            byte[] classByte = FileUtils.readFileToByteArray(inputFile);
            classByte = transform(classByte, inputFile);
            FileUtils.writeByteArrayToFile(outputFile, classByte);
        } else {
            if (inputFile.isFile()) {
                FileUtils.touch(outputFile);
                FileUtils.copyFile(inputFile, outputFile);
            }
        }
    }
}
