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
package org.jjazz.midi.api.synths;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import javax.sound.midi.MidiMessage;
import org.jjazz.midi.api.InstrumentBank;

/**
 * A special "void" instrument: no bank change or program change is associated to this instrument.
 * <p>
 * When used, the system should not send any Midi bank select or program change messages for this instrument.
 */
public class VoidInstrument extends GM1Instrument implements Serializable
{

    private static VoidInstrument INSTANCE;
    private InstrumentBank<?> myBank;

    /**
     * Should be only called via a NotSetBank: this way the bank/synth are correctly set.
     *
     * @return
     */
    static protected VoidInstrument getInstance()
    {
        synchronized (VoidInstrument.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new VoidInstrument(0, "!Not Set!");
            }
        }
        return INSTANCE;
    }

    private VoidInstrument(int programChange, String patchName)
    {
        super(programChange, patchName, Family.Piano);
    }

    @Override
    public void setBank(InstrumentBank<?> bank)
    {
        this.myBank = bank;
    }

    @Override
    public InstrumentBank<?> getBank()
    {
        return this.myBank;
    }

    @Override
    public String getFullName()
    {
        return "Void Instrument";
    }

    /**
     * Overridden : return an empty array.
     *
     * @return
     */
    @Override
    public MidiMessage[] getMidiMessages(int channel)
    {
        return new MidiMessage[0];
    }

    /* ---------------------------------------------------------------------
     * Serialization
     * --------------------------------------------------------------------- */
    private Object writeReplace()
    {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream stream)
            throws InvalidObjectException
    {
        throw new InvalidObjectException("Serialization proxy required");
    }

    /**
     * Special serialization process to not use the default GM1Instrument one.
     */
    private static class SerializationProxy implements Serializable
    {

        private static final long serialVersionUID = -82099017429816L;
        private int spVERSION = 1;      // Do not make final!
        int spProgChange;

        private SerializationProxy(VoidInstrument ins)
        {
            spProgChange = ins.getMidiAddress().getProgramChange();       // Just to save something but useless...
        }

        private Object readResolve() throws ObjectStreamException
        {
            return NotSetBank.getInstance().getVoidInstrument();
        }
    }

}
