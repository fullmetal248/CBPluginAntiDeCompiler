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

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/**
 * Closer.java は、共通的に使用される close処理を集約した、クラスです。
 * <p>
 * 各種 close() 処理では、Exception が発生しても、どうすることも出来ない
 * ケースが多々あります。また、close() 処理中の Exception の為だけに、
 * try ～ catch 節を用意しなければならず、finally 節内からの さらなる
 * throw など、コーディング上、本流以外の箇所で、色々と問題が発生します。
 * ここでは、とりあえず、LogWriter.log するだけにしていますが、
 * 将来的には、エラーを別ファイルにセーブしたり、データベースに書き込んだり
 * 出来ると思います。
 * <p>
 * また、close 処理とは異なりますが、commit や、rollback など、finally 節に
 * 書き込んで、必ず処理したいが、Exception 発生時に、どうしようもない処理も、
 * ここに集約していきます。
 *
 * @author Kazuhiko Hasegawa
 * @version 4.0
 * @since JDK5.0,
 */
public final class Closer {

    /**
     * システム依存の改行記号をセットします。
     */
    private static final String CR = System.getProperty("line.separator");

    /**
     * すべてが staticメソッドなので、コンストラクタを呼び出さなくしておきます。
     */
    private Closer() {
    }

    /**
     * io関連の close 処理時の IOException を無視して、close 処理を行います。
     * ここでは、処理中のエラーは、System.err に出力するだけで無視します。
     * <p>
     * これにより、try ～ catch ～ finally 処理で、close を finally 処理から
     * 例外を送出させなくてすむようになります。
     * 引数が、null の場合は、何も処理しません。(正常:trueを返します。)
     *
     * @param obj Closeableインターフェースを実装したIO関連オブジェクト
     * @return 正常:true/異常:false
     * @og.rev 4.0.0.0 (2007/02/08) 新規追加
     */
    public static boolean ioClose(final Closeable obj) {
        boolean isOK = true;

        try {
            if (obj != null) {
                obj.close();
            }
        } catch (IOException ex) {
            isOK = false;
            String errMsg = "ストリーム close 処理でエラーが発生しました。" + CR
                    + ex.getMessage() + CR
                    + obj.toString();
            Logger.global.log(Level.SEVERE, errMsg);
//            Logger.global.log(Level.SEVERE, ex );
        } catch (RuntimeException ex) {
            isOK = false;
            String errMsg = "予期せぬエラーが発生しました。" + CR
                    + ex.getMessage() + CR
                    + obj.toString();
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log( ex );
        }

        return isOK;
    }

    /**
     * Connection オブジェクトを commit します。
     * ここでは、処理中のエラーは、System.err に出力するだけで無視します。
     *
     * @param conn コネクションオブジェクト
     * @return 正常:true/異常:false
     * @og.rev 4.0.0.0 (2007/02/08) 新規追加
     */
    public static boolean commit(final Connection conn) {
        boolean isOK = true;

        try {
            if (conn != null) {
                conn.commit();
            }
        } catch (SQLException ex) {
            String errMsg = "Connection を commit することが出来ません。" + CR
                    + ex.getMessage() + ":" + ex.getSQLState() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        } catch (RuntimeException ex) {
            isOK = false;
            String errMsg = "予期せぬエラーが発生しました。" + CR
                    + ex.getMessage() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        }

        return isOK;
    }

    /**
     * Connection オブジェクトをrollbackします。
     * ここでは、処理中のエラーは、標準出力に出力するだけで無視します。
     *
     * @param conn コネクションオブジェクト
     * @return 正常:true/異常:false
     * @og.rev 4.0.0.0 (2007/02/08) 新規追加
     */
    public static boolean rollback(final Connection conn) {
        boolean isOK = true;

        try {
            if (conn != null) {
                conn.rollback();
            }
        } catch (SQLException ex) {
            String errMsg = "Connection を rollback することが出来ません。" + CR
                    + ex.getMessage() + ":" + ex.getSQLState() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        } catch (RuntimeException ex) {
            isOK = false;
            String errMsg = "予期せぬエラーが発生しました。" + CR
                    + ex.getMessage() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        }

        return isOK;
    }

