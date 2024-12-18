package com.minicat.asm;

import java.util.Set;

public class ClassFileInfo {
    private final String className;
    private final String superClassName;
    private final Set<String> interfaceNames;
    private final byte[] bytes;

    public ClassFileInfo(String className, String superClassName, Set<String> interfaceNames, byte[] bytes) {
        this.className = className;
        this.superClassName = superClassName;
        this.interfaceNames = interfaceNames;
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getClassName() {
        return className;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public Set<String> getInterfaceNames() {
        return interfaceNames;
    }
}