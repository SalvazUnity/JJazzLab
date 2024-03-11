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
package org.jjazz.rhythmmusicgeneration.api;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.chordleadsheet.api.item.CLI_Factory;
import org.jjazz.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.chordleadsheet.api.item.VoidAltExtChordSymbol;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_Marker;
import org.jjazz.song.api.Song;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.utilities.api.FloatRange;
import org.jjazz.utilities.api.IntRange;

/**
 * A ChordSequence built for a song, or a part of a song.
 * <p>
 * When constructed a SongChordSequence always has a starting chord symbol.
 */
public class SongChordSequence extends ChordSequence
{

    private final Song song;
    private static final Logger LOGGER = Logger.getLogger(SongChordSequence.class.getSimpleName());

    /**
     * Build a ChordSequence for the specified song, or part of the song.
     * <p>
     * The constructor relies on fillChordSequence() to add the chord symbols, then it makes sure that the created object has a ChordSymbol
     * at beginning.<br>
     *
     * @param song
     * @param barRange If null, use the whole song.
     * @throws IllegalArgumentException     If barRange is not contained in the song.
     * @throws UserErrorGenerationException If no chord found to be the 1st chord of the ChordSequence
     * @see #fillChordSequence(org.jjazz.rhythmmusicgeneration.api.ChordSequence, org.jjazz.song.api.Song, org.jjazz.util.api.IntRange)
     */
    public SongChordSequence(Song song, IntRange barRange) throws UserErrorGenerationException
    {
        super(barRange == null ? song.getSongStructure().getBarRange() : barRange);
        if (barRange == null)
        {
            barRange = song.getSongStructure().getBarRange();
        } else
        {
            checkArgument(song.getSongStructure().getBarRange().contains(barRange), "song=%s barRange=%s", song, barRange);
        }
        this.song = song;
        fillChordSequence(this, song, barRange);


        if (!hasChordAtBeginning())
        {
            // OK if the range starts in the middle of the first section                
            SongPart spt0 = getSongParts().get(0);
            IntRange clsRange = toClsBarRange(spt0);
            if (clsRange.from == 0)
            {
                throw new UserErrorGenerationException("Missing chord symbol at the start of song " + song.getName());
            }
            var prevCliCs = song.getChordLeadSheet().getLastItemBefore(new Position(clsRange.from, 0),
                    false, CLI_ChordSymbol.class,
                    item -> true);
            CLI_ChordSymbol newCs = getInitCopy(prevCliCs);
            add(newCs);
            LOGGER.log(Level.FINE, "fixChordSequence()   lacking a starting chord. Add a copy of previous chord={0}", newCs);
        }
    }

    public Song getSong()
    {
        return song;
    }


    /**
     * Get the SongParts (whole or partially) included in this SongChordSequence.
     *
     * @return
     */
    public List<SongPart> getSongParts()
    {
        return song.getSongStructure().getSongParts().stream()
                .filter(spt -> !getSptBarRange(spt).equals(IntRange.EMPTY_RANGE))
                .toList();
    }

    /**
     * Get the BeatRange of this SongChordSequence.
     *
     * @return
     */
    public FloatRange getBeatRange()
    {
        return song.getSongStructure().toBeatRange(getBarRange());
    }

    /**
     * Get the intersection between the specified SongPart and the bar range of this ChordSequence.
     *
     * @param spt
     * @return
     */
    public IntRange getSptBarRange(SongPart spt)
    {
        return spt.getBarRange().getIntersection(getBarRange());
    }

    /**
     * Return the duration in natural beats of the specified chord.
     * <p>
     * This is the duration until next chord or the end of the SongChordSequence.
     *
     * @param cliCs
     * @param ts         The TimeSignature of the section where chordIndex belongs to (a chord symbol can not span on 2 sections).
     * @return
     */
    public float getChordDuration(CLI_ChordSymbol cliCs, TimeSignature ts)
    {
        Preconditions.checkNotNull(cliCs);
        Preconditions.checkNotNull(ts);

        Position pos = cliCs.getPosition();
        Position nextPos = cliCs == last() ? new Position(getBarRange().to + 1, 0) : higher(cliCs).getPosition();
        float duration = pos.getDuration(nextPos, ts);

        return duration;
    }

