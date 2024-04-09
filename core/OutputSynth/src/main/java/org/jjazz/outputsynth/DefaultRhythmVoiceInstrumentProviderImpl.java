/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3) 
 * as published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s): 
 *
 */
package org.jjazz.outputsynth;

import org.jjazz.midi.api.Instrument;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.midimix.spi.RhythmVoiceInstrumentProvider;
import org.jjazz.rhythm.api.RhythmVoice;
import org.openide.util.lookup.ServiceProvider;
import org.jjazz.outputsynth.spi.OutputSynthManager;

/**
 * Default implementation of this service provider
 */
@ServiceProvider(service = RhythmVoiceInstrumentProvider.class)
public class DefaultRhythmVoiceInstrumentProviderImpl implements RhythmVoiceInstrumentProvider
{

    @Override
    public String getId()
    {
        return RhythmVoiceInstrumentProvider.DEFAULT_ID;
    }

    @Override
    public Instrument findInstrument(RhythmVoice rv)
    {
        Instrument ins;
        var outSynth = OutputSynthManager.getDefault().getDefaultOutputSynth();

        if ((rv instanceof UserRhythmVoice) && !rv.isDrums())
        {
            ins = outSynth.getUserSettings().getUserMelodicInstrument();
        } else
        {
            ins = outSynth.findInstrument(rv);

        }

        return ins;
    }

}
