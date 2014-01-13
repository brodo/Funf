package edu.mit.media.funf.testapp;

import android.content.Intent;
import android.test.ServiceTestCase;
import com.google.gson.IJsonObject;
import com.google.gson.JsonElement;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.action.ActionAdapter;
import edu.mit.media.funf.datasource.CompositeDataSource;
import edu.mit.media.funf.datasource.DataSource;
import edu.mit.media.funf.datasource.ProbeDataSource;
import edu.mit.media.funf.datasource.StartableDataSource;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.AlarmProbe;
import edu.mit.media.funf.probe.builtin.WifiProbe;

/**
 * Created by Julian Dax on 01/01/14.
 */
public class FunfManagerTests extends ServiceTestCase<FunfManager> {
    public FunfManagerTests() {
        super(FunfManager.class);
    }

    private void startService(){
        Intent startIntent = new Intent();
        startIntent.setClass(getContext(), FunfManager.class);
        startService(startIntent);
    }

    public void testManagerShouldBeInitialized(){
        startService();
        assertNotNull("The service should exist", getService());
    }

    public void testConfigsShouldBeLoadedOnStartup(){
        startService();
        assertNotNull("Preference config map should exist", getService().getConfigStringsFromPreferences());
        assertNotNull("Metadata config map should exist", getService().getConfigStringsFromMetadata());
        assertTrue("Metadata configs map should contain metadata for default config", getService().getConfigStringsFromMetadata().containsKey("default"));
    }

    public void testPipelinesShouldBeCreatedOnStartup(){
        startService();
        assertNotNull("Pipeline map should exist", getService().getAllPipelines());
        assertTrue("A pipeline called 'default' should have been created from the manifest", getService().getAllPipelines().containsKey("default"));
    }

    public void testRegisterBasicPipeline(){
        startService();
        getService().registerPipeline("basic", new BasicPipeline());
        assertEquals("There should be exactly two disabled pipelines (one from manifest one registered) ", 2, getService().getDisabledPipelines().size());
    }
    public void testRegisteredPipelineShouldBeDisabled(){
        startService();
        getService().registerPipeline("basic", new BasicPipeline());
        assertNotNull("The new pipeline should be in the list", getService().getAllPipelines().get("basic"));
        assertEquals("There should be two pipelines (one from manifest one registered)", 2, getService().getAllPipelines().size());
        assertEquals("There should be two disabled pipelines", 2, getService().getDisabledPipelines().size());
        assertEquals("There should be no enabled pipeline", 0, getService().getEnabledPipelines().size());
        assertEquals("Registered pipelines should be disabled by default", false, getService().getDisabledPipelines().get("basic").isEnabled());
    }
    //See AndroidManifest.xml
    public void testGetMetadataFromManifest(){
        startService();
        assertNotNull("There should be config strings from metadata", getService().getConfigStringsFromMetadata());
        assertNotNull("There should be a config called 'default", getService().getConfigStringsFromMetadata().get("default"));
        assertNotNull("The default pipeline from the manifest should be in the pipeline map", getService().getAllPipelines().get("default"));
    }

    public void testPipelineStart() throws InterruptedException {
        startService();
        assertTrue("The default pipeline should be started", getService().enablePipeline("default"));
        Thread.sleep(50);
        assertNotNull("The default pipeline should be in the list of enabled pipelines", getService().getEnabledPipelines().get("default"));
    }

    public void testPipelineStop() throws InterruptedException {
        startService();
        Pipeline p = new BasicPipeline();
        getService().registerPipeline("default", p);
        p.onCreate(getService());
        Thread.sleep(50);
        assertTrue("The default pipeline should be stopped", getService().disablePipeline("default"));
        Thread.sleep(50);
        assertNotNull("The default pipeline should be in the list of disabled pipelines", getService().getDisabledPipelines().get("default"));
    }

    public void testCreatePipelineFormConfig() throws InterruptedException {
        startService();
        String pipelineConfig = "{\"@type\":\"edu.mit.media.funf.pipeline.BasicPipeline\",\n" +
                "        \"name\":\"default\",\n" +
                "        \"version\":1,\n" +
                "        \"data\":[\n" +
                "            {\"@type\":\"edu.mit.media.funf.probe.builtin.WifiProbe\",\n" +
                "             \"@schedule\":{\"interval\": 3, \"duration\": 1}}\n" +
                "        ]\n" +
                "        }";
        BasicPipeline p = (BasicPipeline)getService().createPipelineFromConfig(pipelineConfig);
        assertNotNull("The pipeline should have been created", p);
        assertEquals("There should be one data source", 1, p.data.size());
        StartableDataSource startableDataSource = p.data.get(0);
        assertEquals("The data source should be a composite data source", CompositeDataSource.class, startableDataSource.getClass());
        CompositeDataSource dataSource = (CompositeDataSource) startableDataSource;
        assertNull("There should be no listener", dataSource.getOutputListener());

        dataSource.setListener(new Probe.DataListener() {
            @Override
            public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
                assertNotNull("there should be data", data);
            }

            @Override
            public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {

            }
        });
        Thread.sleep(4000);

    }

}
