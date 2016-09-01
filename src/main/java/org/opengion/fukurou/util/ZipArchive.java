/*
 * Copyright (c) 2009 The openGion Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.opengion.fukurou.util;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// import java.util.zip.ZipEntry;
// import java.util.zip.ZipInputStream;
// import java.util.zip.ZipOutputStream;

/**
 * ZipArchive.java は、ZIPファイルの解凍・圧縮を行うためのUtilクラスです。
 * <p>
 * zipファイルで、圧縮時のファイルのエンコードを指定できるようにします。
 * ファイルをZIPにするには、java.util.zipパッケージ を利用するのが一般的です。
 * ところが、ファイル名にUTF-8文字エンコーディングを利用する為、Windowsの世界では
 * これを取り扱うアプリケーションも少ないため、文字化けして見えてしまいます。
 * これを解決するには、エンコードが指定できるアーカイバをる要する必要があります。
 * 有名どころでは、ant.jar に含まれる、org.apache.tools.zip と、Apache Commons の
 * org.apache.commons.compress です。
 * org.apache.tools.zip は、java.util.zip とほぼ同じ扱い方、クラス名が使えるので
 * 既存のアプリケーションを作り変えるには、最適です。
 * openGion では、アーカイバ専用ということで、org.apache.commons.compress を
 * 採用します。
 *
 * @author Kazuhiko Hasegawa
 * @version 6.0
 * @og.group ユーティリティ
 * @og.rev 6.0.0.0 (2014/04/11) org.apache.commons.compress パッケージの利用(日本語ファイル名対応)
 * @since JDK5.0,
 */
public final class ZipArchive {

    /**
     * 全てスタティックメソッドのためインスタンスの作成を禁止します。
     */
    private ZipArchive() {
    }

    /**
     * エンコードに、Windows-31J を指定した、ZIPファイルの解凍処理を行います。
     * 引数にフォルダ(targetPath)に指定されたZIPファイル(zipFile)を解凍します。
     * 解凍先のファイルが存在する場合でも、上書きされますので注意下さい。
     *
     * @param targetPath 解凍先のフォルダ
     * @param zipFile    解凍するZIPファイル
     * @return 解凍されたZIPファイルの一覧
     * @og.rev 5.7.1.2 (2013/12/20) org.apache.commons.compress パッケージの利用(日本語ファイル名対応)
     */
    public static List<File> unCompress(final File targetPath, final File zipFile) {
        return unCompress(targetPath, zipFile, "Windows-31J");
    }

