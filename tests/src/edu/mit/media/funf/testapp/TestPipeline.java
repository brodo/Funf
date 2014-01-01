package edu.mit.media.funf.testapp;

import android.test.AndroidTestCase;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.datasource.CompositeDataSource;
import edu.mit.media.funf.datasource.StartableDataSource;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.builtin.LocationProbe;
import edu.mit.media.funf.util.IOUtil;
import edu.mit.media.funf.util.LogUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.Buffer;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class TestPipeline extends AndroidTestCase {
	
	private Gson gson;
	private Queue<String> actions;
	
	public static final String 
	CREATED = "created",
	RAN = "ran",
	DESTROYED = "destroyed";
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gson = FunfManager.getGsonBuilder(getContext()).create();
		actions = new LinkedList<String>();
	}


	
	public String getBasicPipeLineString(){
        int resId = R.raw.basic_pipeline_config;

        BufferedInputStream inputStream =  new BufferedInputStream(getContext().getResources().openRawResource(resId));
        try {
            return IOUtil.inputStreamToString(inputStream, "utf8");
        } catch (IOException e) {
            Log.e(LogUtil.TAG, e.getMessage());
            return "";
        }
    }
	
	
	public void testPipelineLifecycle() {
	
	}
	
	public void testLoadPipeline() {

 		Pipeline pipeline = gson.fromJson(getBasicPipeLineString(), Pipeline.class);
		assertTrue("Should respect runtime type.", pipeline instanceof BasicPipeline);
		BasicPipeline basicPipeline = (BasicPipeline)pipeline;
		Map<String,StartableDataSource> schedules = basicPipeline.schedules;
		assertNotNull("Schedules should exists.", schedules);
        assertNotNull("Data list should exist", basicPipeline.data);
        assertEquals("There should be exactly one item in the data list", basicPipeline.data.size(), 1);


	}
}
