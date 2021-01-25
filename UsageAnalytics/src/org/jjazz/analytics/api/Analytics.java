/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.analytics.api;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.jjazz.analytics.spi.AnalyticsProcessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import org.jjazz.upgrade.UpgradeManager;
import org.jjazz.upgrade.spi.UpgradeTask;
import org.openide.util.*;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.OnShowing;

/**
 * Feature usage analytics methods.
 * <p>
 * The class acts as a centralized bridge to collect all feature analytics events and pass them to AnalyticsProcessor instances
 * present in the global lookup.
 * <p>
 * Properties/event names examples: "Upgrade" or "New Version"<br>
 * Authorized property value classes: String, Long, Float, Boolean, or a Collection of one these classes.
 * <p>
 */
public class Analytics
{

    public static final String EVENT_ENABLED_CHANGE = "Analytics Enabled";

    private static final String PREF_JJAZZLAB_COMPUTER_ID = "JJazzLabComputerId";
    private static final String PREF_ANALYTICS_ENABLED = "AnalyticsEnabled";
    private static Analytics INSTANCE;
    private final List<AnalyticsProcessor> processors;
    private boolean enabled;
    private static Preferences prefs = NbPreferences.forModule(Analytics.class);
    private static final Logger LOGGER = Logger.getLogger(Analytics.class.getSimpleName());