    /**
     * Split this SongChordSequence in different SimpleChordSequences for each song contiguous Rhythm's SongParts which have the same
     * specified RhythmParameter value.
     * <p>
     * The resulting SimpleChordSequences will have a starting chord symbol.
     * <p>
     * Example: <br>
     * Spt0 rpValue=Main A-1 chords=Cm7 F7<br>
     * Spt1 rpValue=Main A-1 chords=Bbm7 Eb7<br>
     * Spt2 rpValue=Main B-2 chords=F7M Dm7<br>
     * Then return 1 chordSequence for Main A-1=Spt0+Spt1="Cm7 F7 Bbm7 Eb7", and 1 chordSequence for Main B-2=Spt2=F7M Dm7<br>
     *
     * @param <T> The type of the RhythmParameter value
     * @param r
     * @param rp  The Rhythm's RhythmParameter for which we will check the value
     * @return The list of ChordSequences with their respective common rpValue, sorted by startBar.
     */
    public <T> List<SplitResult<T>> split(Rhythm r, RhythmParameter<T> rp)
    {
        LOGGER.fine("split() --");

        List<SplitResult<T>> res = new ArrayList<>();


        int seqStartBar = getBarRange().from;
        int seqEndBar = seqStartBar;
        T lastRpValue = null;


        for (SongPart spt : getSongParts())
        {
            IntRange sptCsRange = getSptBarRange(spt);
            if (spt.getRhythm() == r)
            {
                // Song part is covered by this ChordSequence and it's our rhythm
                T rpValue = spt.getRPValue(rp);
                if (lastRpValue == null)
                {
                    // Start a new chord sequence
                    seqStartBar = sptCsRange.from;
                    seqEndBar = sptCsRange.to;
                    lastRpValue = rpValue;
                } else if (lastRpValue == rpValue)
                {
                    // Different song parts with same rpValues: we continue the current chord sequence                    
                    seqEndBar += sptCsRange.size();
                } else
                {
                    // Different song parts with different rpValues: complete the chord sequence and start a new one
                    ChordSequence cSeq = subSequence(new IntRange(seqStartBar, seqEndBar), true);
                    SimpleChordSequence sSeq = new SimpleChordSequence(cSeq, r.getTimeSignature());
                    res.add(new SplitResult(sSeq, lastRpValue));
                    seqStartBar = sptCsRange.from;
                    seqEndBar = sptCsRange.to;
                    lastRpValue = rpValue;
                }
            } else
            {
                // Not our rhythm
                if (lastRpValue != null)
                {
                    // We have one chord sequence pending, save it
                    ChordSequence cSeq = subSequence(new IntRange(seqStartBar, seqEndBar), true);
                    SimpleChordSequence sSeq = new SimpleChordSequence(cSeq, r.getTimeSignature());
                    res.add(new SplitResult(sSeq, lastRpValue));
                    lastRpValue = null;
                }
            }
        }

        if (lastRpValue != null)
        {
            // Complete the last chord sequence 
            ChordSequence cSeq = subSequence(new IntRange(seqStartBar, seqEndBar), true);
            SimpleChordSequence sSeq = new SimpleChordSequence(cSeq, r.getTimeSignature());
            res.add(new SplitResult(sSeq, lastRpValue));
        }

        LOGGER.log(Level.FINE, "split()   res={0}", res.toString());
        return res;
    }


