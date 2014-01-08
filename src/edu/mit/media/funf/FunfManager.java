/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package edu.mit.media.funf;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import com.google.gson.*;
import edu.mit.media.funf.Schedule.BasicSchedule;
import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.action.Action;
import edu.mit.media.funf.config.*;
import edu.mit.media.funf.datasource.Startable;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.pipeline.PipelineFactory;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.storage.HttpArchive;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.util.LogUtil;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FunfManager extends Service {

    public static final String 
    ACTION_KEEP_ALIVE = "funf.keepalive",
    ACTION_INTERNAL = "funf.internal";

    private static final String
    PIPELINE_TYPE = "funf/pipeline",
    ALARM_TYPE = "funf/alarm";

    private static final String
    DISABLED_PIPELINE_PREFIX = "__DISABLED__";

    private Handler handler;
    private SharedPreferences prefs;
    private ConcurrentHashMap<String,Pipeline> pipelines;

    private Map <String, String> configStringsFromPreferences;
    private Map <String, String> configStringsFromMetadata;
    private Gson gson;

    private static PipelineFactory PIPELINE_FACTORY;
    private static DefaultRuntimeTypeAdapterFactory<Action> ACTION_FACTORY;
    private static ListenerInjectorTypeAdapterFactory DATASOURCE_FACTORY;
    private static SingletonTypeAdapterFactory PROBE_FACTORY;

    /**
     * Get pipeline configuration JSON strings from shared preferences.
     * Pipeline configurations are stored in the shared preferences if they are modified on runtime.
     * @see edu.mit.media.funf.config.ConfigUpdater
     * @return a map containing the name and the corresponding config JSON string stored in the shared preferences.
     */
    public Map<String, String> getConfigStringsFromPreferences() {
        return configStringsFromPreferences;
    }

    /**
     * Get pipeline configuration JSON strings from metadata.
     * @return a map containing the name given in the AndroidManifest meta-data tag and the corresponding
     * config JSON string form strings.xml.
     */
    public Map<String, String> getConfigStringsFromMetadata() {
        return configStringsFromMetadata;
    }

    public Handler getHandler() {
        return handler;
    }

    /**
     * Get the shared preferences
     * @return the shared preferences for this class
     */
    public SharedPreferences getPrefs() {
        return prefs;
    }

    /**
     * Get all pipelines registered in the Manager
     * @return all pipelines, no matter if they are enabled or disablonRuned
     */
    public Map <String, Pipeline> getAllPipelines(){
        return pipelines;
    }

    /**
     * Get all enabled pipelines.
     * @return all enabled pipelines
     */
    public Map <String, Pipeline> getEnabledPipelines(){
        final Map <String, Pipeline> enabled = new HashMap<String, Pipeline>();
        for(String pn : pipelines.keySet()) {
            Pipeline p = pipelines.get(pn);
            if(p.isEnabled()) enabled.put(pn,p);
        }
        return enabled;
    }

    /**
     * Get all disabled pipelines.
     * @return all disabled pipelines
     */
    public Map <String, Pipeline> getDisabledPipelines(){
        final Map <String, Pipeline> disabled = new HashMap<String, Pipeline>();
        for(String pn : pipelines.keySet()) {
            Pipeline p = pipelines.get(pn);
            if(!p.isEnabled()) disabled.put(pn,p);
        }
        return disabled;
    }

    /**
     * Called automatically on startup.
     * Loads and registers all pipelines from shared preferences and metadata
     */
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        getGson(); // Sets gson
        prefs = getSharedPreferences(getClass().getName(), MODE_PRIVATE);
        pipelines = new ConcurrentHashMap<String, Pipeline>();
        loadPipelineConfigs();
        loadPipelines();
    }

    private void loadPipelineConfigs(){
        configStringsFromPreferences = (Map <String, String>)prefs.getAll();
        configStringsFromMetadata = convertConfigMetadataToMap(getMetadata());
    }

    /**
     * Loads all pipelines form metadata and shared preferences.
     * Pipelines form shared preferences always "win" if there are preferences from both sources with the same
     * name.
     */
    public void loadPipelines() {
        for (String key : configStringsFromPreferences.keySet()) {
            String config = configStringsFromPreferences.get(key);
            Pipeline pipeline = createPipelineFromConfig(config);
            registerPipeline(key,pipeline);
        }
        for (String key : configStringsFromMetadata.keySet()) {
            String config = configStringsFromMetadata.get(key);
            Pipeline pipeline = createPipelineFromConfig(config);
            registerPipeline(key,pipeline);
        }
    }

    /**
     * Converts a bundle of information fom AndroidManifest.xml to a map.
     * If the value in the bundle is not a String, it is ignored.
     * @see android.content.pm.PackageManager
     * @param metadata Metadata taken form PackageManager
     * @return The metadata which values are strings
     */
    public Map<String, String> convertConfigMetadataToMap(Bundle metadata){
        HashMap<String, String> result = new HashMap<String, String>();
        for(String key : metadata.keySet()){
            Object config = metadata.get(key);
            if(config instanceof String) result.put(key, (String)config);
        }
        return result;
    }

    public Pipeline createPipelineFromConfig(String config){
        return gson.fromJson(config, Pipeline.class);
    }

    public Pipeline createPipelineFromConfig(JsonObject config){
        return gson.fromJson(config, Pipeline.class);
    }

    public JsonObject getPipelineConfig(String name) {
        String configString = configStringsFromPreferences.get(name);
        return configString == null ? null : new JsonParser().parse(configString).getAsJsonObject();
    }

    /**
     * Restarts a pipeline by name
     * @param pipelineName taken form the AndroidManifest and _not_ from the "name" field in the config JSON
     * @return true if the pipeline could be restarted
     */
    public boolean restartPipeline(String pipelineName){
        disablePipeline(pipelineName);
        enablePipeline(pipelineName);
        return true;
    }

    /**
     * Starts the pipeline of a given name
     * @param pipelineName
     * @return true if the pipeline could be found
     */
    public boolean enablePipeline(String pipelineName){
        Pipeline p = pipelines.get(pipelineName);
        if(p == null) return false;
        p.onCreate(this);
        return true;
    }

    /**
     * Stops the pipeline of a given name
     * @param pipelineName
     * @return true if the pipeline could be found
     */
    public boolean disablePipeline(String pipelineName){
        Pipeline p = pipelines.get(pipelineName);
        if(p== null) return false;
        if(p.isEnabled()) p.onDestroy();
        return true;
    }

    /**
     * Saves a pipeline configuration as a string to the shared preferences
     * @param name The name that is used as a key in the shared preferences
     * @param config A drone configuration
     * @return True if the config could be saved
     */
    public boolean savePipelineConfig(String name, JsonObject config) {
        try {
            // Check if this is a valid pipeline before saving
            getGson().fromJson(config, Pipeline.class);
            prefs.edit().clear();
            return prefs.edit().putString(name, config.toString()).commit();
        } catch (Exception e) {
            Log.e(LogUtil.TAG, "Unable to save config: " + config.toString());
            return false;
        }
    }


    /**
     * Saves all pipelines in the pipeline map to the shared preferences
     */
    public void savePipelineConfigs(){
        for(String pipelineName : pipelines.keySet()){
            Pipeline p = pipelines.get(pipelineName);
            String saveKey = p.isEnabled() ? pipelineName :  DISABLED_PIPELINE_PREFIX + pipelineName;
            savePipelineConfig(saveKey, getPipelineConfig(pipelineName));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (Pipeline pipeline : pipelines.values()) if(pipeline.isEnabled()) pipeline.onDestroy();

        // TODO: save outstanding requests
        // TODO: remove all remaining Alarms

        // TODO: make sure to destroy all probes
        for (Object probeObject : getProbeFactory().getCached()) {
            //String componentString = JsonUtils.immutable(gson.toJsonTree(probeObject)).toString();
            //cancelProbe(componentString);
            ((Probe)probeObject).destroy();
        }
        getProbeFactory().clearCache();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action == null || ACTION_KEEP_ALIVE.equals(action)) {
            // Does nothing, but wakes up FunfManager
        } else if (ACTION_INTERNAL.equals(action)) {
            String type = intent.getType();
            Uri componentUri = intent.getData();
            if (PIPELINE_TYPE.equals(type)) {
                // Handle pipeline action
                String pipelineName = getComponentName(componentUri);
                String pipelineAction = getAction(componentUri);
                Pipeline pipeline = pipelines.get(pipelineName);
                if (pipeline != null) {
                    pipeline.onRun(pipelineAction, null);
                }
            } else if (ALARM_TYPE.equals(type)) {
                // Handle registered alarms
                String probeConfig = getComponentName(componentUri);
                final Probe probe = getGson().fromJson(probeConfig, Probe.class); 
                if (probe instanceof Runnable) {
                    handler.post((Runnable)probe);
                }
            }

        }
        return Service.START_FLAG_RETRY; // TODO: may want the last intent always redelivered to make sure system starts up
    }

    private Bundle getMetadata() {
        try {
            Bundle metadata = getPackageManager().getServiceInfo(new ComponentName(this, this.getClass()), PackageManager.GET_META_DATA).metaData;
            return metadata == null ? new Bundle() : metadata;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Unable to get metadata for the FunfManager service.");
        }
    }

    /**
     * Get a gson builder with the probe factory built in
     * @return
     */
    public GsonBuilder getGsonBuilder() {
        return getGsonBuilder(this);
    }

    public static class ConfigurableRuntimeTypeAdapterFactory<E> extends DefaultRuntimeTypeAdapterFactory<E> {

        public ConfigurableRuntimeTypeAdapterFactory(Context context, Class<E> baseClass, Class<? extends E> defaultClass) {
            super(context, 
                    baseClass, 
                    defaultClass, 
                    new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory()));
        }

    }

    /**
     * Get a gson builder with the probe factory built in
     * @return
     */
    public static GsonBuilder getGsonBuilder(Context context) {
        return new GsonBuilder()
        .registerTypeAdapterFactory(getProbeFactory(context))
        .registerTypeAdapterFactory(getActionFactory(context))
        .registerTypeAdapterFactory(getPipelineFactory(context))
        .registerTypeAdapterFactory(getDataSourceFactory(context))
        .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<Schedule>(context, Schedule.class, BasicSchedule.class))
        .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<ConfigUpdater>(context, ConfigUpdater.class, HttpConfigUpdater.class))
        .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<FileArchive>(context, FileArchive.class, DefaultArchive.class))
        .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<RemoteFileArchive>(context, RemoteFileArchive.class, HttpArchive.class))
        .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<DataListener>(context, DataListener.class, null))
        .registerTypeAdapter(DefaultSchedule.class, new DefaultScheduleSerializer())
        .registerTypeAdapter(Class.class, new JsonSerializer<Class<?>>() {

            @Override
            public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
                return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.getName());
            }
        });
    }


    /**
     * Get a Gson instance which includes the SingletonProbeFactory
     * @return
     */
    public Gson getGson() {
        if (gson == null) {
            gson = getGsonBuilder().create();
        }
        return gson;
    }

    public TypeAdapterFactory getPipelineFactory() {
        return getPipelineFactory(this);
    }


    public static PipelineFactory getPipelineFactory(Context context) {
        if (PIPELINE_FACTORY == null) {
            PIPELINE_FACTORY = new PipelineFactory(context);
        }
        return PIPELINE_FACTORY;
    }

    public SingletonTypeAdapterFactory getProbeFactory() {
        return getProbeFactory(this);
    }


    public static SingletonTypeAdapterFactory getProbeFactory(Context context) {
        if (PROBE_FACTORY == null) {
            PROBE_FACTORY = new SingletonTypeAdapterFactory(
                    new DefaultRuntimeTypeAdapterFactory<Probe>(
                            context, 
                            Probe.class, 
                            null, 
                            new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory())));
        }
        return PROBE_FACTORY;
    }

    public DefaultRuntimeTypeAdapterFactory<Action> getActionFactory() {
        return getActionFactory(this);
    }


    public static DefaultRuntimeTypeAdapterFactory<Action> getActionFactory(Context context) {
        if (ACTION_FACTORY == null) {
            ACTION_FACTORY = new DefaultRuntimeTypeAdapterFactory<Action>(
                            context, 
                            Action.class, 
                            null, 
                            new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory()));
        }
        return ACTION_FACTORY;
    }
    
    public ListenerInjectorTypeAdapterFactory getDataSourceFactory() {
        return getDataSourceFactory(this);
    }


    public static ListenerInjectorTypeAdapterFactory getDataSourceFactory(Context context) {
        if (DATASOURCE_FACTORY == null) {
            DATASOURCE_FACTORY = new ListenerInjectorTypeAdapterFactory(
                    new DefaultRuntimeTypeAdapterFactory<Startable>(
                            context, 
                            Startable.class, 
                            null, 
                            new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory())));
        }
        return DATASOURCE_FACTORY;
    }

    public void registerPipeline(String name, Pipeline pipeline) {
        Log.d(LogUtil.TAG, "Registering pipeline: " + name);
        unregisterPipeline(name);
        pipelines.putIfAbsent(name, pipeline);
    }

    public Pipeline getRegisteredPipeline(String name) {
        return pipelines.get(name);
    }

    public void unregisterPipeline(String name) {
        Pipeline existingPipeline = pipelines.remove(name);
        if (existingPipeline != null && existingPipeline.isEnabled()) {
            existingPipeline.onDestroy();
        }
    }


    public boolean isEnabled(String name) {
        return pipelines.containsKey(name) && pipelines.get(name).isEnabled();
    }

    private String getPipelineName(Pipeline pipeline) {
        for (Map.Entry<String, Pipeline> entry : pipelines.entrySet()) {
            if (entry.getValue() == pipeline) {
                return entry.getKey();
            }
        }
        return null;
    }

    public class LocalBinder extends Binder {
        public FunfManager getManager() {
            return FunfManager.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    public static void registerAlarm(Context context, String probeConfig, Long start, Long interval, boolean exact) {
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = getFunfIntent(context, ALARM_TYPE, probeConfig, "");
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        if (start == null)
            start = 0L;

        if (interval == null || interval <= 0) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, start, pendingIntent);
        } else {
            if (exact) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, start, interval, pendingIntent);
            } else {
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, start, interval, pendingIntent);
            }
        }
    }

    public static void unregisterAlarm(Context context, String probeConfig) {
        Intent intent = getFunfIntent(context, ALARM_TYPE, probeConfig, "");
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            pendingIntent.cancel();
        }
    }    

    /////////////////////////////////////////////
    // Reserve action for later inter-funf communication
    // Use type to differentiate between probe/pipeline
    // funf:<componenent_name>#<action>

    private static final String 
    FUNF_SCHEME = "funf";


    // TODO: should these public?  May be confusing for people just using the library
    private static Uri getComponentUri(String component, String action) {
        return new Uri.Builder()
        .scheme(FUNF_SCHEME)
        .path(component) // Automatically prepends slash
        .fragment(action)
        .build();
    }

    private static String getComponentName(Uri componentUri) {
        return componentUri.getPath().substring(1); // Remove automatically prepended slash from beginning
    }

    private static String getAction(Uri componentUri) {
        return componentUri.getFragment();
    }

    private static Intent getFunfIntent(Context context, String type, String component, String action) {
        return getFunfIntent(context, type, getComponentUri(component, action));
    }

    private static Intent getFunfIntent(Context context, String type, Uri componentUri) {
        Intent intent = new Intent();
        intent.setClass(context, FunfManager.class);
        intent.setPackage(context.getPackageName());
        intent.setAction(ACTION_INTERNAL);
        intent.setDataAndType(componentUri, type);
        return intent;
    }
}
