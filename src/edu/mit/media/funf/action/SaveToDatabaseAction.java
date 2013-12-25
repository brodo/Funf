package edu.mit.media.funf.action;

import android.util.Log;
import com.google.gson.JsonElement;
import com.google.gson.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.util.LogUtil;

/**
 * Created by Julian Dax on 04/12/13.
 */
public class SaveToDatabaseAction extends Action implements Probe.DataListener{
    @Override
    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
        Log.d(LogUtil.TAG, "New probe data: " + data);
    }

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        Log.d(LogUtil.TAG, "New probe data: " + checkpoint);
    }

    protected void execute(String key, IJsonObject data) {
       Log.d(LogUtil.TAG, "New Data, probe Key " + key);
    }
}
