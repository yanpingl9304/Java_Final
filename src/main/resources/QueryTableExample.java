import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryTableExample {
    public static void main(String[] args) {
        // 連線字串，注意資料庫名稱
        String url = "jdbc:sqlserver://localhost:1433;databaseName=Java_WeatherBot;encrypt=true;trustServerCertificate=true;";
        String user = "Test";
        String password = "asdasd";

        // 查詢的表格名稱
        String tableName = "Table_1";

        String sql = "SELECT * FROM " + tableName;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            // 取得欄位數量
            int columnCount = rs.getMetaData().getColumnCount();

            // 印出欄位名稱
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(rs.getMetaData().getColumnName(i) + "\t");
            }
            System.out.println();

            // 印出每一列資料
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }

        } catch (SQLException e) {
            System.out.println("資料庫連線或查詢失敗");
            e.printStackTrace();
        }
    }
}