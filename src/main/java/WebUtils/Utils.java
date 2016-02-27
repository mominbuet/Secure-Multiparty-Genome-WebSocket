/*
 * Md. Momin Al Aziz momin.aziz.cse @ gmail.com	
 * http://www.mominalaziz.com
 */
package WebUtils;

import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;

/**
 *
 * @author shad942
 */
public class Utils {

    public static String getMessage(Map<String, String> map) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        for (Map.Entry<String, String> entrySet : map.entrySet()) {
            json.add(entrySet.getKey(), entrySet.getValue());
        }
        return json.build().toString();
    }

    public static String getMessage(String type, JsonArray message) {
        return Json.createObjectBuilder()
                .add("type", type)
                .add("msg", message)
                .build()
                .toString();
    }

    /**
     * Create a json representation.
     *
     * @param message
     * @return
     */
    public static String getMessage(String type, String message) {
        return Json.createObjectBuilder()
                .add("type", type)
                .add("msg", message)
                .build()
                .toString();
    }
}
