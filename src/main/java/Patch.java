import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class Patch {
    static byte[] patchLease(byte[] in, Version v) {
        ClassNode cn = new ClassNode();
        new ClassReader(in).accept(cn, 0);
        boolean found = false;
        for (MethodNode m : cn.methods) {
            if (v.leaseMethod.equals(m.name) && v.leaseDesc.equals(m.desc)) {
                found = true;
                m.instructions.clear();
                m.tryCatchBlocks.clear();
                m.localVariables = null;
                InsnList il = new InsnList();
                il.add(new TypeInsnNode(Opcodes.NEW, v.result));
                il.add(new InsnNode(Opcodes.DUP));
                il.add(new InsnNode(Opcodes.ICONST_1));
                il.add(new LdcInsnNode("local"));
                il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, v.result, "<init>",
                        "(ZLjava/lang/String;)V", false));
                il.add(new InsnNode(Opcodes.ARETURN));
                m.instructions.add(il);
                m.maxStack = 3;
                m.maxLocals = 3;
            }
        }
        if (!found) throw new IllegalStateException("lease method not found");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static byte[] patchBoot(byte[] in) {
        ClassNode cn = new ClassNode();
        new ClassReader(in).accept(cn, 0);
        boolean found = false;
        for (MethodNode m : cn.methods) {
            if ("main".equals(m.name) && "([Ljava/lang/String;)V".equals(m.desc)) {
                found = true;
                m.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "Block", "install", "()V", false));
            }
        }
        if (!found) throw new IllegalStateException("HlBoot.main not found");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static byte[] patchStore(byte[] in, Version v) {
        ClassNode cn = new ClassNode();
        new ClassReader(in).accept(cn, 0);
        boolean cat = false;
        boolean open = false;
        for (MethodNode m : cn.methods) {
            if (v.catalog.equals(m.name) && "()Ljava/util/List;".equals(m.desc)) {
                cat = true;
                m.instructions.clear();
                m.tryCatchBlocks.clear();
                m.localVariables = null;
                InsnList il = new InsnList();
                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Local", "catalog",
                        "()Ljava/util/List;", false));
                il.add(new InsnNode(Opcodes.ARETURN));
                m.instructions.add(il);
                m.maxStack = 1;
                m.maxLocals = 0;
            }
            if (v.open.equals(m.name) && "(Ljava/lang/String;)[B".equals(m.desc)) {
                open = true;
                m.instructions.clear();
                m.tryCatchBlocks.clear();
                m.localVariables = null;
                InsnList il = new InsnList();
                il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Local", "open",
                        "(Ljava/lang/String;)[B", false));
                il.add(new InsnNode(Opcodes.ARETURN));
                m.instructions.add(il);
                m.maxStack = 1;
                m.maxLocals = 1;
            }
        }
        if (!cat || !open) throw new IllegalStateException("store methods not found");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: patch <client.jar> <out.jar> [versions.tsv]");
            return;
        }
        Path src = Path.of(args[0]);
        Path dst = Path.of(args[1]);
        Version v = Version.find(src, args.length > 2 ? args[2] : null);
        InputStream helperStream = Patch.class.getResourceAsStream("/Local.class");
        if (helperStream == null) throw new IllegalStateException("Local.class missing");
        byte[] helper = readAll(helperStream);
        InputStream blockStream = Patch.class.getResourceAsStream("/Block.class");
        if (blockStream == null) throw new IllegalStateException("Block.class missing");
        byte[] block = readAll(blockStream);
        Set<String> written = new HashSet<>();
        Files.deleteIfExists(dst);

        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(src));
             ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(dst))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                String name = e.getName();
                if (e.isDirectory()) {
                    zout.putNextEntry(new ZipEntry(name));
                    zout.closeEntry();
                    continue;
                }
                byte[] data = readAll(zin);
                if ((v.lease + ".class").equals(name)) data = patchLease(data, v);
                else if ((v.store + ".class").equals(name)) data = patchStore(data, v);
                else if ("HlBoot.class".equals(name)) data = patchBoot(data);
                else if ("Local.class".equals(name)) data = helper;
                else if ("Block.class".equals(name)) data = block;

                ZipEntry out = new ZipEntry(name);
                if (e.getTime() >= 0) out.setTime(e.getTime());
                zout.putNextEntry(out);
                zout.write(data);
                zout.closeEntry();
                written.add(name);
            }
            if (!written.contains("Local.class")) {
                zout.putNextEntry(new ZipEntry("Local.class"));
                zout.write(helper);
                zout.closeEntry();
            }
            if (!written.contains("Block.class")) {
                zout.putNextEntry(new ZipEntry("Block.class"));
                zout.write(block);
                zout.closeEntry();
            }
        }
        System.out.println("PATCHED=" + dst.toAbsolutePath());
        System.out.println("SIZE=" + Files.size(dst));
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) out.write(b, 0, n);
        return out.toByteArray();
    }
}
