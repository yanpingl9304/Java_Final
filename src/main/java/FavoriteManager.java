

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

public class FavoriteManager {
    private static final String FILE_PATH = "src/main/resources/favorites.json";
    private static FavoriteManager instance;

    private JSONObject root;

    private FavoriteManager() {
        load();
    }

    public static FavoriteManager getInstance() {
        if (instance == null) {
            instance = new FavoriteManager();
        }
        return instance;
    }

    private void load() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            root = new JSONObject(sb.toString());
        } catch (IOException e) {
            root = new JSONObject();
            root.put("places", new JSONObject());
        }
    }

    private void save() {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            writer.write(root.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addFavorite(String userId, String place) {
        JSONObject places = root.getJSONObject("places");
        JSONArray userPlaces = places.optJSONArray(userId);
        if (userPlaces == null) {
            userPlaces = new JSONArray();
            places.put(userId, userPlaces);
        }
        if (!userPlaces.toList().contains(place)) {
            userPlaces.put(place);
            save();
        }
    }

    public JSONArray getFavorites(String userId) {
        return root.getJSONObject("places").optJSONArray(userId);
    }
}
