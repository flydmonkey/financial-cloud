/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 

 


package com.jinbooks.util;

import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

public class JsonUtils {
	private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

	private static JsonMapper mapper;

	public static void setMapper(JsonMapper jsonMapper) {
		mapper = jsonMapper;
	}

	private static JsonMapper mapper() {
		return mapper;
	}

	private static JsonMapper mapperWithDateFormat(String dateFormat) {
		return mapper().rebuild()
		        .defaultDateFormat(new SimpleDateFormat(dateFormat))
		        .build();
	}
	
    /**
     * jackson Transform json string to java bean object.
     * 
     * @param json String
     * @param bean Object 
     * @return Object 
     */
    public static Object stringToObject(String json, Object bean) {
        try {
            bean = mapper().readValue(json, bean.getClass());
        } catch (Exception e) {
        	logger.error("Exception readValue", e);
        }
        return bean;
    }
    
    /**
     * jackson Transform json string to java bean object.
     * 
     * @param json String
     * @param bean Object 
     * @return Object 
     */
    public static Object stringToObject(String json, Object bean, String dateFormat) {
        try {
            bean = mapperWithDateFormat(dateFormat).readValue(json, bean.getClass());
        } catch (Exception e) {
        	logger.error("Exception DateFormat readValue", e);
        }
        return bean;
    }

    /**
     * jackson Transform json string to java bean object.
     * 
     * @param json String
     * @param cls Class
     * @return Object
     */
    public static <T> T stringToObject(String json, Class<T> cls) {
        T bean = null;
        try {
            bean = mapper().readValue(json, cls);
        } catch (Exception e) {
        	logger.error("Exception Class readValue", e);
        }
        return bean;
    }

    /**
     * jackson Transform json string to java bean object.
     * 
     * @param json String
     * @param cls Class
     * @return Object
     */
    public static <T> T stringToObject(String json, Class<T> cls , String dateFormat) {
        T bean = null;
        try {
            bean = mapperWithDateFormat(dateFormat).readValue(json, cls);
        } catch (Exception e) {
        	logger.error("Exception DateFormat readValue", e);
        }
        return bean;
    }
    
    

    /**
     * jackson Transform java bean object to json string.
     * 
     * @param bean Object
     * @return string
     */
    public static String toString(Object bean) {
        String json = "";
        try {
            json = mapper().writeValueAsString(bean);
        } catch (Exception e) {
        	logger.error("Exception writeValueAsString", e);
        }
        return json;
    }

    /**
     * Pretty-print a JSON string.
     *
     * @param jsonString raw JSON
     * @return formatted JSON, or the original string on failure
     */
    public static String prettyFormat(String jsonString) {
        try {
            return mapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(mapper().readTree(jsonString));
        } catch (Exception e) {
            return jsonString;
        }
    }

}
