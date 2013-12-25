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
package edu.mit.media.funf.config;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.*;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class ConfigurableTypeAdapterFactory implements TypeAdapterFactory {
	 
	 public class ConfigurableExclusionStrategy implements ExclusionStrategy {

		@Override
		public boolean shouldSkipField(FieldAttributes f) {
			return f.getAnnotation(Configurable.class) == null;
		}

		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			return false;
		}
		 
	 }

	 public class ConfigurableFieldNamingStrategy implements FieldNamingStrategy {
		@Override
		public String translateName(Field f) {
			Configurable configAnnotation = f.getAnnotation(Configurable.class);
			if (configAnnotation == null || "".equals(configAnnotation.name())) {
				return FieldNamingPolicy.IDENTITY.translateName(f);
			} else {
				return configAnnotation.name();
			}
		}
		 
	 }

	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        ReflectiveTypeAdapterFactory delegate;
        Map<Type, InstanceCreator<?>> m = new HashMap<Type, InstanceCreator<?>>();
        m.put(type.getRawType(), new InstanceCreator<Object>() {
            @Override
            public Object createInstance(Type type) {
                try {
                    return type.getClass().getConstructor().newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        delegate = new ReflectiveTypeAdapterFactory(
                new ConstructorConstructor(m),
                new ConfigurableFieldNamingStrategy(),
                new Excluder().withExclusionStrategy(new ConfigurableExclusionStrategy(), true, true));

		return delegate.create(gson, type);
	}
	 
	 
}
