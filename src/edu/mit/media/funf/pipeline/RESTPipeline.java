package edu.mit.media.funf.pipeline;

import android.util.Log;
import com.google.gson.JsonElement;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.util.LogUtil;

/**
 * Created with IntelliJ IDEA.
 * User: Julian Dax
 * Date: 03/12/13
 * Time: 13:58
 * To change this template use File | Settings | File Templates.
 */
public class RestPipeline implements Pipeline{
    @Override
    public void onCreate(FunfManager manager) {
        Log.d(LogUtil.TAG, "In REST PIPELINE");
    }

    @Override
    public void onRun(String action, JsonElement config) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onDestroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isEnabled() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
    public int getNumnberOfMeasurements(){
        return 5;
    }
}
