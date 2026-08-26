package com.financial.cloud.util;


import lombok.extern.slf4j.Slf4j;
import java.text.SimpleDateFormat;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class JsonUtils {

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
        	log.error("Exception readValue", e);
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
        	log.error("Exception DateFormat readValue", e);
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
        	log.error("Exception Class readValue", e);
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
        	log.error("Exception DateFormat readValue", e);
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
        	log.error("Exception writeValueAsString", e);
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
