package com.minicat.asm;

import org.objectweb.asm.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class ClassParser {

    public static ClassFileInfo parseClass(InputStream classStream) throws IOException {
        final ClassFileInfo[] classFileInfoHolder = new ClassFileInfo[1];
        ClassReader classReader = new ClassReader(classStream);
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM9) {
            private String className;
            private String superClassName;
            private Set<String> interfaceNames = new HashSet<>();

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.className = name.replace('/', '.');
                this.superClassName = (superName == null || superName.equals("java/lang/Object")) ? null : superName.replace('/', '.');
                if (interfaces != null) {
                    for (String iface : interfaces) {
                        interfaceNames.add(iface.replace('/', '.'));
                    }
                }
            }

            @Override
            public void visitEnd() {
                try {
                    Set<String> allInterfaces = new HashSet<>(interfaceNames);
                    if (superClassName != null) {
                        collectInterfacesFromSuperClass(superClassName, allInterfaces);
                    }

                    byte[] bytes = null;
                    try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); InputStream cloneStream = classStream) {
                        int nextValue;
                        while ((nextValue = cloneStream.read()) != -1) {
                            byteStream.write(nextValue);
                        }
                        bytes = byteStream.toByteArray();
                    }

                    classFileInfoHolder[0] = new ClassFileInfo(className, superClassName, allInterfaces, bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            private void collectInterfacesFromSuperClass(String superClassName, Set<String> allInterfaces) throws IOException {
                InputStream superClassStream = getClassInputStream(superClassName);
                if (superClassStream == null) {
                    return;
                }

                ClassReader superClassReader = new ClassReader(superClassStream);
                superClassReader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        if (interfaces != null) {
                            for (String iface : interfaces) {
                                allInterfaces.add(iface.replace('/', '.'));
                            }
                        }
                        if (superName != null && !superName.equals("java/lang/Object")) {
                            try {
                                collectInterfacesFromSuperClass(superName.replace('/', '.'), allInterfaces);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }, 0);
            }

            private InputStream getClassInputStream(String className) throws IOException {
                String classFilePath = className.replace('.', '/') + ".class";
                return ClassLoader.getSystemResourceAsStream(classFilePath);
            }
        };

        classReader.accept(classVisitor, 0);
        return classFileInfoHolder[0];
    }
}
