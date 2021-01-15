package com.miqt.asm.method_hook;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;


public class MyPlugin implements Plugin<Project> {


    @Override
    public void apply(@NotNull Project project) {
        AppExtension appExtension = project.getExtensions().getByType(AppExtension.class);
        if (appExtension == null) {
            return;
        }
        appExtension.registerTransform(new MyTransForm(project));
    }
}