    /**
     * Fill a ChordSequence with the chord symbols of the specified song (or part of the song).
     * <p>
     * Use the song's SongStructure and ChordLeadSheet, limited to the specified bar range, to fill the specified ChordSequence. Process the
     * alternate chord symbols when relevant.<br>
     * Example: <br>
     * - ChordLeadSheet: Section B1: bar0=Cm7, bar1=empty Section B2: bar2=Bb bar3=empty<br>
     * - SongStructure: B1 B2 B1 <br>
     * - Range: [bar1; bar5] Method returns: Cm7(bar1), Bb(bar2), Cm7(bar4), empty(bar5)
     *
     * @param cSeq
     * @param song
     * @param barRange If null, use the whole song.
     * @throws IllegalArgumentException If barRange is not contained in the song.
     */
    static public void fillChordSequence(ChordSequence cSeq, Song song, IntRange barRange)
    {
        Preconditions.checkNotNull(cSeq);
        Preconditions.checkNotNull(song);
        final IntRange br = barRange == null ? song.getSongStructure().getBarRange() : barRange;
        checkArgument(song.getSongStructure().getBarRange().contains(br), "song=%s br=%s", song, br);

        ChordLeadSheet cls = song.getChordLeadSheet();
        CLI_Factory clif = CLI_Factory.getDefault();

        // The song parts impacted by the bar range
        var spts = song.getSongStructure().getSongParts().stream()
                .filter(spt -> !spt.getBarRange().getIntersection(br).equals(IntRange.EMPTY_RANGE))
                .toList();

        // Process all SongParts in the range
        for (SongPart spt : spts)
        {
            // The song part range impacted by barRange
            IntRange sptSubRange = spt.getBarRange().getIntersection(br);

            // The corresponding range in the chord leadsheet
            CLI_Section section = spt.getParentSection();
            int sectionStartBar = section.getPosition().getBar();
            IntRange clsSubRange = new IntRange(sectionStartBar + sptSubRange.from - spt.getStartBarIndex(),
                    sectionStartBar + sptSubRange.to - spt.getStartBarIndex());


            // Prepare Marker data
            RP_SYS_Marker rpMarker = RP_SYS_Marker.getMarkerRp(spt.getRhythm());
            String sptMarker = (rpMarker == null) ? null : spt.getRPValue(rpMarker);


            // Process each chord symbol
            for (var cliCs : cls.getItems(clsSubRange.from, clsSubRange.to, CLI_ChordSymbol.class))
            {
                Position pos = cliCs.getPosition();
                ExtChordSymbol ecs = cliCs.getData();
                int absoluteBar = spt.getStartBarIndex() + pos.getBar() - spt.getParentSection().getPosition().getBar();

                // Prepare the ChordSymbol copy to be added
                Position newPos = new Position(absoluteBar, pos.getBeat());
                ExtChordSymbol newEcs = ecs.getChordSymbol(sptMarker);      // Use alternate chord symbol if relevant      

                // Don't allow Void chordsymbol if it's the init chord symbol
                if (newEcs == VoidAltExtChordSymbol.getInstance() && newPos.equals(new Position(0, 0)))
                {
                    LOGGER.log(Level.INFO, "fillChordSequence() Can''t use the void alternate chord symbol of {0} at initial position.",
                            ecs.getName());
                    newEcs = ecs;
                }

                // Add the ChordSymbol to this ChordSequence
                if (newEcs != VoidAltExtChordSymbol.getInstance())
                {
                    CLI_ChordSymbol newCliCs = clif.createChordSymbol(cls, newEcs, newPos);
                    cSeq.add(newCliCs);
                }
            }
        }
    }

    // ====================================================================================
    // Inner classes
    // ====================================================================================

    /**
     * A result of the split method.
     *
     * @param <T>
     * @see #split(org.jjazz.rhythm.api.Rhythm, org.jjazz.rhythm.api.RhythmParameter)
     */
    static public class SplitResult<T>
    {

        public SimpleChordSequence simpleChordSequence;
        public T rpValue;

        public SplitResult(SimpleChordSequence simpleChordSequence, T rpValue)
        {
            this.simpleChordSequence = simpleChordSequence;
            this.rpValue = rpValue;
        }

        @Override
        public String toString()
        {
            return "rpValue=" + rpValue + " simpleCseq=" + simpleChordSequence;
        }
    }

    // ====================================================================================
    // Private methods
    // ====================================================================================
    /**
     * Get the chord leadsheet bar range corresponding to the intersection of the specified song part with this ChordSequence bar range.
     *
     * @param spt
     * @return
     */
    private IntRange toClsBarRange(SongPart spt)
    {
        IntRange sptRange = getSptBarRange(spt);
        CLI_Section section = spt.getParentSection();
        int sectionStartBar = section.getPosition().getBar();
        IntRange r = new IntRange(sectionStartBar + sptRange.from - spt.getStartBarIndex(),
                sectionStartBar + sptRange.to - spt.getStartBarIndex());
        return r;
    }

}
