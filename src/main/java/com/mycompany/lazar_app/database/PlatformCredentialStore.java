package com.mycompany.lazar_app.database;

import com.mycompany.lazar_app.util.AppPaths;
import com.mycompany.lazar_app.util.AtomicProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

/** Explicit opt-in native protection: CurrentUser DPAPI or the desktop Secret Service via libsecret. */
public final class PlatformCredentialStore {
    public record Secrets(String password, String trustStorePassword) {
        @Override public String toString() { return "Secrets[redacted]"; }
    }
    private PlatformCredentialStore() {}
    private static boolean windows() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows"); }
    private static String key(DatabaseConfig config) throws Exception {
        String identity = config.getType() + "\n" + config.getHost() + "\n" + config.getPort() + "\n"
                + config.getDatabaseName() + "\n" + config.getUsername();
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
    }
    public static void save(DatabaseConfig config) throws Exception {
        String value = Base64.getEncoder().encodeToString(config.getPassword().getBytes(StandardCharsets.UTF_8)) + "."
                + Base64.getEncoder().encodeToString(config.getTrustStorePassword().getBytes(StandardCharsets.UTF_8));
        String id = key(config);
        if (windows()) {
            String encrypted = dpapi(value, true);
            Properties p = new Properties(); p.setProperty("dpapi", encrypted);
            AtomicProperties.write(AppPaths.configDirectory().resolve("credentials").resolve(id + ".properties"), p, "CurrentUser DPAPI protected credential");
        } else {
            execute(List.of("/usr/bin/secret-tool", "store", "--label=GymFlow database", "application", "GymFlow", "database", id), value);
        }
    }
    public static Secrets load(DatabaseConfig config) throws Exception {
        String value;
        String id = key(config);
        if (windows()) {
            Properties p = new Properties();
            try(var input=Files.newInputStream(AppPaths.configDirectory().resolve("credentials").resolve(id + ".properties"))) { p.load(input); }
            String ciphertext=p.getProperty("dpapi");
            if(ciphertext==null)throw new IOException("Protected credential unavailable.");
            value = dpapi(ciphertext, false);
        } else value=execute(List.of("/usr/bin/secret-tool", "lookup", "application", "GymFlow", "database", id), "");
        String[] pieces = value.split("\\.", -1);
        if(pieces.length!=2)throw new IOException("Protected credential is invalid.");
        return new Secrets(new String(Base64.getDecoder().decode(pieces[0]), StandardCharsets.UTF_8),
                new String(Base64.getDecoder().decode(pieces[1]), StandardCharsets.UTF_8));
    }
    private static String dpapi(String input, boolean protect) throws Exception {
        String operation = protect ? "Protect" : "Unprotect";
        String command = "Add-Type -AssemblyName System.Security; $v=[Console]::In.ReadLine();"
                + "$b=[Convert]::FromBase64String($v);"
                + "$r=[Security.Cryptography.ProtectedData]::" + operation + "($b,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser);"
                + "[Console]::Out.Write([Convert]::ToBase64String($r))";
        Path powershell = Path.of(System.getenv().getOrDefault("SystemRoot", "C:\\Windows"), "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
        String encodedInput = protect ? Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8)) : input;
        String output=execute(List.of(powershell.toString(),"-NoProfile","-NonInteractive","-Command",command),encodedInput+"\n");
        return protect ? output : new String(Base64.getDecoder().decode(output),StandardCharsets.UTF_8);
    }
    private static String execute(List<String> command, String input) throws Exception {
        if(input.length()>8192)throw new IOException("Credential is too long.");
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            try(var output=process.getOutputStream()) { output.write(input.getBytes(StandardCharsets.UTF_8)); }
            if(!process.waitFor(20,TimeUnit.SECONDS))throw new IOException("Platform credential service timed out.");
            if(process.exitValue()!=0)throw new IOException("Platform credential service unavailable or locked.");
            String result=new String(process.getInputStream().readNBytes(16384),StandardCharsets.UTF_8);
            return result.endsWith("\n") ? result.substring(0,result.length()-1).replaceFirst("\\r$", "") : result;
        } finally { if(process.isAlive())process.destroyForcibly(); }
    }
}
