# JJazzLab-X

JJazzLab-X is a Midi-based framework dedicated to automatic backing tracks generation -some people prefer to say “auto-accompaniment applications”. 

JJazzLab-X is used to develop desktop applications similar to (and better than!) Band In A Box, Impro-visor, ChordPulse, iReal Pro, etc. 

## Example

The JJazzLab application is built upon JJazzLab-X : download it at [www.jjazzlab.com](https://www.jjazzlab.com) and try it to see the JJazzLab-X capabilities. Or you can check out the [JJazzLab YouTube channel](https://www.youtube.com/channel/UC0L3SwjY6bhTj6jsbOYzzAw).

## Develop your own music generation engine without hassle

Thanks to JJazzLab-X developers can save a huge amount of work by only focusing on their music generation engine. Out of the box, the JJazzLab-X framework provides all the infrastructure, all the “plumbing” that, before, every developer had to write themselves. 

JJazzLab-X can host any number of music generation engines as plugins. What happens when you load a song file and press the Play button?

1. The framework shows the song in the editors
2. The framework sends Midi messages to initialize the connected Midi sound device
3. When user press Play, the framework sends the song data to the music generation engine
4. The music engine uses the song data to generate the Midi data for the backing tracks
5. The framework retrieves the Midi data and plays it

## Based on the Apache Netbeans Platform 

JJazzLab-X is based on the [Netbeans Platform](https://netbeans.org/features/platform/features.html) (now hosted by the Apache foundation). It provides a reliable and extensible application architecture.

The Netbeans Platform turns JJazzLab-X in a pluggable application where plugins can be installed or deactivated at runtime. Plugins can easily add/alter functionalities and insert UI elements such as menu items.

## Installation

The current version is an Ant-based Netbeans IDE project (Netbeans 11, JDK>=8).

From Netbeans IDE:
- menu Team/Git/Clone, enter repository address: https://github.com/jjazzboss/JJazzLabWebSite.git
- let Netbeans create a new project from the cloned files
- select the created project, righ-click Build (or Run)

**Note**: JJazzLab-X only embeds a very basic music generation for debugging purpose.

## Contribution

Contributions welcome !

## License

Lesser GPL v3 (LGPL v3), see LICENCE file.

## Documentation 
See GitHub's Wiki.
