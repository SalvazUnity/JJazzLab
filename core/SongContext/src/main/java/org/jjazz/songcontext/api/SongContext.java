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
package org.jjazz.songcontext.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jjazz.harmony.api.Position;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.LongRange;


/**
 * Collect various data about a Song context in order to facilitate music generation.
 * <p>
 * Note that a SongContext instance should be discarded if song is structurally modified, because some SongContext methods may return data
 * not consistent anymore with the actual Song.
 */
public class SongContext
{

    private Song song;
    private MidiMix midiMix;
    private IntRange barRange;
    protected List<SongPart> songParts;
    private FloatRange beatRange;
    private LongRange tickRange;


    /**
     * Create a SongContext object for the whole song.
     *
     * @param s
     * @param mm
     */
    public SongContext(Song s, MidiMix mm)
    {
        this(s, mm, null);
    }

    /**
     * Create a SongContext which reuse sgContext's Song and MidiMix, but with the specified range.
     *
     * @param sgContext
     * @param newRange
     */
    public SongContext(SongContext sgContext, IntRange newRange)
    {
        this(sgContext.getSong(), sgContext.getMidiMix(), newRange);
    }

    /**
     * Create a SongContext object for whole or part of a song.
     *
     * @param s
     * @param mm
     * @param bars If null, the range will represent the whole song from first to last bar.
     */
    public SongContext(Song s, MidiMix mm, IntRange bars)
    {
        if (s == null || mm == null)
        {
            throw new IllegalArgumentException("s=" + s + " mix=" + mm + "barRg=" + bars);
        }
        song = s;
        this.midiMix = mm;
        int sizeInBars = s.getSongStructure().getSizeInBars();
        if (sizeInBars == 0)
        {
            this.barRange = IntRange.EMPTY_RANGE;
        } else if (bars == null)
        {
            this.barRange = new IntRange(0, sizeInBars - 1);
        } else if (bars.to > sizeInBars - 1)
        {
            throw new IllegalArgumentException("s=" + s + " sizeInBars=" + sizeInBars + " mix=" + mm + " bars=" + bars);
        } else
        {
            this.barRange = bars;
        }
        songParts = song.getSongStructure().getSongParts().stream()
                .filter(spt -> contains(spt))
                .toList();
        beatRange = song.getSongStructure().toBeatRange(barRange);
        tickRange = new LongRange((long) (beatRange.from * MidiConst.PPQ_RESOLUTION), (long) (beatRange.to * MidiConst.PPQ_RESOLUTION));

    }


    /**
     * Clone the SongContext reusing the same song and midi mix.
     *
     * @return
     */
    @Override
    public SongContext clone()
    {
        return new SongContext(this, getBarRange());
    }

    /**
     * Deep clone the SongContext : make a copy of the song and the midimix.
     *
     * @param register If true the created song is registered by the SongFactory
     * @return
     * @see SongContextCopy &nbsp; (if you need to keep a reference to the original song and midiMix)
     */
    public SongContext deepClone(boolean register)
    {        
        SongFactory sf = SongFactory.getInstance();
        Song songCopy = sf.getCopy(song, register);
        MidiMix mixCopy = midiMix.getDeepCopy();
        return new SongContext(songCopy, mixCopy, barRange);
    }

    /**
     * Music should be produced for this song.
     *
     * @return
     */
    public Song getSong()
    {
        return song;
    }

    /**
     * Music should be produced for this MidiMix.
     *
     * @return
     */
    public MidiMix getMidiMix()
    {
        return midiMix;
    }

    /**
     * Music should be produced only for this range of bars.
     * <p>
     * The range can start/end anywhere in the song (including in the middle of a song part).
     *
     * @return
     */
    public IntRange getBarRange()
    {
        return barRange;
    }

    /**
     * The range (computed at the time of this object creation) for which music should be produced.
     * <p>
     * The range can start/end anywhere in the song (including in the middle of a song part). If getBarRange().from &gt; 0 then
 toBeatRange().from is also &gt; 0.
     *
     * @return
     */
    public FloatRange getBeatRange()
    {
        return beatRange;
    }

    /**
     * The tick range (computed at the time of this object creation) corresponding to getBarRange() or getBeatRange().
     * <p>
 The range can start/end anywhere in the song (including in the middle of a song part). If toBeatRange().from &gt; 0 then
     * getTickRange().from is also &gt; 0.
     *
     * @return
     */
    public LongRange getTickRange()
    {
        return tickRange;
    }

    /**
     * Get all the song parts (at the time of this object creation) which are contained in this context.
     * <p>
     *
     * @return Can be empty.
     * @see #contains(org.jjazz.songstructure.api.SongPart)
     */
    public List<SongPart> getSongParts()
    {
        return songParts;
    }

    /**
     * Get the intersection between the song part bar range and this SongContext bar range.
     * <p>
     *
     * @param spt
     * @return Can be the EMPTY_RANGE if spt is not part of this context.
     */
    public IntRange getSptBarRange(SongPart spt)
    {
        return spt.getBarRange().getIntersection(barRange);
    }

