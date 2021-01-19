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
import org.gradle.internal.impldep.org.joda.time.DateTimeUtils;
import org.gradle.internal.time.Time;
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

        extension = (E) project.getExtensions().create(e.getExtensionName(), e.getClass());
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
        if (isApp) {
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
        super.transform(transformInvocation);
        logger.init();
        boolean isIncremental = transformInvocation.isIncremental();

        logger.log("ProjectName: " + transformInvocation.getContext().getProjectName());
        logger.log("ProjectPath: " + transformInvocation.getContext().getPath());
        logger.log("BuildType  : " + transformInvocation.getContext().getVariantName());
        logger.log("Incremental: " + isIncremental);
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
                eachJar(transformInvocation, jarInput);
            });
        });

        logger.release();
    }

    private void eachJar(TransformInvocation transformInvocation, JarInput jarInput) {
        try {
            String jarName = jarInput.getName();
            File file = jarInput.getFile();
            File temDir = transformInvocation.getContext().getTemporaryDir();
            File dest = transformInvocation.getOutputProvider().getContentLocation(
                    jarInput.getFile().getAbsolutePath(),
                    jarInput.getContentTypes(),
                    jarInput.getScopes(),
                    Format.JAR);

            JarFile jarFile = new JarFile(file);
            File temJar = new File(temDir, file.getName());
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(temJar));
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
                        modifiedClassBytes = transformJar(sourceClassBytes, jarName);
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
            FileUtils.copyFile(temJar, dest);
        } catch (IOException e) {
            logger.log(e);
        }
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
                    switch (status) {
                        case NOTCHANGED:
                            break;
                        case REMOVED:
                            com.android.utils.FileUtils.deleteIfExists(destFile);
                            break;
                        case ADDED:
                        case CHANGED:
                            if (!file.getName().endsWith(".class")) {
                                break;
                            }
                            byte[] bytes = FileUtils.readFileToByteArray(file);
                            try {
                                byte[] resultBytes = transform(bytes);
                                FileUtils.writeByteArrayToFile(file, resultBytes);
                            } catch (Throwable e) {
                                logger.log(e);
                            }
                            break;

                    }
                } catch (IOException e) {
                    logger.log(e);
                }
            };
            if (isIncremental) {
                directoryInput.getChangedFiles().forEach(biConsumer);
            } else {
                com.android.utils.FileUtils.getAllFiles(directoryInput.getFile()).forEach(file -> {
                    biConsumer.accept(file, Status.CHANGED);
                });
            }
            FileUtils.copyDirectory(directoryInput.getFile(), dest);
        } catch (IOException e) {
            logger.log(e);
        }
    }

    public abstract byte[] transform(byte[] classBytes);

    public abstract byte[] transformJar(byte[] classBytes, String jarName);
}
