package edu.mit.media.funf.testapp;

import android.test.AndroidTestCase;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.mit.media.funf.config.*;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.util.LogUtil;

public class TestConfigurableParsing extends AndroidTestCase {
    private Gson gson;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(
                        new SingletonTypeAdapterFactory(
                                new DefaultRuntimeTypeAdapterFactory<TestConfigurable>(
                                        getContext(),
                                        TestConfigurable.class,
                                        Test1.class,
                                        new ConfigurableTypeAdapterFactory())
                        )
                )
                .create();
    }

    public void testDefaultProbeConfigShouldExist(){
        assertNotNull("JSON string should not be null", Probe.DEFAULT_CONFIG);
    }

    public void testObjectCreation(){
        Test1 test1 = gson.fromJson(Probe.DEFAULT_CONFIG, Test1.class);
        assertNotNull("An object should be returned", test1);
    }

    public void testIfOverriddenFieldsAreDeserialized(){
        Test1 test1 = gson.fromJson(Probe.DEFAULT_CONFIG, Test1.class);
        assertEquals("Default was not set in configurable", 1, test1.intfield);
        assertEquals("Default private field was not set in configurable", 2, test1.getPrivateField());
    }

    public void testConfigurableDeserializationWithDefaultValues() {
        Test1 test1 = gson.fromJson(Probe.DEFAULT_CONFIG, Test1.class);
        JsonObject expectedConfig = new JsonObject();
        expectedConfig.addProperty("@type", Test1.class.getName());
        expectedConfig.addProperty("intfield", 1);
        expectedConfig.addProperty("privateField", 2);
        assertEquals("Configurable not serialized correctly", expectedConfig, gson.toJsonTree(test1));
    }

    public void testConfigurableDeserializationWithCustomValues() {
        Test1 test1 = gson.fromJson("{\"intfield\": 5, \"privateField\": 3}", Test1.class);
        assertEquals("Specified config was not set in configurable", 5, test1.intfield);
        assertEquals("Specified config on private field was not set in configurable", 3, test1.getPrivateField());
    }

    public void testCustomTypeAnnotations(){
        Test1 test1 = gson.fromJson("{\"@type\":\"" + Test2.class.getName() + "\", \"intfield\": 5, \"privateField\": 3}", Test1.class);
        assertTrue("Runtime type not created from config", test1 instanceof Test2);
        assertEquals("Specified config was not set in configurable", 5, test1.intfield);
        assertEquals("Specified config was not set in configurable", 5, ((Test2)test1).intfield);
        assertEquals("Specified config on private field was not set in configurable", 3, test1.getPrivateField());

    }

    public void testNotConfigurable(){
        Test1 test1 = gson.fromJson("{\"notConfigurable\": \"yes\"}", Test1.class);
        assertEquals("Specified config was not set in configurable", "no", test1.notConfigurable);
    }

    public void testDefaultIsUsedWhenNotConfigurated(){
        TestConfigurable test = gson.fromJson("{\"intfield\": 5, \"privateField\": 3}", TestConfigurable.class);
        assertTrue("Runtime type not created from config", test instanceof Test1);
    }

    public void testNestedTypes(){
        String nestedJson = "{\"@type\":\"" + Test2.class.getName() + "\", \"intfield\": 5, \"privateField\": 3, \"nested\": {\"@type\":\"" + Test2.class.getName() + "\", \"privateField\": 5}}";
        TestConfigurable test = gson.fromJson(nestedJson, TestConfigurable.class);
        assertTrue("Runtime type not created from config", test instanceof Test2);
        Test2 test2 = (Test2)test;
        assertTrue("Runtime type not created from nested config", test2.nested instanceof Test2);
        assertEquals("Specified config was not set in nested configurable", 5, test2.nested.getPrivateField());

//      Taken out until i figure out where and why this is needed, if at all.
//      String expectedSerialized = "{\"@type\":\"TestConfigurableParsing$Test2\",\"nested\":{\"@type\":\"TestConfigurableParsing$Test2\",\"nested\":{\"@type\":\"TestConfigurableParsing$Test1\",\"intfield\":1,\"privateField\":2},\"intfield\":1,\"privateField\":5},\"intfield\":5,\"privateField\":3}";
//      assertEquals("Configurable not serialized to json correctly", new JsonParser().parse(expectedSerialized), gson.toJsonTree(test));
    }


	
	public void testSingleton() {
		Gson gson = new GsonBuilder().registerTypeAdapterFactory(
				new SingletonTypeAdapterFactory(
					new DefaultRuntimeTypeAdapterFactory<TestConfigurable>(
							getContext(), 
							TestConfigurable.class, 
							Test1.class, 
							new ConfigurableTypeAdapterFactory())
					)
				).create();
		
		TestConfigurable test1 = gson.fromJson(Probe.DEFAULT_CONFIG, Test1.class);
		TestConfigurable test2 = gson.fromJson(Probe.DEFAULT_CONFIG, Test1.class);
		TestConfigurable test3 = gson.fromJson(Probe.DEFAULT_CONFIG, TestConfigurable.class);
		assertSame("Singleton Type Adapter should return identical object for identical config and runtime configurations",
				test1, test2);
		assertSame("Singleton Type Adapter should return identical object for identical config and runtime configurations",
				test1, test3);
		
		test1 = gson.fromJson(Probe.DEFAULT_CONFIG, Test1.class);
		test2 = gson.fromJson("{\"privateField\": 5}", Test1.class);
		assertNotSame("Two different configurations should not be cached the same.", test1, test2);
		
		// Specifying default should not return different
        // TODO: this is the way it should work, but need to come up with a method for doing this that does not involve creating an instance to figure out if you need to create a new instance
        // test1 = gson.fromJson(Probe.DEFAULT_CONFIG, Test1.class);
        // test2 = gson.fromJson("{\"privateField\": 2}", Test1.class);
        // assertSame("Two configurations that produce the same runtime object should be the same.", test1, test2);

	}
	
	public interface TestConfigurable {
		
	}
	
	public static class Test1 implements TestConfigurable {
		@Configurable
		public int intfield = 1;
		@Configurable
		private int privateField = 2;
		
		public String notConfigurable = "no";
		
		public int getPrivateField() {
			return privateField;
		}
	}
	
	public static class Test2 extends Test1 {
		@Configurable
		public Test1 nested = new Test1();
	}
}