    /**
     * Connection オブジェクトをcloseします。
     * ここでは、処理中のエラーは、標準出力に出力するだけで無視します。
     * <p>
     * ここでは、現実の Connection の close() メソッドを呼び出しますので、
     * キャッシュ等で使用しているコネクションには適用しないでください。
     *
     * @param conn コネクションオブジェクト
     * @return 正常:true/異常:false
     * @og.rev 4.0.0.0 (2007/02/08) 新規追加
     * @og.rev 5.5.5.0 (2012/07/28) commit追加
     */
    public static boolean connClose(final Connection conn) {
        boolean isOK = true;

        try {
            if (conn != null && !conn.isClosed()) {
                conn.commit(); // 5.5.5.0 (2012/07/28)
                conn.close();
            }
        } catch (SQLException ex) {
            String errMsg = "Connection を rollback することが出来ません。" + CR
                    + ex.getMessage() + ":" + ex.getSQLState() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        } catch (RuntimeException ex) {
            isOK = false;
            String errMsg = "予期せぬエラーが発生しました。" + CR
                    + ex.getMessage() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        }

        return isOK;
    }

    /**
     * Statement オブジェクトをクローズします。
     * ここでは、処理中のエラーは、標準出力に出力するだけで無視します。
     *
     * @param stmt Statementオブジェクト
     * @return 正常:true/異常:false
     * @og.rev 4.0.0.0 (2007/02/08) 新規追加
     */
    public static boolean stmtClose(final Statement stmt) {
        boolean isOK = true;

        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException ex) {
            String errMsg = "Statement を close することが出来ません。"
                    + ex.getMessage() + ":" + ex.getSQLState() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        } catch (RuntimeException ex) {
            isOK = false;
            String errMsg = "予期せぬエラーが発生しました。" + CR
                    + ex.getMessage() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        }

        return isOK;
    }

    /**
     * ResultSet オブジェクトをクローズします。
     * ここでは、処理中のエラーは、標準出力に出力するだけで無視します。
     *
     * @param result ResultSetオブジェクト
     * @return 正常:true/異常:false
     * @og.rev 4.0.0.0 (2007/02/08) 新規追加
     */
    public static boolean resultClose(final ResultSet result) {
        boolean isOK = true;

        try {
            if (result != null) {
                result.close();
            }
        } catch (SQLException ex) {
            String errMsg = "ResultSet を close することが出来ません。"
                    + ex.getMessage() + ":" + ex.getSQLState() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        } catch (RuntimeException ex) {
            isOK = false;
            String errMsg = "予期せぬエラーが発生しました。" + CR
                    + ex.getMessage() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        }

        return isOK;
    }

    /**
     * ZipFile オブジェクトをクローズします。
     * Jar ファイルも、このメソッドでクローズします。
     * ここでは、処理中のエラーは、標準出力に出力するだけで無視します。
     *
     * @param zipFile ZipFileオブジェクト
     * @return 正常:true/異常:false
     * @og.rev 5.5.2.6 (2012/05/25) findbugs対応に伴い、新規追加
     */
    public static boolean zipClose(final ZipFile zipFile) {
        boolean isOK = true;

        try {
            if (zipFile != null) {
                zipFile.close();
            }
        } catch (IOException ex) {
            String errMsg = "ZipFile/JarFile を close することが出来ません。"
                    + ex.getMessage() + ":" + zipFile.getName() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        } catch (RuntimeException ex) {
            isOK = false;
            String errMsg = "予期せぬエラーが発生しました。" + CR
                    + ex.getMessage() + CR;
            Logger.global.log(Level.SEVERE, errMsg);
//            LogWriter.log(ex);
        }

        return isOK;
    }
}