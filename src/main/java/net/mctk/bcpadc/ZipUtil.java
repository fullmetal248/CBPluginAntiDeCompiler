package net.mctk.bcpadc;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import java.io.*;
import java.util.*;
import java.util.zip.ZipException;

/**
 * Created by 2016/07/30.
 * http://yonchu.hatenablog.com/entry/20100727/1280239709から
 */
public class ZipUtil {
    /**
     * zipファイルを解凍します。<br>
     * Ant1.8.1にて確認
     *
     * @param zipFile   解凍するZIPファイル
     * @param outputDir 解凍先ディレクトリ
     * @param charset   文字コード(ファイル名またはディレクトリ名に使用される文字コード)
     * @return 出力ディレクトリ直下に解凍されたファイルまたディレクトリのリスト
     * @throws FileNotFoundException
     * @throws ZipException
     * @throws IOException
     */
    public static List<File> unZip(final File zipFile, final File outputDir,
                                   final String charset) throws FileNotFoundException, ZipException,
            IOException {
        if (zipFile == null) {
            throw new IllegalArgumentException("引数(zipFile)がnullです。");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("引数(outputDir)がnullです。");
        }
        if (charset == null || charset.isEmpty()) {
            throw new IllegalArgumentException("引数(charset)がnullまたは空文字です。");
        }
        if (outputDir.exists() && !outputDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "引数(outputDir)はディレクトリではありません。outputDir=" + outputDir);
        }

        // 出力ディレクトリ直下に解凍されたファイルまたディレクトリのセット
        final Set<File> fileSet = new HashSet<File>();

        // 解答したファイルの親ディレクトリのセット
        final Set<File> parentDirSet = new HashSet<File>();

        ZipFile zip = null;
        try {
            try {
                // 文字コードを指定することで文字化けを回避
                zip = new ZipFile(zipFile, charset);
            } catch (IOException e) {
                throw e;
            }

            final Enumeration<?> zipEnum = zip.getEntries();
            while (zipEnum.hasMoreElements()) {
                // 解凍するアイテムを取得
                final ZipEntry entry = (ZipEntry) zipEnum.nextElement();

                if (entry.isDirectory()) {
                    // 解凍対象がディレクトリの場合
                    final File dir = new File(outputDir, entry.getName());
                    if (dir.getParentFile()
                            .equals(outputDir)) {
                        // 親ディレクトリが出力ディレクトリなのでfileSetに格納
                        fileSet.add(dir);
                    }
                    // ディレクトリは自分で生成
                    if (!dir.exists() && !dir.mkdirs()) {
//                        logger.error("ディレクトリの生成に失敗しました。dir=" + dir);
                    }
                } else {
                    // 解凍対象がファイルの場合
                    final File file = new File(outputDir, entry.getName());
                    final File parent = file.getParentFile();
                    assert parent != null;

                    if (parent.equals(outputDir)) {
                        // 解凍ファイルの親ディレクトリが出力ディレクトリの場合
                        fileSet.add(file);
                    }

                    if (!parentDirSet.contains(parent)) {
                        // 親ディレクトリが初見の場合
                        parentDirSet.add(parent);

                        // 解凍ファイルの上位にある出力ディレクトリ直下のディレクトリを取得
                        final File rootDir = getRootDir(outputDir, file);
                        assert rootDir != null;
                        fileSet.add(rootDir);

                        // 親ディレクトリを生成
                        if (!parent.exists() && !parent.mkdirs()) {
//                            logger.error("親ディレクトリの生成に失敗しました。parent=" + parent);
                        }
                    }

                    // 解凍対象のファイルを書き出し
                    FileOutputStream fos = null;
                    InputStream is = null;
                    try {
                        fos = new FileOutputStream(file);
                        is = zip.getInputStream(entry);

                        byte[] buf = new byte[1024];
                        int size = 0;
                        while ((size = is.read(buf)) != -1) {
                            fos.write(buf, 0, size);
                        }
                        fos.flush();
                    } catch (FileNotFoundException e) {
                        throw e;
                    } catch (ZipException e) {
                        throw e;
                    } catch (IOException e) {
                        throw e;
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e1) {
//                                logger.error("IOリソース開放失敗(FileOutputStream)", e1);
                            }
                        }
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e1) {
//                                logger.error("IOリソース開放失敗(InputStream)", e1);
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (ZipException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
//                    logger.error("IOリソース開放失敗(ZipFile)", e);
                }
            }
        }

        // Setだと何かと不便なのでListに変換
        List<File> retList = new ArrayList<File>(fileSet);
        // ソート：特に意味はなし
        Collections.sort(retList);

        return retList;
    }

    /**
     * zipファイルを解凍します。
     *
     * @param zipFilePath   解凍するZIPファイルのフルパス
     * @param outputDirPath 解凍先ディレクトリのフルパス
     * @param charset       文字コード(ファイル名またはディレクトリ名に使用される文字コード)
     * @return 出力ディレクトリ直下に解凍されたファイルまたディレクトリのリスト
     * @throws IOException
     */
    public static List<File> unZip(final String zipFilePath,
                                   final String outputDirPath, final String charset) throws IOException {
        if (zipFilePath == null || zipFilePath.isEmpty()) {
            throw new IllegalArgumentException("引数(zipFilePath)がnullまたは空文字です。");
        }
        if (outputDirPath == null || outputDirPath.isEmpty()) {
            throw new IllegalArgumentException(
                    "引数(outputDirPath)がnullまたは空文字です。");
        }
        return unZip(new File(zipFilePath), new File(outputDirPath), charset);
    }


    /**
     * 指定ディレクトリ直下にある、指定ファイルの親ディレクトリを再帰的に検索する。
     *
     * @param dir
     * @param file
     * @return
     */
    private static File getRootDir(final File dir, final File file) {
        assert dir != null;
        assert !dir.exists() || dir.exists() && dir.isDirectory();
        assert file != null;

        final File parent = file.getParentFile();
        if (parent == null) {
            return null;
        }
        if (parent.equals(dir)) {
            return file;
        }
        return getRootDir(dir, parent);
    }
}
