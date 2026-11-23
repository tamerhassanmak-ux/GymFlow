package com.mycompany.lazar_app.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;

/** Crash-safe configuration writes. The original remains intact if staging fails. */
public final class AtomicProperties {
    private AtomicProperties() {}
    public static void write(Path target, Properties values, String comment) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path stage=Files.createTempFile(target.toAbsolutePath().getParent(), ".gymflow-config-", ".tmp");
        try {
            try {Files.setPosixFilePermissions(stage, PosixFilePermissions.fromString("rw-------"));}
            catch(UnsupportedOperationException ignored) { /* Windows inherits user-profile ACLs. */ }
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();values.store(bytes,comment);
            try(FileChannel file=FileChannel.open(stage,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer=ByteBuffer.wrap(bytes.toByteArray());while(buffer.hasRemaining()) file.write(buffer);file.force(true);
            }
            Files.move(stage,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(stage);}
    }
}