    /**
     * Get the intersection between the song part beat range and this SongContext beat range.
     * <p>
     *
     * @param spt
     * @return
     */
    public FloatRange getSptBeatRange(SongPart spt)
    {
        FloatRange sptRange = song.getSongStructure().toBeatRange(spt.getBarRange());
        return sptRange.getIntersectRange(getBeatRange());
    }

    /**
     * Get the range of ticks of spt belonging to this context.
     *
     * @param spt
     * @return
     */
    public LongRange getSptTickRange(SongPart spt)
    {
        FloatRange sptRange = getSptBeatRange(spt);
        LongRange lr = new LongRange((long) (sptRange.from * MidiConst.PPQ_RESOLUTION), (long) (sptRange.to * MidiConst.PPQ_RESOLUTION));
        return lr;
    }

    /**
     * Check if the specified spt has at least one bar in the range of this context.
     *
     * @param spt
     * @return
     */
    public boolean contains(SongPart spt)
    {
        return barRange.isIntersecting(spt.getBarRange());
    }

    /**
     * Get the list of unique rhythms used in this context.
     *
     * @return
     */
    public List<Rhythm> getUniqueRhythms()
    {
        ArrayList<Rhythm> res = new ArrayList<>();
        for (SongPart spt : song.getSongStructure().getSongParts())
        {
            if (contains(spt) && !res.contains(spt.getRhythm()))
            {
                res.add(spt.getRhythm());
            }
        }
        return res;
    }

    /**
     * Get the list of unique rhythm voices used in this context.
     *
     * @return
     */
    public List<RhythmVoice> getUniqueRhythmVoices()
    {
        ArrayList<RhythmVoice> rvs = new ArrayList<>();
        for (Rhythm r : getUniqueRhythms())
        {
            rvs.addAll(r.getRhythmVoices());
        }
        return rvs;
    }

    /**
     * Converts a tick position relative to this context into an absolute SongStructure Position.
     *
     * @param relativeTick 0 for the start of this context bar range.
     * @return Null if tick is out of the bounds of this context.
     */
    public Position toPosition(long relativeTick)
    {
        FloatRange br = getBeatRange();
        float absPosInBeats = toPositionInBeats(relativeTick);
        if (!br.contains(absPosInBeats, true))
        {
            return null;
        }
        return song.getSongStructure().toPosition(absPosInBeats);
    }

    /**
     * Converts a tick position relative to this context into an absolute SongStructure position in beats.
     *
     * @param relativeTick 0 for the start of this context bar range.
     * @return -1 if tick is out of the bounds of this context.
     */
    public float toPositionInBeats(long relativeTick)
    {
        FloatRange br = getBeatRange();
        float res = br.from + (float) relativeTick / MidiConst.PPQ_RESOLUTION;
        if (!br.contains(res, true))
        {
            res = -1;
        }
        return res;
    }


    /**
     * Converts a tick position relative to this context into a ChordLeadSheet Position.
     *
     * @param relativeTick
     * @return Null if tick is out of the bounds of this context.
     */
    public Position toClsPosition(long relativeTick)
    {
        Position ssPos = toPosition(relativeTick);
        if (ssPos == null)
        {
            return null;
        }
        SongPart spt = song.getSongStructure().getSongPart(ssPos.getBar());
        int sectionBar = spt.getParentSection().getPosition().getBar();
        return new Position(ssPos.getBar() - spt.getStartBarIndex() + sectionBar, ssPos.getBeat());
    }

    /**
     * Compute the tick relative to this context for the given absolute position expressed in bar/beat.
     * <p>
     *
     * @param pos
     * @return -1 if pos is outside this context range. Returns 0 for the first bar/beat of the context range.
     */
    public long toRelativeTick(Position pos)
    {
        if (pos == null)
        {
            throw new NullPointerException("pos");
        }
        long tick = -1;
        int bar = pos.getBar();
        float beat = pos.getBeat();
        if (barRange.contains(bar))
        {
            SongPart spt = song.getSongStructure().getSongPart(bar);
            LongRange sptTickRange = getSptTickRange(spt);
            IntRange sptBarRange = getSptBarRange(spt);
            int relativeBar = bar - sptBarRange.from;
            float relativeNbBeats = (relativeBar * spt.getRhythm().getTimeSignature().getNbNaturalBeats()) + beat;
            tick = sptTickRange.from + (long) (relativeNbBeats * MidiConst.PPQ_RESOLUTION);
            tick -= getTickRange().from;
        }
        return tick;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.song);
        hash = 97 * hash + Objects.hashCode(this.midiMix);
        hash = 97 * hash + Objects.hashCode(this.barRange);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final SongContext other = (SongContext) obj;
        if (!Objects.equals(this.song, other.song))
        {
            return false;
        }
        if (!Objects.equals(this.midiMix, other.midiMix))
        {
            return false;
        }
        if (!Objects.equals(this.barRange, other.barRange))
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "SongContext[song=" + song.getName() + ", midiMix=" + midiMix + ", range=" + barRange + "]";
    }


    // ============================================================================================
    // Private methods
    // ============================================================================================   
}
