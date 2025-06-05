import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SQLManager {

    private static SQLManager instance;

    private final String url = "jdbc:sqlserver://localhost:1433;databaseName=Java_WeatherBot;encrypt=true;trustServerCertificate=true;";
    private final String user = "sa";
    private final String password = "0000";


    private SQLManager() {

    }

    public static synchronized SQLManager getInstance() {
        if (instance == null) {
            instance = new SQLManager();
        }
        return instance;
    }

    public boolean isExist(String serverID) {

        String tableName = "Guild_" + serverID;;
        boolean isExist = false;

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData meta = conn.getMetaData();

            ResultSet tables = meta.getTables(null, null, tableName, new String[] { "TABLE" });

            if (tables.next()) {
                isExist = true;
            }
            tables.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isExist;
    }

    public void createTable(String serverID){
        String sql = "CREATE TABLE [Guild_" + serverID + "] (" +
                     "userId VARCHAR(255) NOT NULL, " +
                     "place VARCHAR(255))";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
            System.out.println("Created table REGISTRATION in given database...");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insert(String serverID, String userID, String place) {
        String sql = "INSERT INTO [Guild_" + serverID + "] (userId, place) VALUES (?, ?)";
        place = formatPlace(place);
        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userID);
            pstmt.setString(2, place);

            pstmt.executeUpdate();
            System.out.println("Inserted data into table Guild_" + serverID);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void remove(String serverID, String userID, String place) {
        String sql = "DELETE FROM [Guild_" + serverID + "] WHERE place = ? AND userId = ?";
        place = formatPlace(place);
        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, place);
            pstmt.setString(2, userID);

            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Deleted " + rowsAffected + " row(s) from table Guild_" + serverID);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isPlaceExist(String serverID, String userID, String place) {
        String sql = "SELECT 1 FROM [Guild_" + serverID + "] WHERE userId = ? AND place = ?";
        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userID);
            pstmt.setString(2, place);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public String formatPlace(String place) {
        if (place == null || !place.contains(",")) return place;

        String[] parts = place.split(",", 2); // 拆成 city 和 country
        String city = capitalizeWords(parts[0].trim());
        String country = capitalizeWords(parts[1].trim());

        return city + "," + country;
    }

    private String capitalizeWords(String input) {
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }

        return sb.toString().trim();
    }

    public List<String> listPlaces(String serverID, String userID) {
        List<String> places = new ArrayList<>();
        String tableName = "Guild_" + serverID;

        String sql = "SELECT place FROM [" + tableName + "] WHERE userId = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                places.add(rs.getString("place"));
            }

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return places;
    }

}
