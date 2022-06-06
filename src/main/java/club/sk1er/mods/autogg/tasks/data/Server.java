package club.sk1er.mods.autogg.tasks.data;

public class Server
{
    private final String name;

    private final String kind;

    private final String data;

    private final String messagePrefix;

    private final Trigger[] triggers;

    private DetectorHandler detectorHandler;

    public Server(String name, String kind, String data, String messagePrefix, Trigger[] triggers,
            String[] casualTriggers, String antiGGTrigger, String antiKarmaTrigger)
    {
        this.name = name;
        this.kind = kind;
        this.data = data;
        this.messagePrefix = messagePrefix;
        this.triggers = triggers;
    }

    public String getName()
    {
        return name;
    }

    public DetectorHandler getDetectionHandler()
    {
        if (detectorHandler == null) detectorHandler = DetectorHandler.valueOf(kind);
        return detectorHandler;
    }

    public String getData()
    {
        return data;
    }

    public Trigger[] getTriggers()
    {
        return triggers;
    }

    public String getMessagePrefix()
    {
        return messagePrefix;
    }
}
