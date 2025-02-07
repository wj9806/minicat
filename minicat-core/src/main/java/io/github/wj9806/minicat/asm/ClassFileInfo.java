package io.github.wj9806.minicat.asm;

import java.util.Set;

public class ClassFileInfo {
    private final String className;
    private final String superClassName;
    private final Set<String> interfaceNames;
    private final Set<String> annotations;
    private final byte[] bytes;

    public ClassFileInfo(String className, String superClassName, Set<String> interfaceNames, Set<String> annotations, byte[] bytes) {
        this.className = className;
        this.superClassName = superClassName;
        this.interfaceNames = interfaceNames;
        this.annotations = annotations;
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

    public Set<String> getAnnotations() {
        return annotations;
    }
}