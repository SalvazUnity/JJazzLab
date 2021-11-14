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
package org.jjazz.ui.spteditor.spi;

import org.jjazz.ui.spteditor.SptEditorFactoryImpl;
import org.jjazz.ui.spteditor.api.SptEditor;
import org.jjazz.ui.spteditor.api.SptEditorSettings;
import org.openide.util.Lookup;

public interface SptEditorFactory
{

    public static SptEditorFactory getDefault()
    {
        SptEditorFactory rlef = Lookup.getDefault().lookup(SptEditorFactory.class);
        if (rlef == null)
        {
            rlef = SptEditorFactoryImpl.getInstance();
        }
        return rlef;
    }

    default DefaultRpEditorComponentFactory getDefaultRpEditorFactory()
    {
        return DefaultRpEditorComponentFactory.getDefault();
    }

    default SptEditorSettings getDefaultSptEditorSettings()
    {
        return SptEditorSettings.getDefault();
    }

    SptEditor createEditor(SptEditorSettings settings, DefaultRpEditorComponentFactory factory);
}