    public static Analytics getInstance()
    {
        synchronized (Analytics.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new Analytics();
            }
        }
        return INSTANCE;
    }

    private Analytics()
    {
        processors = new ArrayList<>(Lookup.getDefault().lookupAll(AnalyticsProcessor.class));
        enabled = prefs.getBoolean(PREF_ANALYTICS_ENABLED, true);
        if (System.getProperty("jjazzlab.version") == null)
        {
            // By default no analytics if run from IDE, except if "use.analytics" property is set
            String ideAnalytics = System.getProperty("ide.analytics");
            enabled = "true".equalsIgnoreCase(ideAnalytics);
            LOGGER.info("Analytics() Application is run from Netbeans IDE");
            LOGGER.info("Analytics() ide.analytics=" + ideAnalytics);

        }
    }

    public void setEnabled(boolean b)
    {
        if (enabled == b)
        {
            return;
        }

        if (!b)
        {
            logEvent(EVENT_ENABLED_CHANGE, buildMap("Value", b));
        }

        prefs.putBoolean(PREF_ANALYTICS_ENABLED, b);
        enabled = b;

        if (b)
        {
            logEvent(EVENT_ENABLED_CHANGE, buildMap("Value", b));
        }

    }

    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Log a generic event with no properties.
     *
     * @param eventName
     */
    static public void logEvent(String eventName)
    {
        if (getInstance().isEnabled())
        {
            getInstance().processors.forEach(p -> p.logEvent(eventName));
        }
    }

    /**
     * Generic event with properties.
     *
     * @param eventName
     * @param properties Authorized value classes: String, Long, Float, Boolean, or a Collection of one these classes..
     */
    static public void logEvent(String eventName, Map<String, ?> properties)
    {
        if (getInstance().isEnabled())
        {
            checkProperties(properties);
            getInstance().processors.forEach(p -> p.logEvent(eventName, properties));
        }
    }

    /**
     * Update the properties of the current JJazzLab computer.
     * <p>
     *
     * @param properties Authorized value classes: String, Long, Float, Boolean, or a Collection of one these classes..
     * @see Analytics#getJJazzLabComputerId()
     */
    static public void setProperties(Map<String, ?> properties)
    {
        if (getInstance().isEnabled())
        {
            checkProperties(properties);
            getInstance().processors.forEach(p -> p.setProperties(properties));
        }
    }

    /**
     * Update the properties of the current JJazzLab computer only if they are not already set.
     * <p>
     *
     * @param properties Authorized value classes: String, Long, Float, Boolean, or a Collection of one these classes..
     * @see Analytics#getJJazzLabComputerId()
     */
    static public void setPropertiesOnce(Map<String, ?> properties)
    {
        if (getInstance().isEnabled())
        {
            checkProperties(properties);
            getInstance().processors.forEach(p -> p.setPropertiesOnce(properties));
        }
    }

    /**
     * Increment the properties of the current JJazzLab computer by the corresponding Long value.
     *
     * @param properties
     * @see Analytics#getJJazzLabComputerId()
     */
    static public void incrementProperties(Map<String, Long> properties)
    {
        if (getInstance().isEnabled())
        {
            getInstance().processors.forEach(p -> p.incrementProperties(properties));
        }
    }

    static public void incrementProperties(String property, long value)
    {
        if (getInstance().isEnabled())
        {
            HashMap<String, Long> map = new HashMap<>();
            map.put(property, value);
            getInstance().processors.forEach(p -> p.incrementProperties(map));
        }
    }

    static public void incrementProperties(String p1, long v1, String p2, long v2)
    {
        if (getInstance().isEnabled())
        {
            HashMap<String, Long> map = new HashMap<>();
            map.put(p1, v1);
            map.put(p1, v2);
            getInstance().processors.forEach(p -> p.incrementProperties(map));
        }
    }

    /**
     * Helper methods to quickly build a map from specified parameters.
     *
     * @param <T>
     * @param key
     * @param value
     * @return
     */
    static public <T> Map<String, T> buildMap(String key, T value)
    {
        HashMap<String, T> res = new HashMap<>();
        res.put(key, value);
        return res;
    }

    static public Map<String, Object> buildMap(String k1, Object v1, String k2, Object v2)
    {
        HashMap<String, Object> res = new HashMap<>();
        res.put(k1, v1);
        res.put(k2, v2);
        return res;
    }

    static public Map<String, Object> buildMap(String k1, Object v1, String k2, Object v2, String k3, Object v3)
    {
        HashMap<String, Object> res = new HashMap<>();
        res.put(k1, v1);
        res.put(k2, v2);
        res.put(k3, v3);
        return res;
    }

    static public Map<String, Object> buildMap(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4)
    {
        HashMap<String, Object> res = new HashMap<>();
        res.put(k1, v1);
        res.put(k2, v2);
        res.put(k3, v3);
        res.put(k4, v4);
        return res;
    }

    static public Map<String, Object> buildMap(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5)
    {
        HashMap<String, Object> res = new HashMap<>();
        res.put(k1, v1);
        res.put(k2, v2);
        res.put(k3, v3);
        res.put(k4, v4);
        res.put(k5, v5);
        return res;
    }

    static public Map<String, Object> buildMap(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6)
    {
        HashMap<String, Object> res = new HashMap<>();
        res.put(k1, v1);
        res.put(k2, v2);
        res.put(k3, v3);
        res.put(k4, v4);
        res.put(k5, v5);
        res.put(k6, v6);
        return res;
    }

    /**
     * Helper method to get the current date and time as a string in a consistent way, whatever the current locale or time zone.
     * <p>
     * Uses UTC time and ISO format: YYYY-MM-DDTHH:MM:SS
     *
     * @return
     */
    static public String toStdDateTimeString()
    {
        ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("Z"));      // UTC time zone
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(zdt);
    }

    /**
     * Helper method to convert a collection of objects to a list of the corresponding strings.
     *
     * @param c
     * @return
     */
    static public List<String> toStrList(Collection<?> c)
    {
        return c.stream().map(o -> o.toString()).collect(Collectors.toList());
    }

    /**
     * A unique and anonymous id computed when JJazzLab is run for the first time on a given computer.
     * <p>
     * The id is stored as a user preference, so it might be deleted if Netbeans user directory is deleted. If user upgrades to a
     * new version, the id is imported from the previous version settings.
     * <p>
     * Id is calculated from current time in milliseconds + a random number, converted to hexadecimal.
     *
     * @return
     */
    public String getJJazzLabComputerId()
    {
        String id = prefs.get(PREF_JJAZZLAB_COMPUTER_ID, null);
        if (id == null)
        {
            id = Long.toHexString(System.currentTimeMillis() + (long) (Math.random() * 100000));
            prefs.put(PREF_JJAZZLAB_COMPUTER_ID, id);
        }
        return id;
    }

    // =====================================================================================
    // Log the start application event
    // =====================================================================================    
    @OnShowing
    static public class ApplicationStart implements Runnable
    {

        @Override
        public void run()
        {
            // Save OS info
            String name = System.getProperty("os.name", "?");
            String version = System.getProperty("os.version", "?");
            String arch = System.getProperty("os.arch", "?");
            setProperties(buildMap("OS Name", name, "OS Version", version, "OS Arch.", arch));
            setProperties(buildMap("Country", Locale.getDefault().getCountry(), "Language", Locale.getDefault().getLanguage()));


            // Log
            logEvent("Start Application");
            incrementProperties("Nb Start Application", 1);
            setPropertiesOnce(buildMap("First Start Application", toStdDateTimeString()));
        }

    }

    // =====================================================================================
    // Upgrade Task
    // =====================================================================================
    @ServiceProvider(service = UpgradeTask.class)
    static public class RestoreSettingsTask implements UpgradeTask
    {

        @Override
        public void upgrade(String oldVersion)
        {
            // Copy the PREF_JJAZZLAB_COMPUTER_ID preference if present
            UpgradeManager um = UpgradeManager.getInstance();
            um.duplicateOldPreferences(prefs);


            String version = System.getProperty("jjazzlab.version");
            logEvent("Upgrade", buildMap("Old Version", oldVersion, "New Version", (version == null ? "unknown" : version)));
        }

    }
    // =====================================================================================
    // Private methods
    // =====================================================================================

    private static void checkProperties(Map<String, ?> properties)
    {
        for (Object o : properties.values())
        {
            if (!((o instanceof String)
                    || (o instanceof Long)
                    || (o instanceof Float)
                    || (o instanceof Boolean)
                    || (o instanceof Collection)))
            {
                throw new IllegalArgumentException("properties=" + properties);
            }

            if (o instanceof Collection)
            {
                Collection c = (Collection) o;
                for (Object item : c)
                {
                    if (!((item instanceof String)
                            || (item instanceof Long)
                            || (item instanceof Float)
                            || (item instanceof Boolean)))
                    {
                        throw new IllegalArgumentException("properties=" + properties + " invalid collection item=" + item);
                    }
                }
            }

        }
    }

}
