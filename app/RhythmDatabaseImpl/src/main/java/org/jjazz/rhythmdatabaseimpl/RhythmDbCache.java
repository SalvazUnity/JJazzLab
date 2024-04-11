/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3)
 *  as published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 *  Contributor(s):
 */
package org.jjazz.rhythmdatabaseimpl;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythm.spi.RhythmProvider;

/**
 * Contains the serializable cached data of the RhythmDatabase.
 * <p>
 */
public class RhythmDbCache implements Serializable
{

    private transient static final long serialVersionUID = 2922229276100L;
    private transient static final String DB_CACHE_FILE = "RhythmDbCache.dat";

    private Map<String, List<RhythmInfo>> data;
    private transient static final Logger LOGGER = Logger.getLogger(RhythmDbCache.class.getSimpleName());

    /**
     * Create the cache object.
     * <p>
     * Cache will contain only file-based RhythmInfo instances and no AdaptedRhythms.
     *
     * @param map
     */
    public RhythmDbCache(Map<RhythmProvider, List<RhythmInfo>> map)
    {
        data = new HashMap<>();
        
        // Copy data : just change RhythmProvider by its id
        for (RhythmProvider rp : map.keySet())
        {
            var rhythms =  new ArrayList<>(map.get(rp)
                    .stream()
                    .filter(ri -> !ri.file().getName().equals("") && !ri.isAdaptedRhythm())
                    .toList());      // returned object by toList() might not be serializable     
            if (!rhythms.isEmpty())
            {
                data.put(rp.getInfo().getUniqueId(), rhythms);
            }
        }
    }

    /**
     * The cached data.
     * <p>
     * Cache data is used only for file-based rhythms.
     *
     * @return RhyhtmProviderId strings are used as map keys.
     */
    public Map<String, List<RhythmInfo>> getData()
    {
        return data;
    }

    public void dump()
    {
        LOGGER.info("dump():");   
        for (String rpId : data.keySet())
        {
            var rhythms = data.get(rpId);
            LOGGER.log(Level.INFO, "- {0}: total={1}", new Object[]{rpId, rhythms.size()});   
        }
    }

    /**
     * The number of RhythmInfo instances.
     *
     * @return
     */
    public int getSize()
    {
        int n = 0;
        for (String rpId : this.data.keySet())
        {
            n += data.get(rpId).size();
        }
        return n;
    }


    static public File getFile()
    {
        var fdm = FileDirectoryManager.getInstance();
        File dir = fdm.getAppConfigDirectory(null);
        assert dir != null;   
        return new File(dir, DB_CACHE_FILE);
    }

    // =========================================================================
    // Private methods
    // =========================================================================   

}
