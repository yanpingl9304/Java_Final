

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONObject;

public class ConfigManager {
    private static final String CONFIG_PATH = "src\\main\\resources\\config.json";
    private static ConfigManager instance; // Singleton 實例

    private JSONObject config;

    // 私有建構子，外部不能 new
    private ConfigManager() {
        loadConfig();
    }

    // 提供對外取得唯一實例的方法
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    // 載入 JSON 檔案
    private void loadConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_PATH))) {
            StringBuilder jsonText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonText.append(line);
            }
            config = new JSONObject(jsonText.toString());
        } catch (IOException e) {
            e.printStackTrace();
            config = new JSONObject();
        }
    }

    // 儲存 JSON 檔案
    private void saveConfig() {
        try (FileWriter file = new FileWriter(CONFIG_PATH)) {
            file.write(config.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadConfig(); // 重新讀取，以防格式錯誤
    }

    public void setIsMetric(boolean isMetric) {
        config.put("isMetric", isMetric);
        System.out.println(config.toString());
        saveConfig();
    }

    public boolean isMetric() {
        return config.optBoolean("isMetric", true); // 預設為 true（Metric）
    }
}
