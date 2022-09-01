package consulting.reservoir.log.dao;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class RsvrLogH2ConnUtil {
    /**
     * h2データベースの接続を取得します。
     * 
     * @return データベース接続。
     * @throws SQLException 入出力例外が発生した場合。
     */
    public static Connection getLogDbConnection() throws SQLException {
        Connection conn;
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new SQLException("No class found: " + ex.toString());
        }

        File fileLogDb = new File("./target/rsvrlog");
        fileLogDb.mkdirs();
        final File dbFile = new File(fileLogDb, "rsvrlog");
        try {
            final String jdbcConnStr = "jdbc:h2:file:" + dbFile.getCanonicalPath()
                    + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
            conn = DriverManager.getConnection(jdbcConnStr, "sa", "");
        } catch (IOException ex) {
            throw new SQLException("想定外: ", ex);
        }
        return conn;
    }
}
