package com.miqt.plugin.sample;

import com.miqt.asm.method_hook.Extension;

public class SampleExtension extends Extension {

    @Override
    public String getExtensionName() {
        return "hook_method";
    }
}
