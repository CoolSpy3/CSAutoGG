package com.coolspy3.csautogg;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.coolspy3.csmodloader.mod.Entrypoint;
import com.coolspy3.csmodloader.mod.Mod;
import com.coolspy3.csmodloader.network.PacketHandler;
import com.coolspy3.csmodloader.network.SubscribeToPacketStream;
import com.coolspy3.csmodloader.network.packet.Packet;
import com.coolspy3.cspackets.datatypes.MCColor;
import com.coolspy3.cspackets.packets.ClientChatSendPacket;
import com.coolspy3.cspackets.packets.ServerChatSendPacket;
import com.coolspy3.util.ModUtil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(id = "csautogg", name = "CSAutoGG",
        description = "Automatically runs commands at the end of Hypixel games.", version = "1.0.0",
        dependencies = {"csmodloader:[1,2)", "cspackets:[1.2,2)", "csutils:[1,2)"})
public class CSAutoGG implements Entrypoint
{

    private static final Logger logger = LoggerFactory.getLogger(CSAutoGG.class);

    public static final Map<String, List<Pattern>> ggRegexes = new ConcurrentHashMap<>();
    public static final Map<String, Pattern> otherRegexes = new ConcurrentHashMap<>();
    public static final Map<String, String> other = new ConcurrentHashMap<>();
    private boolean isRunning;

    @Override
    public void init(PacketHandler handler)
    {
        try
        {
            Config.load();
        }
        catch (IOException e)
        {
            logger.error("Error loading Config", e);
        }
        ModUtil.executeAsync(this::loadTriggers);
        handler.register(this);
        handler.register(new AutoGGCommand());
    }

    @SubscribeToPacketStream
    public void onChat(ServerChatSendPacket event)
    {
        if (isRunning) return;

        String unformattedText = MCColor.stripFormatting(event.msg);
        Iterator<Pattern> var3;
        Pattern trigger;
        var3 = CSAutoGG.ggRegexes.get("triggers").iterator();

        while (var3.hasNext())
        {
            trigger = (Pattern) var3.next();
            if (trigger.matcher(unformattedText).matches())
            {
                isRunning = true;
                this.sayGG(true, 240);
                return;
            }
        }
    }

    private void sayGG(boolean doSecond, int addedTime)
    {
        ModUtil.executeAsync(() -> {
            try
            {
                Thread.sleep(addedTime);
            }
            catch (InterruptedException e)
            {
                logger.error("InterruptedException", e);
            }

            try
            {
                for (String msg : Config.getInstance().ggMsgs)
                {
                    Packet packet = new ClientChatSendPacket(msg);

                    if (!PacketHandler.getLocal().dispatch(new ClientChatSendPacket(msg)))
                        PacketHandler.getLocal().sendPacket(packet);
                }
            }
            finally
            {
                try
                {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e)
                {
                    logger.error("InterruptedException", e);
                }
                this.isRunning = false;
            }

        });
    }

    public void loadTriggers()
    {
        try
        {
            JsonObject json = JsonParser.parseString(downloadTriggers()).getAsJsonObject();
            getDataFromDownloadedTriggers(json);
        }
        catch (Exception e)
        {
            logger.error("Error downloading triggers", e);
        }
    }

    public String downloadTriggers() throws IOException
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection) (new URL(
                    "http://static.sk1er.club/autogg/regex_triggers_new.json").openConnection());
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/4.76 (Sk1er AutoGG)");
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
            connection.setDoOutput(true);
            try (InputStream is = connection.getInputStream())
            {
                return IOUtils.toString(is, Charset.defaultCharset());
            }
        }
        finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

    public void getDataFromDownloadedTriggers(JsonObject triggerJson)
    {
        ggRegexes.clear();
        otherRegexes.clear();
        other.clear();

        JsonObject firstServerObject;
        try
        {
            firstServerObject = triggerJson.get("servers").getAsJsonObject().get(
                    (String) keySet(triggerJson.get("servers").getAsJsonObject()).iterator().next())
                    .getAsJsonObject();
        }
        catch (NullPointerException var15)
        {
            setDefaultTriggerData();
            return;
        }

        Set<String> ggOptions = keySet(firstServerObject.get("gg_triggers").getAsJsonObject());
        Set<String> otherPatternOptions =
                keySet(firstServerObject.get("other_patterns").getAsJsonObject());
        Set<String> otherOptions = keySet(firstServerObject.get("other").getAsJsonObject());
        Iterator<String> var5 = ggOptions.iterator();

        while (var5.hasNext())
        {
            String s = (String) var5.next();
            ggRegexes.put(s, new ArrayList<>());
        }

        Set<String> keySet;
        try
        {
            keySet = keySet(triggerJson.get("servers").getAsJsonObject());
        }
        catch (NullPointerException var14)
        {
            return;
        }

        Iterator<String> var7 = keySet.iterator();

        String a;
        do
        {
            if (!var7.hasNext())
            {
                setDefaultTriggerData();
                return;
            }

            a = (String) var7.next();
        }
        while (!Pattern.compile(a).matcher("mc.hypixel.net").matches());

        JsonObject data = triggerJson.get("servers").getAsJsonObject().get(a).getAsJsonObject();
        Iterator<String> var10 = ggOptions.iterator();

        String s;
        while (var10.hasNext())
        {
            s = (String) var10.next();
            Iterator<JsonElement> var12 =
                    data.get("gg_triggers").getAsJsonObject().get(s).getAsJsonArray().iterator();

            while (var12.hasNext())
            {
                JsonElement j = (JsonElement) var12.next();
                ggRegexes.get(s).add(Pattern.compile(j.toString()
                        .substring(1, j.toString().length() - 1).replaceAll("\\\\{2}", "\\\\")));
            }
        }

        var10 = otherPatternOptions.iterator();

        String p;
        while (var10.hasNext())
        {
            s = (String) var10.next();
            p = data.get("other_patterns").getAsJsonObject().get(s).toString();
            otherRegexes.put(s,
                    Pattern.compile(p.substring(1, p.length() - 1).replaceAll("\\\\{2}", "\\\\")
                            .replaceAll("(?<!\\\\)\\$\\{antigg_strings}",
                                    String.join("|", new String[] {"gg"}))));
        }

        var10 = otherOptions.iterator();

        while (var10.hasNext())
        {
            s = (String) var10.next();
            p = data.get("other").getAsJsonObject().get(s).toString();
            other.put(s, p.substring(1, p.length() - 1));
        }
    }

    public static Set<String> keySet(JsonObject json) throws NullPointerException
    {
        Set<String> keySet = new HashSet<>();
        Iterator<Map.Entry<String, JsonElement>> var3 = json.entrySet().iterator();

        while (var3.hasNext())
        {
            Map.Entry<String, JsonElement> entry = (Map.Entry<String, JsonElement>) var3.next();
            keySet.add(entry.getKey());
        }

        return keySet;
    }

    private static void setDefaultTriggerData()
    {
        Pattern nonMatching = Pattern.compile("$^");
        otherRegexes.put("antigg", nonMatching);
        otherRegexes.put("anti_karma", nonMatching);
        other.put("msg", "");
    }

}
