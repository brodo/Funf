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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class ConfigurableTypeAdapterFactory implements TypeAdapterFactory {
	 private ReflectiveTypeAdapterFactory delegate;
	 
	 public ConfigurableTypeAdapterFactory() {
		 delegate = new ReflectiveTypeAdapterFactory(
				 new ConstructorConstructor(),
				 new ConfigurableFieldNamingStrategy(), 
				new Excluder().withExclusionStrategy(new ConfigurableExclusionStrategy(), true, true));
	 }
	 
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
		return delegate.create(gson, type);
	}
	 
	 
}
