import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.Calendar;


import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Listeners extends ListenerAdapter {

    ConfigManager config = ConfigManager.getInstance();
    Weather weatherManager = new Weather();

    public void deleteAllCommand(@NotNull ReadyEvent event , Guild guild){

        // 🔹 刪除全域 Slash Commands
        event.getJDA().retrieveCommands().queue(globalCommands -> {
            System.out.println("刪除全域指令:");
            for (Command command : globalCommands) {
                System.out.println(" - " + command.getName());
                event.getJDA().deleteCommandById(command.getId()).queue();
            }
        });

        if (guild != null) {
            // 刪除 Guild Slash Commands
            guild.retrieveCommands().queue(guildCommands -> {
                System.out.println("刪除伺服器指令:");
                for (Command command : guildCommands) {
                    System.out.println(" - " + command.getName());
                    guild.deleteCommandById(command.getId()).queue();
                }
            });
        } else {
            System.out.println("⚠ 無法取得指定的 Guild，請確認 guildId 是否正確。");
        }


    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        for (Guild guild : event.getJDA().getGuilds()) {

            /*
             * 在伺服器註冊指令
             */
            
            guild.upsertCommand("units", "Unit, Metric is Celsius/KPH and Imperial is Fahrenheit/MPH.")
                .addOptions(new OptionData(OptionType.STRING, "unit", "Unit, Metric is Celsius/KPH and Imperial is Fahrenheit/MPH.", true)
                    .addChoice("Metric", "metric")
                    .addChoice("Imperial", "imperial"))
                .queue();

            guild.upsertCommand("addfavorite", "Adds a place to your favorite places.")
                .addOptions(new OptionData(OptionType.STRING, "place", "Enter the place in the format: {city},{country}", true))
                .queue();

            guild.upsertCommand("favorite", "Select one of your saved places.")
                .addOptions(new OptionData(OptionType.STRING, "place", "Choose from your saved places.", true)
                .setAutoComplete(true))
                .queue();

            guild.upsertCommand("removefavorite", "Remove from your saved places.")
                .addOptions(new OptionData(OptionType.STRING, "place", "Choose from your saved places.", true)
                .setAutoComplete(true))
                .queue();
        }
    }

    String currentUser = "x";
    boolean first = true;

    boolean gameStarted = false;
    int[] GameStateQueue = new int[6];
    int[] GameCurState = new int[10];
    int pointer = 0,player=0;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (event.getAuthor().isBot()) return;

        String commandLine = event.getMessage().getContentRaw().toLowerCase();
        String content = event.getMessage().getContentRaw();
        String[] commands = commandLine.split(" ");
        EmbedBuilder embed = new EmbedBuilder();
        MessageChannel channel = event.getChannel();

        if(commands[0].equalsIgnoreCase("user")){
            if (commands.length == 2 && (commands[1].equalsIgnoreCase("a")) || (commands[1].equalsIgnoreCase("b"))){
                currentUser = commands[1].toUpperCase();
                event.getChannel().sendMessage("✅ Successfully assigned user " + currentUser).queue();
            } else {
                event.getChannel().sendMessage("please enter user name(A or B), e.g. user a ").queue();
            }
        }
        if (currentUser.equals("x") && !event.getAuthor().isBot()){
            if (first){
                event.getChannel().sendMessage("please enter user name(A or B), e.g. user a ").queue();
                first = false;
            }
        }

        if(commands[0].equalsIgnoreCase("weather")) {
            String option = commands[commands.length - 1];

            if(commands.length == 2) {

                if(commands[1].equalsIgnoreCase("help")){
                    event.getChannel().sendMessage("Usage: weather [city] [function],\nfunctions:\n" +
                                "detail: getDetailWeather\n" +
                                "weekly: getWeeklyWeather\n" +
                                "hourly: getHourlyWeather").queue();
                } else {
                    System.out.println("==================================================================");
                    System.out.println("GetCurrentWeather");
                    embed = weatherManager.GetCurrentWeather(commands[1]);
                    sendEmbedMessage(event,embed,event.getChannel());
                    System.out.println("==================================================================");
                }

            } else if(commands.length == 3) {

                String cityName = commands[1];
                if (cityName.contains("_")) cityName = cityName.replace("_", "%20");

                switch (option) {
                    case "detail":
                        embed = weatherManager.getDetailWeather(event,cityName);
                        sendEmbedMessage(event,embed,event.getChannel());
                        break;
                    case "hourly":
                        embed = weatherManager.getHourlyForecast(event ,cityName);
                        sendEmbedMessage(event,embed,event.getChannel());
                        break;
                    case "weekly":
                        embed = weatherManager.getWeeklyForecast(event,cityName);
                        sendEmbedMessage(event,embed,event.getChannel());
                        break;
                    default:
                        break;
                }
            }
        }

        /*
         * Calendar
         */


        if (commands[0].equalsIgnoreCase("calendar")) {
            CalendarManager.currentUser = currentUser;
            CalendarManager.handleCalendarCommand(commands, event);
            return;
        }


        /*
         * Game Zone
         */

        if (content.equalsIgnoreCase("help")&&!gameStarted) {
            StringBuilder sb = new StringBuilder();
            sb.append("This is a variant of the classic tic-tac-toe game. The rules are almost the same as the standard version, with one added condition:\n" + //
                                    "\n" + //
                                    "**The Rule of Three:**\n" + //
                                    "There can be a maximum of three identical symbols on the board at any time. When a fourth symbol of the same type is placed, the earliest one on the board will be removed to maintain this rule.\n" + //
                                    "\n" + //
                                    "The input format for placing a move is as follows grid:\n");
            String grid =
                    "1️⃣2️⃣3️⃣\n" + 
                    "4️⃣5️⃣6️⃣\n" +
                    "7️⃣8️⃣9️⃣";
            sb.append(grid);
            sb.append("\nYou only need to key in the correct number.\n");
            channel.sendMessage(sb.toString()).queue();
        }

        if(gameStarted&&content.matches("^[1-9]$")){
            int target=Integer.parseInt(content);;
            String returnMsg="";
            if(GameCurState[target]>0){
                returnMsg+="-----**Invalid target number**-----\n";
                returnMsg+=printGameGrid(GameCurState);
                returnMsg+=roundDet(player); 
            }   
            else{
                GameCurState[target]=player+1;
                GameCurState[GameStateQueue[pointer]]=0;
                GameStateQueue[pointer]=target;
                int winner=detGameEnd(GameCurState);
                pointer = (pointer+1)%6;
                player=(player+1)%2;
                returnMsg+=printGameGrid(GameCurState);
                if(winner==3)
                    returnMsg+=roundDet(player);   
                else{
                    gameStarted=false;
                    if(winner==0)
                        returnMsg+="\n-----Game Over-----\n🟢 win!!!\n";
                    else
                        returnMsg+="\n-----Game Over-----\n❌ win!!!\n";
                }

            }
            channel.sendMessage(returnMsg).queue();
            channel.getHistory().retrievePast(3).queue(messages -> {
                if (messages.size() >= 3) {
                    List<Message> toDelete = messages.subList(1, 3);
                    ((TextChannel) channel).deleteMessages(toDelete).queue();
                }
            });

        }

        if (content.equalsIgnoreCase("newgame")) {
            for(int i=0;i<6;i++)
                GameStateQueue[i]=0;
            for(int i=0;i<10;i++)
                GameCurState[i]=0;
            pointer=0;
            player=0;
            StringBuilder sb = new StringBuilder();
            sb.append("\n-------**New Game Start:**----------\n");
            sb.append(printGameGrid(GameCurState));    
            sb.append(roundDet(0));
            gameStarted=true;
            channel.sendMessage(sb.toString()).queue();
        }

        if (!gameStarted&&content.equalsIgnoreCase("comporg"))
            channel.sendMessage("Such a great course — you just can't help but take it again... and again.").queue();
        if(content.equalsIgnoreCase("np-hard")&&!gameStarted)
            channel.sendMessage("Love is NP-hard. You can guess, but you’ll never verify in polynomial time.").queue();
        if(content.equalsIgnoreCase("recursion")&&!gameStarted)
            channel.sendMessage("To do the recursion, please type \"recursion\" again.").queue();

        if (content.equalsIgnoreCase("clear")) {
            channel.getHistory().retrievePast(100).queue(messages -> {
            ((TextChannel) channel).deleteMessages(messages).queue(
                success -> channel.sendMessage("✅ clear.").queue(),
                error -> channel.sendMessage("❌ error").queue()
            );
            });
        }
    }

    public String printGameGrid(int[] GameCurState){
        StringBuilder sb = new StringBuilder();
        sb.append("**The corresponding number:**\n\n"+"1️⃣2️⃣3️⃣\n" + "4️⃣5️⃣6️⃣\n" +"7️⃣8️⃣9️⃣\n\n");
        sb.append("**Current board:**\n");
        for(int i=1;i<10;i++){
            if(GameCurState[i]==0)
                sb.append("⬜");
            else if(GameCurState[i]==1)
                sb.append("🟢");
            else if(GameCurState[i]==2)
                sb.append("❌");
            else
                sb.append("⁉️");
            if(i%3==0) sb.append("\n");
        }
        return sb.toString();
    }

    public int detGameEnd(int[] GameCurState){
        int pt=0;
        int[] chk = {-4,-4,-4};
        for(int i=1;i<10;i++)
            if(GameCurState[i]==(player+1))
                chk[pt++]=i;
        if(chk[0]%3==chk[1]%3 && chk[0]%3==chk[2]%3)
            return player;
        if((chk[0]-1)/3==(chk[1]-1)/3 && (chk[0]-1)/3==(chk[2]-1)/3)
            return player;
        if(chk[1]==5 && chk[0]+chk[2]==10)
            return player;
        return 3;
    }

    public String roundDet(int who){
        if(who==0) return "\n-----🟢's round-----\n";
        else return "\n-----❌'s round-----\n";
    }

    public void sendEmbedMessage(Object event, EmbedBuilder embed , MessageChannel channel) {
        if(embed == null ) {
            channel.sendMessage("⚠️ Please check your input.").queue();
        } else {
            if (event instanceof SlashCommandInteractionEvent) {
                ((SlashCommandInteractionEvent) event).replyEmbeds(embed.build()).queue();
            } else if (event instanceof MessageReceivedEvent) {
                ((MessageReceivedEvent) event).getChannel().sendMessageEmbeds(embed.build()).queue();
            }
        }
    }



}
