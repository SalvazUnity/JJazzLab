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
package org.jjazz.embeddedsynth;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.Lame;
import net.sourceforge.lame.mp3.MPEGMode;
import org.jjazz.embeddedsynth.api.EmbeddedSynthException;
import org.jjazz.embeddedsynth.api.Mp3Encoder;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.utilities.api.Utilities;
import org.netbeans.api.progress.BaseProgressUtils;

/**
 * A mp3 encoder which relies on net.sourceforge.lame-3.98.4.jar found here
 * https://github.com/nwaldispuehl/java-lame
 */
public class JavaLameMp3Encoder implements Mp3Encoder
{

    private static final int MP3_GOOD_QUALITY_BITRATE = 320;
    private static final int MP3_LOW_QUALITY_BITRATE = 128;

    @Override
    public void encode(File inFile, File mp3File, boolean lowQuality, boolean useVariableEncoding) throws EmbeddedSynthException
    {
        if (!Utilities.getExtension(inFile.getName()).equalsIgnoreCase("wav"))
        {
            throw new EmbeddedSynthException("File format not supported: " + inFile.getName());
        }

        class ProcessTask implements Runnable
        {

            private EmbeddedSynthException exception = null;

            @Override
            public void run()
            {
                try
                {
                    wavToMp3(inFile, mp3File, lowQuality ? MP3_LOW_QUALITY_BITRATE : MP3_GOOD_QUALITY_BITRATE, useVariableEncoding);

                } catch (IOException | UnsupportedAudioFileException ex)
                {
                    exception = new EmbeddedSynthException(exception.getMessage());
                }
            }
        };
        ProcessTask task = new ProcessTask();
        BaseProgressUtils.showProgressDialogAndRun(task, ResUtil.getString(getClass(), "GeneratingMp3File", mp3File.getAbsolutePath()));

        if (task.exception != null)
        {
            throw task.exception;
        }

    }

    // ============================================================================================================
    // Private methods
    // ============================================================================================================
    /**
     * Convert a WAV file to a MP3 file.
     *
     * @param wavFile
     * @param mp3File
     * @param bitRate E.g 128 for medium quality, 320 for high quality
     * @param useVariableEncoding
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    private void wavToMp3(File wavFile, File mp3File, int bitRate, boolean useVariableEncoding) throws IOException, UnsupportedAudioFileException
    {
        var is = new FileInputStream(wavFile);
        AudioInputStream audioIs = AudioSystem.getAudioInputStream(new BufferedInputStream(is));        // BufferedInputStream needed to add mark/reset support
        byte[] mp3Bytes = encodeToMp3(audioIs, bitRate, useVariableEncoding);
        new FileOutputStream(mp3File).write(mp3Bytes);
    }

    /**
     * Do the stream encoding using lame
     */
    private byte[] encodeToMp3(AudioInputStream audioInputStream, int bitRate, boolean useVariableEncoding) throws IOException
    {
        LameEncoder encoder = new LameEncoder(audioInputStream.getFormat(), bitRate, MPEGMode.STEREO, Lame.QUALITY_HIGHEST, useVariableEncoding);

        ByteArrayOutputStream mp3 = new ByteArrayOutputStream();
        byte[] inputBuffer = new byte[encoder.getPCMBufferSize()];
        byte[] outputBuffer = new byte[encoder.getPCMBufferSize()];

        int bytesRead;
        int bytesWritten;

        while (0 < (bytesRead = audioInputStream.read(inputBuffer)))
        {
            bytesWritten = encoder.encodeBuffer(inputBuffer, 0, bytesRead, outputBuffer);
            mp3.write(outputBuffer, 0, bytesWritten);
        }

        encoder.close();
        return mp3.toByteArray();
    }
}
