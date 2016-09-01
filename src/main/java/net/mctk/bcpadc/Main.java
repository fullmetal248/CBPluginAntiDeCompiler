package net.mctk.bcpadc;

import com.sun.istack.internal.NotNull;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opengion.fukurou.util.ZipArchive;
import org.yaml.snakeyaml.Yaml;

import javax.crypto.*;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by 2016/07/29.
 */
public class Main {

    @Option(name = "-i", usage = "input from this file", metaVar = "INPUT", required = true)
    private File inFile;
    @Option(name = "-o", usage = "output to this file", metaVar = "OUTPUT", required = true)
    private File outFile;
    @Option(name = "-b", usage = "Bukkit lib", required = true)
    private File bukkitFile;

    private Key key;

    public static void main(String... args) {
        new Main().start(args);
    }

    private void start(String... args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            parser.printUsage(System.err);
            return;
        }
        key = generationKey();
        ArrayList<File> files = new ArrayList<File>();
        unzipJar();
        FileUtil.listf(new File(FileUtils.getTempDirectory(), "cbpadc"), files);
        for (File f : files) {
            if (f.getName().endsWith(".class")) encrypt(f);
        }
        editManifest();
        addKeyFile(key);
//        addLoader();
        addLoader(getPackageName(getMainClass()), new File(new File(FileUtils.getTempDirectory(), "cbpadc"), mainClassNameToPath(getMainClass())), bukkitFile);
        editPluginYML();
        rezipJar();
        deleteTmpDir();
    }

    private void encrypt(File file) {
        byte[] inByte = null;
        try {
            System.out.println(file.getAbsolutePath() + "を暗号化中");
            inByte = FileUtils.readFileToByteArray(file);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(inByte);

            FileUtils.writeByteArrayToFile(file, encrypted);
            decryptTest(file, key, inByte);
//            decrypt(inByte, key);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private String getPackageName(String className) {
        StringBuilder path = new StringBuilder();
        String[] arrayStr = className.split("\\.");
        for (int i = 0; i < arrayStr.length - 1; i++) {
            if (i != 0) path.append(".");
            path.append(arrayStr[i]);
        }

        return path.toString();
    }

    @NotNull
    private String mainClassNameToPath(String mainClassName) {
        StringBuilder path = new StringBuilder();
        String[] arrayStr = mainClassName.split(".");
        for (int i = 0; i < arrayStr.length - 1; i++) {
            if (i != 0) path.append(File.pathSeparator);
            path.append(arrayStr[i]);
        }

//        if (path.toString().equals("")) throw new IllegalArgumentException("メインクラス名が渡されていません。");
        return path.toString();
    }

    private String getMainClass() {
        Yaml yaml = new Yaml();

        File pluginYml = new File(new File(FileUtils.getTempDirectory(), "cbpadc"), "plugin.yml");

        Map map = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(pluginYml);
            map = yaml.loadAs(fileInputStream, Map.class);
            fileInputStream.close();
            return (String) map.get("main");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addKeyFile(Key key) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(new File(FileUtils.getTempDirectory(), "cbpadc"), "Key.class"));
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(key);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void editPluginYML() {
        Yaml yaml = new Yaml();
        try {
            String str = FileUtils.readFileToString(new File(new File(FileUtils.getTempDirectory(), "cbpadc"), "plugin.yml"), "utf-8");
            Map map = yaml.loadAs(str, Map.class);
            map.put("main", getPackageName(getMainClass()) + ".PluginLoader");
            str = yaml.dumpAsMap(map);
            FileUtils.writeStringToFile(new File(new File(FileUtils.getTempDirectory(), "cbpadc"), "plugin.yml"), str, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void editManifest() {
        File manifest = new File(new File(new File(FileUtils.getTempDirectory(), "cbpadc"), "META-INF"), "MANIFEST.MF");    // TODO 最適化
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Encrypted-MainClass: ");
            stringBuilder.append(getMainClass());
            stringBuilder.append("\n");
            StringBuilder manifestStr = new StringBuilder(FileUtils.readFileToString(manifest, "utf-8"));
            manifestStr.delete(manifestStr.length() - 2, manifestStr.length());   // WindowsOnly
            manifestStr.append(stringBuilder);
//            FileUtils.write(manifest, manifestStr.toString(), "utf-8", false);
            FileUtils.writeStringToFile(manifest, manifestStr.toString(), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addLoader() {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("PluginLoader");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(new File(FileUtils.getTempDirectory(), "cbpadc"), "PluginLoader.class"));

            byte[] buffer = new byte[1024]; // Adjust if you want
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileOutputStream.close();
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addLoader(String packageName, File target, File bukkitJar) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            String pluginLoaderJava = FileUtils.readFileToString(new File(ClassLoader.getSystemResource("PluginLoader.java").getFile()), "utf-8");
            JavaFileObject file = new JavaSourceFromString("PluginLoader", pluginLoaderJava.replace("{{package}}", "package " + packageName + ";"));

            String[] compileOptions = new String[]{"-d", target.getAbsolutePath(), "-classpath", bukkitJar.getAbsolutePath()};
            Iterable<String> compilationOption = Arrays.asList(compileOptions);
            Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
//            compiler.getStandardFileManager()
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    null,
                    null,
                    compilationOption,
                    null,
                    compilationUnits);

            task.call();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rezipJar() {
        ZipArchive.compress(new File(FileUtils.getTempDirectory(), "cbpadc"), outFile);
    }

    private List<File> unzipJar() {
        List<File> retList = new ArrayList<File>();
        try {
            retList = ZipUtil.unZip(inFile, new File(FileUtils.getTempDirectory(), "cbpadc"), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retList;
    }

    private void deleteTmpDir() {
        try {
            FileUtils.deleteDirectory(new File(FileUtils.getTempDirectory(), "cbpadc"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Key generationKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            generator.init(128, random);
            return generator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // テスト用
    private void decrypt(byte[] bytes, Key key) {
        byte[] inByte = null;
        try {
            inByte = FileUtils.readFileToByteArray(outFile);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            inByte = cipher.doFinal(inByte);

            FileUtils.writeByteArrayToFile(new File(outFile.getPath() + "1"), inByte);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    private void decryptTest(File file, Key key, byte[] orig) {
        try {
            byte[] inBytes = FileUtils.readFileToByteArray(file);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decrypted = cipher.doFinal(inBytes);
            if (!Arrays.equals(decrypted, orig)) System.out.println("復号化テストに失敗: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    class JavaSourceFromString extends SimpleJavaFileObject {

        final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') +
                    Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }

    }
}
