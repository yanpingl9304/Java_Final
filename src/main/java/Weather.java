import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Weather {

    String apiKey = Main.getApiKey();
    ConfigManager config = ConfigManager.getInstance();

    // get weather data
    public JSONObject getJSONData(String urlString) {
        try{
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if(conn.getResponseCode() == 400) {
                return null;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            conn.disconnect();

            JSONObject obj = new JSONObject(content.toString());
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // get current weather information including current temperature ,day temperature ,night temperature and weather condition
    public EmbedBuilder GetCurrentWeather(String city){
        city = city.replaceAll(" ", "%20");
        String urlString = "http://api.weatherapi.com/v1/current.json?key=" + apiKey + "&q=" + city + "&aqi=no";
        JSONObject weatherData = getJSONData(urlString);
        
        if(weatherData == null) return null;

        String condition = weatherData.getJSONObject("current").getJSONObject("condition").getString("text");
        String iconUrl = weatherData.getJSONObject("current").getJSONObject("condition").getString("icon");
        String location = weatherData.getJSONObject("location").getString("name");
        String country = weatherData.getJSONObject("location").getString("country");

        double temperature = 0;
        double feelsLike = 0;
 
        if(config.isMetric()){
            temperature = weatherData.getJSONObject("current").getDouble("temp_c");
            feelsLike = weatherData.getJSONObject("current").getDouble("feelslike_c");
        } else {
            temperature = weatherData.getJSONObject("current").getDouble("temp_f");
            feelsLike = weatherData.getJSONObject("current").getDouble("feelslike_f");
        }
        
        if (iconUrl.startsWith("//")) {
            iconUrl = "https:" + iconUrl;
        }


        System.out.println("地點：" + location + ", " + country);
        System.out.println("溫度：" + temperature + "°C");
        System.out.println("天氣狀況：" + condition);
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setImage(iconUrl);
        embed.setTitle("Current Weather Information in " + location + "," + country);

        if(config.isMetric()) {
            embed.setDescription("Temperature : " + temperature + "°C" +"\n"
                    +"Weather : " + condition+"\n"
                    +"Feels Like " + feelsLike + "°C" + "\n\n"
                    );
        } else {
            embed.setDescription("Temperature : " + temperature + "°F" +"\n"
                                +"Weather : " + condition+"\n"
                                +"Feels Like " + feelsLike + "°F" + "\n\n"
                                );
        }


        embed.setColor(0x3498DB); //Sky Blue

        return embed;
    }

    // get detail weather information including  high/low temperature ,wind speed ,humidity ,and UV ,classify humidity and UV
    public EmbedBuilder getDetailWeather(MessageReceivedEvent event ,String city){
        city = city.replaceAll(" ", "%20");

        String urlStringForecast = "http://api.weatherapi.com/v1/forecast.json?key="+ apiKey + "&q=" + city + "&days=1&aqi=no&alerts=no"; 

        JSONObject forecastWeatherData = getJSONData(urlStringForecast);
        if (forecastWeatherData == null ) return null;

        String location = forecastWeatherData.getJSONObject("location").getString("name");
        String country = forecastWeatherData.getJSONObject("location").getString("country");
        double humidity = forecastWeatherData.getJSONObject("current").getDouble("humidity");
        double UV = forecastWeatherData.getJSONObject("current").getDouble("uv");

        double wind = 0;
        double maxtemp = 0;
        double mintemp = 0;
        if(config.isMetric()){
            wind = forecastWeatherData.getJSONObject("current").getDouble("wind_kph");
            maxtemp = forecastWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONObject("day").getDouble("maxtemp_c");
            mintemp = forecastWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONObject("day").getDouble("mintemp_c");
        } else {
            wind = forecastWeatherData.getJSONObject("current").getDouble("wind_mph");
            maxtemp = forecastWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONObject("day").getDouble("maxtemp_f");
            mintemp = forecastWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONObject("day").getDouble("mintemp_f");            
        }


        // output
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Current Weather Detail Information in "+ location + "," + country);
        if(config.isMetric()) {
            embed.setDescription("🌡️   |   Temperature High / Low : " + maxtemp + "°C / " + mintemp + "°C"  + "\n"
                                    +"💨   |   Wind : " + wind +" KPH \n"
                                    +"💧   |   Humidity : " + humidity + "%\n"
                                    +"☀️   |   UV : " + UV + " " + UVLevel(UV));
        } else {
            embed.setDescription("🌡️   |   Temperature High / Low : " + maxtemp + "°F / " + mintemp + "°F"  + "\n"
                        +"💨   |   Wind : " + wind +" MPH \n"
                        +"💧   |   Humidity : " + humidity + "%\n"
                        +"☀️   |   UV : " + UV + " " + UVLevel(UV));
        }


        embed.setColor(0x3498DB); //Sky Blue

        return embed;
    }
    
    public EmbedBuilder getWeeklyForecast(MessageReceivedEvent event ,String city) {

        city = city.replaceAll(" ", "%20");

        String urlStringForecast = "http://api.weatherapi.com/v1/forecast.json?key="+ apiKey + "&q=" + city + "&days=7&aqi=no&alerts=no"; 
        JSONObject forecastWeatherData = getJSONData(urlStringForecast);

        if (forecastWeatherData == null ) return null;

        JSONArray weatherDataArray = forecastWeatherData.getJSONObject("forecast").getJSONArray("forecastday");
        String location = forecastWeatherData.getJSONObject("location").getString("name");
        String country = forecastWeatherData.getJSONObject("location").getString("country");

        StringBuilder sb = new StringBuilder();
        for(int i = 0 ; i < weatherDataArray.length() ; i++){
            String line = "📅  ";

            JSONObject obj = weatherDataArray.getJSONObject(i);

            String condition = obj.getJSONObject("day").getJSONObject("condition").getString("text").trim();
            String monthAndDay = obj.getString("date").toString().replace("2025-" , "").replace("-","/");
            int daily_chance_of_rain = obj.getJSONObject("day").getInt("daily_chance_of_rain");

            double maxtemp = 0;
            double mintemp = 0;

            if(config.isMetric()) {
                maxtemp = obj.getJSONObject("day").getDouble("maxtemp_c");
                mintemp = obj.getJSONObject("day").getDouble("mintemp_c");
            } else {
                maxtemp = obj.getJSONObject("day").getDouble("maxtemp_f");
                mintemp = obj.getJSONObject("day").getDouble("mintemp_f");
            }

            if(config.isMetric()) {
                line += (monthAndDay + "  |  " + condition +"  | 🌡️  " + maxtemp + "°C / " + mintemp + "°C" + " |  🌧️  "+ daily_chance_of_rain +"%\n");
            } else {
                line += (monthAndDay + "  |  " + condition +"  | 🌡️  " + maxtemp + "°F / " + mintemp + "°F" + " |  🌧️  "+ daily_chance_of_rain +"%\n");
            }
            
            line += "========================================================\n";

            sb.append(line);
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Weekly Weather Forecast of "+ location + "," + country);
        embed.setDescription(sb.toString());
        embed.setColor(0x3498DB); //Sky Blue

        return embed;
    }

    public EmbedBuilder getHourlyForecast(MessageReceivedEvent event ,String city){
        String urlStringForecast = "http://api.weatherapi.com/v1/forecast.json?key="+ apiKey + "&q=" + city + "&days=2&aqi=no&alerts=no"; 
        JSONObject forecastTodayWeatherData = getJSONData(urlStringForecast);
        if (forecastTodayWeatherData == null ) return null;
        
        String localtime = forecastTodayWeatherData.getJSONObject("location").getString("localtime");
        String location = forecastTodayWeatherData.getJSONObject("location").getString("name");
        String country = forecastTodayWeatherData.getJSONObject("location").getString("country");
        LocalDateTime local = LocalDateTime.parse(localtime.replace(" " , "T"));
        int hourOfCurrentTime = local.getHour();
        JSONArray forecastByHourToday = forecastTodayWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONArray("hour");
        JSONArray forecastByHourTomorrow = forecastTodayWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(1).getJSONArray("hour");

        int counter = 0;
        StringBuilder sb = new StringBuilder();

        for (int i = hourOfCurrentTime; i < forecastByHourToday.length() && counter < 5; i++) {
            JSONObject data = forecastByHourToday.getJSONObject(i);
            String line = HourlyInfoBuilder(data, i, hourOfCurrentTime);
            sb.append(line);
            counter++;
        }

        if (counter < 5 && forecastByHourTomorrow != null) {
            for (int i = 0; i < forecastByHourTomorrow.length() && counter < 5; i++) {
                JSONObject data = forecastByHourTomorrow.getJSONObject(i);
                String line = HourlyInfoBuilder(data, i, hourOfCurrentTime);
                sb.append(line);
                counter++;
            }
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Hourly Weather Forecast of "+ location + "," + country);
        embed.setDescription(sb.toString());
        embed.setColor(0x3498DB); //Sky Blue

        System.out.println(sb.toString());

        return embed;
    }

    public String HourlyInfoBuilder(JSONObject data , int i , int hourOfCurrentTime) {
        String time = data.getString("time").replace(" ", "T");

        LocalDateTime dateTime = LocalDateTime.parse(time);
        String hour = hourFormat(dateTime.getHour());
        String condition = data.getJSONObject("condition").getString("text");
        double chance_of_rain = data.getDouble("chance_of_rain");
        double temp = 0;

        if(config.isMetric()) {
            temp = data.getDouble("temp_c");
        } else {
            temp = data.getDouble("temp_f");
        }

        String line = "🕐  | ";
        if(i == hourOfCurrentTime) line += "Now\n";
        else line += hour + "\n";

        line += "☁️   | " + condition + "\n";

        if(config.isMetric()) {
            line += "🌡️   | " + temp + "°C\n";
        } else {
            line += "🌡️   | " + temp + "°F\n";
        }

        line += "🌧️   | " + chance_of_rain + "%\n";
        line += "=========================\n";

        return line;
    }

    public String hourFormat(int hour) {
        if (hour == 0) return "12am";
        else if (hour == 12) return "12pm";
        else if (hour > 12) return (hour - 12) + "pm";
        else return hour + "am";
    }

    public String dayOfWeekFormat(String input){
        LocalDate date = LocalDate.parse(input);
        String dayOfWeek = date.getDayOfWeek().toString().toLowerCase();
        String reslut = "";

        switch (dayOfWeek) {
            case "monday":
                reslut = "Mon.";
                break;
            case "tuesday":
                reslut = "Tue.";
                break;
            case "wednesday":
                reslut = "Wed.";
                break;
            case "thursday":
                reslut = "Thu.";
                break;
            case "friday":
                reslut = "Fri.";
                break;
            case "saturday":
                reslut = "Sat.";
                break;
            case "sunday":
                reslut = "Sun.";
                break;
            default:
                break;
        }

        return reslut;
    }

    // classify the UV level
    private String UVLevel(double UV){

        String level = "";
        UV = (int)UV;
        if(0<=UV && UV <=2) {
            level = "LOW";
        } else if (3<=UV && UV <=5) {
            level = "MODERATE";
        } else if (6<=UV && UV <=7) {
            level = "HIGH";
        } else if (8<=UV && UV <=10) {
            level = "VERY HIGH";
        } else if (11<=UV) {
            level = "EXTREME";
        }

        return level;
    }
}
