package com.coolspy3.csautogg;

import java.io.IOException;

import com.coolspy3.cspackets.datatypes.MCColor;
import com.coolspy3.util.ListCommand;
import com.coolspy3.util.ModUtil;

public class AutoGGCommand extends ListCommand
{

    public AutoGGCommand()
    {
        super("/gg", "message/command");
    }

    @Override
    public boolean validate(String str)
    {
        return true;
    }

    @Override
    public void add(String str) throws IOException
    {
        Config.getInstance().ggMsgs.add(str);
        Config.save();
    }

    @Override
    public void remove(String str) throws IOException
    {
        Config.getInstance().ggMsgs.remove(str);
        Config.save();
    }

    @Override
    public void list()
    {
        ModUtil.sendMessage(MCColor.AQUA + "Current GG Msgs:");
        if (Config.getInstance().ggMsgs.isEmpty())
        {
            ModUtil.sendMessage(MCColor.AQUA + "<None>");
            return;
        }
        for (String msg : Config.getInstance().ggMsgs)
        {
            ModUtil.sendMessage(MCColor.AQUA + msg);
        }
    }

}
