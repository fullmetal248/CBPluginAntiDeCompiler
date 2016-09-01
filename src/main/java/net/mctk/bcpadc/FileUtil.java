package net.mctk.bcpadc;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by 2016/07/30.
 */
public class FileUtil {
    /**
     * ファイル/ディレクトリを削除する。
     *
     * @param root 削除対象
     */
    public static final void clean(File root) {
        if (root == null || !root.exists()) {
            return;
        }
        if (root.isFile()) {
            // ファイル削除
            if (root.exists() && !root.delete()) {
                root.deleteOnExit();
            }
        } else {
            // ディレクトリの場合、再帰する
            File[] list = root.listFiles();
            for (int i = 0; i < list.length; i++) {
                clean(list[i]);
            }
            if (root.exists() && !root.delete()) {
                root.deleteOnExit();
            }
        }
    }

    // http://stackoverflow.com/questions/14676407/list-all-files-in-the-folder-and-also-sub-folders
    public static void listf(File directory, ArrayList<File> files) {

        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                listf(file, files);
            }
        }
    }
}
