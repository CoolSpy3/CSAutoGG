package com.coolspy3.csautogg;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import com.coolspy3.csmodloader.mod.Entrypoint;
import com.coolspy3.csmodloader.mod.Mod;
import com.coolspy3.csmodloader.network.PacketHandler;
import com.coolspy3.csmodloader.network.SubscribeToPacketStream;
import com.coolspy3.csmodloader.network.packet.Packet;
import com.coolspy3.csmodloader.util.Utils;
import com.coolspy3.cspackets.packets.ClientChatSendPacket;
import com.coolspy3.util.ClientChatReceiveEvent;
import com.coolspy3.util.ModUtil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(id = "csautogg", name = "CSAutoGG",
        description = "Automatically runs commands at the end of Hypixel games.", version = "1.2.1",
        dependencies = {"csmodloader:[1.3.1,2)", "cspackets:[1.2.1,2)", "csutils:[1.1.1,2)"})
public class CSAutoGG implements Entrypoint
{

    private static final Logger logger = LoggerFactory.getLogger(CSAutoGG.class);

    public static final List<Pattern> ggRegexes = new CopyOnWriteArrayList<>();
    private volatile boolean isRunning = false;

    static
    {
        loadTriggers();
    }

    public CSAutoGG()
    {
        Utils.reporting(Config::load);
    }

    @Override
    public Entrypoint create()
    {
        return new CSAutoGG();
    }

    @Override
    public void init(PacketHandler handler)
    {
        handler.register(this);
        handler.register(new AutoGGCommand()::register, ClientChatSendPacket.class);
    }

    @SubscribeToPacketStream
    public void onChat(ClientChatReceiveEvent event)
    {
        if (isRunning) return;

        Iterator<Pattern> var3;
        Pattern trigger;
        var3 = CSAutoGG.ggRegexes.iterator();

        while (var3.hasNext())
        {
            trigger = (Pattern) var3.next();
            if (trigger.matcher(event.msg).matches())
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

                    if (!PacketHandler.getLocal().dispatch(packet))
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

    public static void loadTriggers()
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

    public static String downloadTriggers() throws IOException
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection) (new URL(
                    "http://static.sk1er.club/autogg/regex_triggers_3.json").openConnection());
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

    public static void getDataFromDownloadedTriggers(JsonObject json)
    {
        json.get("servers").getAsJsonArray().get(0).getAsJsonObject().get("triggers")
                .getAsJsonArray().forEach(trigger -> {
                    JsonObject triggerObj = trigger.getAsJsonObject();
                    if (triggerObj.get("type").getAsInt() == 0)
                        ggRegexes.add(Pattern.compile(triggerObj.get("pattern").getAsString()));
                });
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

    // private static void setDefaultTriggerData()
    // {
    // Pattern nonMatching = Pattern.compile("$^");
    // otherRegexes.put("antigg", nonMatching);
    // otherRegexes.put("anti_karma", nonMatching);
    // other.put("msg", "");
    // }

}
