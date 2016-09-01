{{package}}

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by 2016/08/01.
 */
public class PluginLoader extends JavaPlugin {

    private Key key;
    private Map<String, byte[]> classes;
    private JavaPlugin javaPlugin;
    private Class mainClass;
    private List<String> stub = new ArrayList<>();
    private String mainClassName;

    @Override
    public void onEnable() {
        resetPluginClassLoader();

        classes = new HashMap<>();
        try {
            JarFile jarFile = new JarFile(getFile());
            this.mainClassName = jarFile.getManifest().getMainAttributes().getValue("Encrypted-MainClass");
            JarEntry keyJarEntry = jarFile.getJarEntry("Key.class");
            key = getKey(jarFile.getInputStream(keyJarEntry));
            for (Enumeration<? extends JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                String className = entry.getName();
                if (entry.isDirectory()) continue;
                if (!entry.getName().endsWith(".class")) continue;
                if (entry.getName().equals("Key.class") || entry.getName().contains("PluginLoader.class")) continue;

                InputStream is = jarFile.getInputStream(entry);
                byte[] encryptedClassBytes = read(is);

                classes.put(className, encryptedClassBytes);
            }
            jarFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        do {
            Set<Map.Entry<String, byte[]>> entrySet = classes.entrySet();
            for (Map.Entry<String, byte[]> entry : entrySet) {
                byte[] encryptedClassBytes = entry.getValue();
                try {
                    System.out.println(entry.getKey() + "の復号化中");
                    loadClass(decrypt(encryptedClassBytes, key), entry.getKey());

                } catch (ClassFormatError classFormatError) {
//                    classFormatError.printStackTrace();
                }
            }
            for (String s : stub) {
                classes.remove(s);
            }
            stub.clear();
        } while (classes.size() != 0);
        // 復号化完了

        runPlugin();
    }

    @Override
    public void onDisable() {
        if (this.javaPlugin != null) getServer().getPluginManager().disablePlugin(this.javaPlugin);

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return this.javaPlugin == null ? false : this.javaPlugin.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.javaPlugin == null ? null : this.javaPlugin.onTabComplete(sender, command, alias, args);
    }

    @Override
    public void onLoad() {
        if (this.javaPlugin != null) this.javaPlugin.onLoad();
    }

    private void pluginSurikae(JavaPlugin javaPlugin) {  // TODO 名前
//        Bukkit.getPluginManager();
        Class<SimplePluginManager> simplePluginManagerClass = SimplePluginManager.class;
        try {
            Field lookupNamesField = simplePluginManagerClass.getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(Bukkit.getPluginManager());
            lookupNames.put(getName().replace(' ', '_'), javaPlugin);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private byte[] read(InputStream inputStream) {
        byte[] buf = new byte[1024];
        int len;
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            while ((len = bufferedInputStream.read(buf)) > 0) {
                byteArrayOutputStream.write(buf, 0, len);
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }

    private Key getKey(InputStream inputStream) {
        Key retKey = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            retKey = (Key) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return retKey;
    }

    private byte[] decrypt(byte[] bytes, Key key) {
        byte[] inByte = null;
        try {
//            inByte = FileUtils.readFileToByteArray(outFile);
            if (key == null) throw new IllegalArgumentException("Keyがnullです。");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            inByte = cipher.doFinal(bytes);

//            FileUtils.writeByteArrayToFile(new File(outFile.getPath() + "1"), inByte);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
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
        return inByte;
    }

    private void resetPluginClassLoader() {
        Object loader = getClassLoader();
        try {
            Field pluginField = loader.getClass().getDeclaredField("plugin");
            Field pluginInitField = loader.getClass().getDeclaredField("pluginInit");

            pluginField.setAccessible(true);
            pluginInitField.setAccessible(true);

            pluginField.set(loader, null);
            pluginInitField.set(loader, null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void loadClass(byte[] bytes, String name) throws ClassFormatError {
        try {
            String packageName = name.replaceAll("/", ".").substring(0, name.length() - 6);
            Method define0Method = ClassLoader.class.getDeclaredMethod("defineClass0", new Class[]{String.class, byte[].class, int.class, int.class, ProtectionDomain.class});
            define0Method.setAccessible(true);
            Class loadedClass = (Class) define0Method.invoke(getClassLoader(), packageName, bytes, 0, bytes.length, null);
            if (packageName.equals(mainClassName)) {
                this.mainClass = loadedClass;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ClassFormatError) {
                throw (ClassFormatError) cause;
            }
        }
        stub.add(name);
    }

    private void runPlugin() {
        try {
            Constructor loadedClassConstructor = mainClass.getConstructor(null);
            loadedClassConstructor.setAccessible(true);

            this.javaPlugin = (JavaPlugin) loadedClassConstructor.newInstance();
            pluginSurikae(this.javaPlugin);
            getServer().getPluginManager().enablePlugin(this.javaPlugin);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private String getPackageName(String className) {
        StringBuilder path = new StringBuilder();
        String[] arrayStr = className.split("\\.");
        for (int i=0; i<arrayStr.length-1; i++) {
            if (i!=0) path.append(".");
            path.append(arrayStr[i]);
        }

        return path.toString();
    }

    private String mainClassNameToPath(String mainClassName) {
        StringBuilder path = new StringBuilder();
        String[] arrayStr = mainClassName.split(".");
        for (int i=0; i<arrayStr.length-1; i++) {
            if (i!=0) path.append(File.pathSeparator);
            path.append(arrayStr[i]);
        }
        return path.toString();
    }
}
