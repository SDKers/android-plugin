package com.miqt.plugin.hookmethod;

import com.miqt.asm.method_hook.Extension;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HookMethodExtension extends Extension {
    //是否启用
    public boolean enable = true;
    //是否将所有的方法都统计，否则只统计注解和正则设置的
    public  boolean all = false;
    //是否打印日志
    public  boolean log = true;
    //是否保存mapping
    public boolean mapping = true;
    //jar包 名称 正则表达式,不设置默认不插桩jar包
    public List<String> jarRegexs = new ArrayList<>();
    //类名称 正则表达式,class全名
    public List<String> classRegexs = new ArrayList<>();
    //方法名称 正则表达式
    public List<String> methodRegexs = new ArrayList<>();
    //是否用插桩后的jar包替换项目中的jar包
    public boolean replaceJar = false;
    public String impl = "";


    public boolean isEnable() {
        return enable;
    }

    public boolean isAll() {
        return all;
    }

    public boolean isLog() {
        return log;
    }

    public boolean isMapping() {
        return mapping;
    }

    public List<String> getJarRegexs() {
        return jarRegexs;
    }

    public List<String> getClassRegexs() {
        return classRegexs;
    }

    public List<String> getMethodRegexs() {
        return methodRegexs;
    }

    public boolean isReplaceJar() {
        return replaceJar;
    }

    public String getImpl() {
        return impl;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("enable", enable);
            jsonObject.put("all", all);
            jsonObject.put("log", log);
            jsonObject.put("mapping", mapping);
            jsonObject.put("jarRegexs", jarRegexs);
            jsonObject.put("classRegexs", classRegexs);
            jsonObject.put("methodRegexs", methodRegexs);
            jsonObject.put("replaceJar", replaceJar);
            jsonObject.put("impl", impl);
        } catch (Throwable e) {
            //JSONException
        }
        return jsonObject;
    }

    @Override
    public String getExtensionName() {
        return "hook_method";
    }
}
