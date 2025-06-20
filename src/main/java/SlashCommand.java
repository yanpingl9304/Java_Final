import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.entities.Guild;
import java.util.List;


public class SlashCommand extends ListenerAdapter {

    JSONObject configFile;
    ConfigManager config = ConfigManager.getInstance();
    SQLManager DBManager = SQLManager.getInstance();
    Listeners listener = new Listeners();
    Weather weatherManager = new Weather();
    SlashCommand(JSONObject configFile){
        this.configFile = configFile;
    }

    SlashCommand(){

    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        Guild guild = event.getGuild();
        String serverID = guild.getId();
        if (event.getName().equals("units")) {

            boolean isMetric = config.isMetric();
            boolean isChange = false;
            OptionMapping option = event.getOption("unit");
            EmbedBuilder embed = new EmbedBuilder();

            if(option != null) {
                String userChoice = option.getAsString();
                if (userChoice.equals("metric") && !isMetric) {
                    config.setIsMetric(true);
                    isChange = true;
                } else if (userChoice.equals("imperial") && isMetric){
                    config.setIsMetric(false);
                    isChange = true;
                }

                if(isChange) {
                    embed.setTitle("Changed your preferred unit to " + userChoice + ".");
                    embed.setColor(0xDB3444);
                } else {
                    embed.setTitle("Nothing changed, your preferred unit is still " + userChoice + ".");
                    embed.setColor(0xDB3444); 
                }

                event.replyEmbeds(embed.build()).queue();
            }   
        }

        if(event.getName().equals("addfavorite")) {
            OptionMapping option = event.getOption("place");

            String place = option.getAsString().trim();
            String userId = event.getUser().getId();
            place = formatPlace(place);
            if(!DBManager.isExist(serverID)) {
                DBManager.createTable(serverID);
            }

            EmbedBuilder embed = new EmbedBuilder();
            if(DBManager.isPlaceExist(serverID, userId, place)) {
                embed.setTitle(place + " is already in your favorite places.");
                embed.setColor(0xE74C3C);
            } else {
                DBManager.insert(serverID, userId, place);
                embed.setTitle(place + " has been added to your favorite places.");
                embed.setColor(0x27AE60);
            }

            event.replyEmbeds(embed.build()).queue();
        }

        if(event.getName().equals("removefavorite")) {

            String place = event.getOption("place").getAsString().trim();
            String userId = event.getUser().getId();

            place = formatPlace(place);
            DBManager.remove(serverID, userId, place);
            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(place + " has been removed from your favorite places.");
            embed.setColor(0xDB3444);
            event.replyEmbeds(embed.build()).queue();
        }

        if(event.getName().equals("favorite")) {
            String place = event.getOption("place").getAsString().trim();

            EmbedBuilder embed = new EmbedBuilder();
            embed = weatherManager.GetCurrentWeather(place);
            event.replyEmbeds(embed.build()).queue();
        }

    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("favorite") && event.getFocusedOption().getName().equals("place")){
            String serverId = event.getGuild().getId();
            String userId = event.getUser().getId();

            String input = event.getFocusedOption().getValue().toLowerCase();

            List<String> allPlaces = SQLManager.getInstance().listPlaces(serverId, userId);

            List<Command.Choice> options = allPlaces.stream()
                .filter(place -> place.toLowerCase().contains(input))
                .limit(25)
                .map(place -> new Command.Choice(place, place))
                .toList();

            event.replyChoices(options).queue();
        }

        if(event.getName().equals("removefavorite") && event.getFocusedOption().getName().equals("place")) {
            String serverId = event.getGuild().getId();
            String userId = event.getUser().getId();

            String input = event.getFocusedOption().getValue().toLowerCase();

            List<String> allPlaces = SQLManager.getInstance().listPlaces(serverId, userId);

            List<Command.Choice> options = allPlaces.stream()
                .filter(place -> place.toLowerCase().contains(input))
                .limit(25)
                .map(place -> new Command.Choice(place, place))
                .toList();

            event.replyChoices(options).queue();
        }
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

}
