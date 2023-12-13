package soargroup.mobilesim.util;

import java.awt.*;
import java.io.*;
import java.util.*;

import april.config.*;
import april.jmat.*;
import april.vis.VisCameraManager.CameraPosition;
import april.util.*;

public class Util
{
    public static String configPath = "$MOBILE_SIM_HOME/config/robot.config";
    private static Config config = null;

    /** Give a string of key=token pairs, extract the token
     *  value corresponding to the given key
     **/
    public static String getTokenValue(String params, String tokenKey)
    {
        String[] tokens = params.split(",");
        for (String token: tokens) {
            String[] keyValuePair = token.split("=");
            if (keyValuePair.length < 2)
                continue;
            if (keyValuePair[0].equals(tokenKey))
                return keyValuePair[1];
        }

        return null;    // No token found
    }

    public static ArrayList<String> getPossibleValues(String[] pairs, String key)
    {
        ArrayList<String> values = new ArrayList<String>();
        for (String pair: pairs) {
            String[] keyValuePair = pair.split("=");
            if (keyValuePair[0].equals(key))
                values.add(keyValuePair[1]);
        }

        return values;
    }

    public static String nextValue(ArrayList<String> values, String value)
    {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(value))
                return values.get((i+1)%values.size());
        }
        return null;
    }

    static int id = 0;
    public static int nextID()
    {
        return id++;
    }

    public static boolean equals(double a, double b, double thresh)
    {
        return Math.abs(a-b) < thresh;
    }

    public static double[] toArray(Collection<Double> collection, double[] da)
    {
        if (da == null || da.length < collection.size()) {
            da = new double[collection.size()];
        }
        int i = 0;
        for (Double d: collection) {
            da[i++] = d;
        }

        return da;
    }

    public static int[] toArray(Collection<Integer> collection, int[] ia)
    {
        if (ia == null || ia.length < collection.size()) {
            ia = new int[collection.size()];
        }
        int j = 0;
        for (Integer i: collection) {
            ia[j++] = i;
        }

        return ia;
    }

    static public void printCamera(CameraPosition camera)
    {
        System.out.printf("eye:    [%2.3f, %2.3f, %2.3f]\n"+
                          "lookat: [%2.3f, %2.3f, %2.3f]\n"+
                          "up:     [%2.3f, %2.3f, %2.3f]\n",
                          camera.eye[0], camera.eye[1], camera.eye[2],
                          camera.lookat[0], camera.lookat[1], camera.lookat[2],
                          camera.up[0], camera.up[1], camera.up[2]);
    }

    // Get the ground station ID from the ENV var
    static int groundStationID = -1;
    public static int getGNDID()
    {
        if (groundStationID < 0) {
            String id_str = System.getenv("BASE_STATION_ID");
            if (id_str != null) {
                groundStationID = Integer.parseInt(id_str);
                System.out.println("NFO: groundStationID parsed as: "+groundStationID);
            } else {
                System.err.println("ERR: failed to load 'BASE_STATION_ID'. Exiting.");
                System.exit(-1);
            }
        }

        return groundStationID;
    }

    //Lookup the location of the config file, and load it locally
    public static void loadConfig()
    {
        String path = StringUtil.replaceEnvironmentVariables(configPath);
        // Try to replace with a local version of the file if it exists
        String localPath = StringUtil.replaceEnvironmentVariables("$PROBCOG_CONFIG");
        if (localPath != null && localPath.length() > 0)
            path = localPath;
        File file = new File(path);

        if (file == null)
        {
            System.err.println("ERR: Failed to load config from "+path);
            System.exit(-1);
        }

		System.out.println(localPath);
        try{
            config = new ConfigFile(file);
        }catch(IOException e){}
    }

    //Ensure the existence of a config file
    public static void ensureConfig(){
        if(config == null){
            loadConfig();
            assert(config != null);
        }
    }

    /**
     * Convenience method to get the preloaded Config object
     */
    public static Config getConfig()
    {
        ensureConfig();
        return config;
    }

}
