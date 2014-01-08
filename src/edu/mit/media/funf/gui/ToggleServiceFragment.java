package edu.mit.media.funf.gui;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.R;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.util.LogUtil;

/**
 * Created with IntelliJ IDEA.
 * User: brodo
 * Date: 15/11/13
 * Time: 10:30
 * To change this template use File | Settings | File Templates.
 */
public class ToggleServiceFragment extends Fragment {
    private ToggleButton toggleButton;
    FunfManager funfManager;
    Pipeline pipeline;
    Context appContext;
    private ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(LogUtil.TAG, "Service connected");
            funfManager = ((FunfManager.LocalBinder)service).getManager();
            pipeline = funfManager.getRegisteredPipeline("default");

            toggleButton.setChecked(pipeline.isEnabled());

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            funfManager = null;
            pipeline = null;
        }
    };
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.toggle_service, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        toggleButton = (ToggleButton)getView().findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(toggle);
        appContext = getActivity().getApplicationContext();
        Intent intent = new Intent(appContext, FunfManager.class);
        appContext.bindService(intent, funfManagerConn, appContext.BIND_AUTO_CREATE);
    }

    private CompoundButton.OnCheckedChangeListener toggle = new CompoundButton.OnCheckedChangeListener () {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (funfManager != null) {
                if (isChecked) {
                    funfManager.enablePipeline("default");
                    pipeline = funfManager.getRegisteredPipeline("default");
                } else {
                    funfManager.disablePipeline("default");
                }
            }
        }
    };
}
