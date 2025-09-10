package es.agonzalez.multiagent.app.core;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;



@Component
public class IntentDetector {

        private static final Pattern RECIPE = Pattern.compile("^!recipe\\b", Pattern.CASE_INSENSITIVE);
        private static final Pattern WEATHER = Pattern.compile("^!weather\\b", Pattern.CASE_INSENSITIVE);
        private static final Pattern NPC = Pattern.compile("^!npc\\b", Pattern.CASE_INSENSITIVE);
        private static final Pattern FOLLOW = Pattern.compile("^!follow\\b", Pattern.CASE_INSENSITIVE);
        private static final Pattern VOICE = Pattern.compile("^!voice\\b", Pattern.CASE_INSENSITIVE);
        private static final Pattern FORGET = Pattern.compile("^!forget\\b", Pattern.CASE_INSENSITIVE);
        private static final Pattern MEMON = Pattern.compile("^!memoria\\s+on\\b", Pattern.CASE_INSENSITIVE);
        private static final Pattern MEMOFF = Pattern.compile("^!voice\\s+off\\b", Pattern.CASE_INSENSITIVE);

        private static final Pattern CHAT = Pattern.compile("^!ai\\b", Pattern.CASE_INSENSITIVE);
        private static final Map<String, String> INTENTS = Map.of(
          "chat","Agent.Chat",
          "recipe_request","Agent.Recipe",
          "memory_forget","Agent.Memory.Forget",
          "memory_on", "Agent.Memory.On",
          "memory_off","Agent.Memory.Off"
        );

        public String detect(String text) {
          if (text == null || text.isBlank()) return null;

          var t = text.trim();
          if (RECIPE.matcher(t).find()) return "recipe_request";
          if (WEATHER.matcher(t).find()) return "weather";
          if (NPC.matcher(t).find()) return "npc_chat";
          if (CHAT.matcher(t).find()) return "chat";
          if (FOLLOW.matcher(t).find()) return "respond_follow";
          if (VOICE.matcher(t).find()) return "play_voice";
          if (FORGET.matcher(t).find()) return "memory_forget";
          if (MEMON.matcher(t).find()) return "memory_on";
          if (MEMOFF.matcher(t).find()) return "memory_off";

          return null;
        }

        public String agent(String intent) 
        {
            if(INTENTS.containsKey(intent))
              return INTENTS.get(intent);
            return null;
        }

        public boolean isValidIntent(String intent) {
          return intent == null ? false : INTENTS.containsKey(intent); 
        }
}