    /**
     * エンコードを指定した、ZIPファイルの解凍処理を行います。
     * 引数にフォルダ(targetPath)に指定されたZIPファイル(zipFile)を解凍します。
     * 解凍先のファイルが存在する場合でも、上書きされますので注意下さい。
     * <p>
     * 解凍途中のエラーは、エラー出力に出力するだけで、処理は止めません。
     *
     * @param targetPath 解凍先のフォルダ
     * @param zipFile    解凍するZIPファイル
     * @param encording  ファイルのエンコード(Windows環境では、"Windows-31J" を指定します)
     * @return 解凍されたZIPファイルの一覧
     * @og.rev 4.1.0.2 (2008/02/01) 新規追加
     * @og.rev 4.3.1.1 (2008/08/23) mkdirs の戻り値判定
     * @og.rev 4.3.3.3 (2008/10/22) mkdirsする前に存在チェック
     * @og.rev 5.1.9.0 (2010/08/01) 更新時刻の設定
     * @og.rev 5.7.1.2 (2013/12/20) org.apache.commons.compress パッケージの利用(日本語ファイル名対応)
     */
    public static List<File> unCompress(final File targetPath, final File zipFile, final String encording) {
        List<File> list = new ArrayList<File>();

        // 解凍先フォルダの末尾が'/'又は'\'でなければ区切り文字を挿入
        //      String tmpPrefix = targetPath;
        //      if( File.separatorChar != targetPath.charAt( targetPath.length() - 1 ) ) {
        //              tmpPrefix = tmpPrefix + File.separator;
        //      }

        ZipArchiveInputStream zis = null;
        File tmpFile = null;
        //      String fileName = null;

        try {
            zis = new ZipArchiveInputStream(new BufferedInputStream(new FileInputStream(zipFile)), encording);

            ZipArchiveEntry entry = null;
            while ((entry = zis.getNextZipEntry()) != null) {
                //                      fileName = tmpPrefix + entry.getName().replace( '/', File.separatorChar );
                tmpFile = new File(targetPath, entry.getName());
                list.add(tmpFile);

                // ディレクトリの場合は、自身を含むディレクトリを作成
                if (entry.isDirectory()) {
                    if (!tmpFile.exists() && !tmpFile.mkdirs()) {
                        String errMsg = "ディレクトリ作成に失敗しました。[ファイル名=" + tmpFile + "]";
                        System.err.println(errMsg);
                        continue;
                    }
                }
                // ファイルの場合は、自身の親となるディレクトリを作成
                else {
                    // 4.3.3.3 (2008/10/22) 作成する前に存在チェック
                    if (!tmpFile.getParentFile().exists() && !tmpFile.getParentFile().mkdirs()) {
                        String errMsg = "親ディレクトリ作成に失敗しました。[ファイル名=" + tmpFile + "]";
                        System.err.println(errMsg);
                        continue;
                    }

                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile));
                    try {
                        IOUtils.copy(zis, out);
                    } catch (IOException zex) {
                        String errMsg = "ZIPファイルの作成(copy)に失敗しました。[ファイル名=" + tmpFile + "]";
                        System.err.println(errMsg);
                        continue;
                    } finally {
                        Closer.ioClose(out);
                    }
                }
                // 5.1.9.0 (2010/08/01) 更新時刻の設定
                long lastTime = entry.getTime();
                if (lastTime >= 0 && !tmpFile.setLastModified(lastTime)) {
                    String errMsg = "ZIP更新時刻の設定に失敗しました。[ファイル名=" + tmpFile + "]";
                    System.err.println(errMsg);
                }
            }
        } catch (FileNotFoundException ex) {
            String errMsg = "解凍ファイルが作成できません。[ファイル名=" + tmpFile + "]";
            throw new RuntimeException(errMsg, ex);
        } catch (IOException ex) {
            String errMsg = "ZIPファイルの解凍に失敗しました。[ファイル名=" + tmpFile + "]";
            throw new RuntimeException(errMsg, ex);
        } finally {
            Closer.ioClose(zis);
        }

        return list;
    }

    /**
     * 引数に指定されたファイル又はフィルダ内に存在するファイルをZIPファイルに圧縮します。
     * 圧縮レベルはデフォルトのDEFAULT_COMPRESSIONです。
     * 圧縮ファイルのエントリー情報として本来は、圧縮前後のファイルサイズ、変更日時、CRCを登録する
     * 必要がありますが、ここでは高速化のため、設定していません。(特に圧縮後ファイルサイズの取得は、
     * 非常に不可がかかる。)
     * このため、一部のアーカイバでは正しく解凍できない可能性があります。
     * 既にZIPファイルが存在する場合でも、上書きされますので注意下さい。
     *
     * @param files   圧縮対象のファイル配列
     * @param zipFile ZIPファイル名
     * @return ZIPファイルのエントリーファイル名一覧
     * @og.rev 4.1.0.2 (2008/02/01) 新規追加
     * @og.rev 5.7.1.2 (2013/12/20) org.apache.commons.compress パッケージの利用(日本語ファイル名対応)
     */
    public static List<File> compress(final File[] files, final File zipFile) {
        return compress(files, zipFile, "Windows-31J");
    }

    /**
     * 引数に指定されたファイル又はフィルダ内に存在するファイルをZIPファイルに圧縮します。
     * 圧縮レベルはデフォルトのDEFAULT_COMPRESSIONです。
     * 圧縮ファイルのエントリー情報として本来は、圧縮前後のファイルサイズ、変更日時、CRCを登録する
     * 必要がありますが、ここでは高速化のため、設定していません。(特に圧縮後ファイルサイズの取得は、
     * 非常に不可がかかる。)
     * このため、一部のアーカイバでは正しく解凍できない可能性があります。
     * 既にZIPファイルが存在する場合でも、上書きされますので注意下さい。
     *
     * @param dir     圧縮対象のディレクトリか、ファイル
     * @param zipFile ZIPファイル名
     * @return ZIPファイルのエントリーファイル名一覧
     * @og.rev 5.7.1.2 (2013/12/20) org.apache.commons.compress パッケージの利用(日本語ファイル名対応)
     */
    public static List<File> compress(final File dir, final File zipFile) {
        File[] files = null;
        if (dir.isDirectory()) {
            files = dir.listFiles();
        } else {
            files = new File[]{dir};
        }                                   // 単独の場合は、配列にして渡します。

        return compress(files, zipFile, "Windows-31J");
    }

    /**
     * 引数に指定されたファイル又はフィルダ内に存在するファイルをZIPファイルに圧縮します。
     * 圧縮レベルはデフォルトのDEFAULT_COMPRESSIONです。
     * 圧縮ファイルのエントリー情報として本来は、圧縮前後のファイルサイズ、変更日時、CRCを登録する
     * 必要がありますが、ここでは高速化のため、設定していません。(特に圧縮後ファイルサイズの取得は、
     * 非常に不可がかかる。)
     * このため、一部のアーカイバでは正しく解凍できない可能性があります。
     * 既にZIPファイルが存在する場合でも、上書きされますので注意下さい。
     *
     * @param files     圧縮対象のファイル配列
     * @param zipFile   ZIPファイル名
     * @param encording ファイルのエンコード(Windows環境では、"Windows-31J" を指定します)
     * @return ZIPファイルのエントリーファイル名一覧
     * @og.rev 4.1.0.2 (2008/02/01) 新規追加
     * @og.rev 5.7.1.2 (2013/12/20) org.apache.commons.compress パッケージの利用(日本語ファイル名対応)
     */
    public static List<File> compress(final File[] files, final File zipFile, final String encording) {
        List<File> list = new ArrayList<File>();
        ZipArchiveOutputStream zos = null;

        try {
            zos = new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
            if (encording != null) {
                zos.setEncoding(encording);           // "Windows-31J"
            }

            // ZIP圧縮処理を行います
            addZipEntry(list, zos, "", files);   // 開始フォルダは、空文字列とします。
        } catch (FileNotFoundException ex) {
            String errMsg = "ZIPファイルが見つかりません。[ファイル名=" + zipFile + "]";
            throw new RuntimeException(errMsg, ex);
        } finally {
            Closer.ioClose(zos);
            //              zos.finish();
            //              zos.flush();
            //              zos.close();
        }

        return list;
    }

    /**
     * ZIP圧縮処理を行います。
     * 引数に指定されたFileオブジェクトがディレクトリであれば再帰的に呼び出し、
     * 下層のファイルをエントリーします。但し、そのディレクトリ自身が空である場合は、
     * ディレクトリをエントリー情報として設定します。
     *
     * @param list   ZIPファイルのエントリーファイル名一覧
     * @param zos    ZIP用OutputStream
     * @param prefix 圧縮時のフォルダ
     * @param files  圧縮対象のファイル配列
     * @throws IOException 入出力エラーが発生した場合
     * @og.rev 4.1.0.2 (2008/02/01) 新規追加
     * @og.rev 5.1.9.0 (2010/08/01) 更新時刻の設定 、BufferedInputStream のスコープを小さくする。
     */
    private static void addZipEntry(final List<File> list, final ZipArchiveOutputStream zos, final String prefix, final File[] files) {
        File tmpFile = null;
        try {
            for (File fi : files) {
                tmpFile = fi;                           // エラー時のファイル
                list.add(fi);
                if (fi.isDirectory()) {
                    String entryName = prefix + fi.getName() + "/";
                    ZipArchiveEntry zae = new ZipArchiveEntry(entryName);
                    zos.putArchiveEntry(zae);
                    zos.closeArchiveEntry();

                    addZipEntry(list, zos, entryName, fi.listFiles());
                } else {
                    String entryName = prefix + fi.getName();
                    ZipArchiveEntry zae = new ZipArchiveEntry(entryName);
                    zos.putArchiveEntry(zae);
                    InputStream is = new BufferedInputStream(new FileInputStream(fi));
                    IOUtils.copy(is, zos);
                    zos.closeArchiveEntry();
                    Closer.ioClose(is);
                }
            }
        } catch (FileNotFoundException ex) {
            String errMsg = "圧縮対象のファイルが見つかりません。[ファイル名=" + tmpFile + "]";
            throw new RuntimeException(errMsg, ex);
        } catch (IOException ex) {
            String errMsg = "ZIP圧縮に失敗しました。[ファイル名=" + tmpFile + "]";
            throw new RuntimeException(errMsg, ex);
        }
    }

    /**
     * ファイルの圧縮または解凍を行います。
     *
     * @param args パラメータ
     * @og.rev 4.1.0.2 (2008/02/01) 新規追加
     * <p>
     * Usage: java org.opengion.fukurou.util.ZipArchive comp|uncomp targetPath zipFileName
     * 第1引数 : comp:圧縮 uncomp:解凍
     * 第2引数 : ZIPファイル名
     * 第3引数 : 圧縮時:圧縮対象のファイル又はフォルダ 解凍時:解凍先のフォルダ
     */
    public static void main(final String[] args) {
        String usage = "Usage: java org.opengion.fukurou.util.ZipArchive comp|uncomp targetPath zipFileName";
        if (args.length < 3) {
            System.out.println(usage);
            return;
        }

        // 開始時間
        long start = System.currentTimeMillis();

        List<File> list = null;
        File tgtFile = new File(args[1]);
        File zipFile = new File(args[2]);
        if ("comp".equalsIgnoreCase(args[0])) {
            list = compress(tgtFile, zipFile);
        } else if ("uncomp".equalsIgnoreCase(args[0])) {
            list = unCompress(tgtFile, zipFile);
        } else {
            System.out.println(usage);
            return;
        }

        if (list != null) {
            // 処理時間を表示
            System.out.println("処理時間 : " + (System.currentTimeMillis() - start) + "(ms)");
            // 結果を表示
            for (File fileName : list) {
                System.out.println(fileName);
            }
        }
    }
}
