package com.miqt.asm.method_hook;

public abstract class Extension {
   public boolean enable = true;
   public boolean justDebug = false;
   //是否关注jar包进行字节码处理
   public boolean injectJar = false;
   public abstract String getExtensionName();
}
