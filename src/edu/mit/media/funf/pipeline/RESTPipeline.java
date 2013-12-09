package edu.mit.media.funf.pipeline;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import com.google.gson.JsonElement;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.action.SaveToDatabaseAction;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.StartableDataSource;
import edu.mit.media.funf.probe.builtin.LocationProbe;
import edu.mit.media.funf.util.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Julian Dax
 * Date: 03/12/13
 * Time: 13:58
 * To change this template use File | Settings | File Templates.
 */
public class RestPipeline implements Pipeline{
    @Configurable
    protected List<StartableDataSource> data = new ArrayList<StartableDataSource>();

    @Configurable
    protected String name = "restpipeline";

    @Configurable
    protected int version = 1;

    private Looper looper;
    private Handler handler;
    private boolean isEnabled = false;

    @Override
    public void onCreate(FunfManager manager) {
        HandlerThread thread = new HandlerThread(getClass().getName());
        thread.start();
        looper = thread.getLooper();
        handler = new Handler(looper);
        if(!isEnabled){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    setupDataSources();
                }
            });
        }
    }

    @Override
    public void onRun(String action, JsonElement config) {
        Log.d(LogUtil.TAG, "Action: " + action);
    }

    @Override
    public void onDestroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    protected void setupDataSources() {
        SaveToDatabaseAction action = new SaveToDatabaseAction();
        action.setHandler(handler);
        for (StartableDataSource dataSource : data) {
            dataSource.setListener(action);
            dataSource.start();
        }
        isEnabled = true;
    }

    public int getNumberOfMeasurements(){
        return 5;
    }
}