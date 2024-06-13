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
package org.jjazz.rpcustomeditorfactoryimpl;

import org.jjazz.phrasetransform.api.rps.RP_SYS_DrumsTransform;
import org.jjazz.rpcustomeditorfactoryimpl.api.RealTimeRpEditorDialog;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.ss_editor.rpviewer.api.RpCustomEditor;
import org.jjazz.ss_editor.rpviewer.spi.RpCustomEditorFactory;
import org.openide.util.lookup.ServiceProvider;

/**
 * A default factory for RpCustomEditors.
 * <p>
 */
@ServiceProvider(service = RpCustomEditorFactory.class)
public class RpCustomEditorFactoryImpl implements RpCustomEditorFactory
{

    @Override
    public boolean isSupported(RhythmParameter<?> rp)
    {
        boolean b = false;
        if (rp instanceof RP_SYS_CustomPhrase)
        {
            b = true;
        } else if (rp instanceof RP_SYS_DrumsTransform)
        {
            b = true;
        }
        return b;
    }

    @Override
    public <E> RpCustomEditor<E> getEditor(RhythmParameter<E> rp)
    {
        RpCustomEditor res = null;


        if (rp instanceof RP_SYS_CustomPhrase rpCustom)
        {
//            var editor = new RP_SYS_CustomPhraseComp2(rpCustom);
//            res = new RealTimeRpEditorDialog(editor);
            res = new RP_SYS_CustomPhraseEditor(rpCustom);

        } else if (rp instanceof RP_SYS_DrumsTransform rpDrums)
        {
            var editor = new RP_SYS_DrumsTransformComp(rpDrums);
            res = new RealTimeRpEditorDialog(editor);
        }

        return res;
    }


}
