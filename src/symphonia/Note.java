package symphonia;

import java.util.HashMap;
import javax.sound.midi.MidiChannel;

/**
 * A user-defined type to represent a musical Note. This is the core data
 * structure of the application, the main tune being an ArrayList of Notes.
 *
 * Each note has a note name (musical name), a duration (the number of beats it
 * should be played for), a dynamic marking (how loud it should be played;
 * represented in standard musical terms), and a variable for octave changes
 * (shifting the note a few octaves higher or lower). It also contains some
 * constant class variables used by the Main and Symphonia classes.
 */
public class Note {

    public static final double[] allowedDurations = {0.25, 0.5, 1, 1.5, 2, 3, 4};
    
    // a map from a musical note name to standard MIDI numbers
    public static final HashMap<String, Integer> noteToMidiNum;
    
    // a map from musical dynamic markings to velocities used for MIDI synthesis
    public static final HashMap<String, Integer> dynamicToVelocity;

    static {
        // initialize static variables
        noteToMidiNum = new HashMap<>();
        dynamicToVelocity = new HashMap<>();
        
        noteToMidiNum.put("C", 60);
        noteToMidiNum.put("D", 62);
        noteToMidiNum.put("E", 64);
        noteToMidiNum.put("F", 65);
        noteToMidiNum.put("G", 67);
        noteToMidiNum.put("A", 69);
        noteToMidiNum.put("B", 71);

        dynamicToVelocity.put("pppp", 8); // pianissississimo : very very very soft
        dynamicToVelocity.put("ppp", 20); // pianississimo : very very soft
        dynamicToVelocity.put("pp", 31); // pianissimo : very soft
        dynamicToVelocity.put("p", 42); // piano : soft
        dynamicToVelocity.put("mp", 53); // mezzo-piano : moderately soft
        dynamicToVelocity.put("mf", 64); // mezzo-forte : moderately loud
        dynamicToVelocity.put("f", 80); // forte : loud
        dynamicToVelocity.put("ff", 96); // fortissimo : very loud
        dynamicToVelocity.put("fff", 112); // fortississimo : very very loud
        dynamicToVelocity.put("ffff", 127); // fortissississimo : very very very loud
    }

    // the difference in MIDI numbers of the same note in two consecutive octaves
    private final int OCTAVE_INTERVAL = 12;

    String noteName;
    int deltaOctave;
    int midiNum;
    double duration;
    String dynamic_marking;
    int velocity;

    /**
     * Parameterized constructor.
     * 
     * @param noteName        musical name of the note
     * @param duration        the duration (in number of beats) of the note
     * @param deltaOctave     the number of octaves above/below the middle octave
     * @param dynamic_marking the dynamic of the note
     */
    public Note(String noteName, double duration, int deltaOctave, String dynamic_marking) {
        this.noteName = noteName;
        this.deltaOctave = deltaOctave;
        
        // calculate MIDI number
        this.midiNum = noteToMidiNum.get(Character.toString(noteName.charAt(0))) + (deltaOctave * OCTAVE_INTERVAL);
        if (noteName.contains("#")) {
            this.midiNum++;
        } else if (noteName.contains("b")) {
            this.midiNum--;
        }
        
        this.duration = duration;
        this.dynamic_marking = dynamic_marking;
        this.velocity = dynamicToVelocity.get(dynamic_marking);
    }

    /**
     * Play the Note. Play the Note using a given MidiChannel and beat duration.
     * 
     * @param mc                the MidiChannel to play the note on
     * @param crotchet_duration the duration (in seconds) of one beat
     */
    void play(MidiChannel mc, double crotchet_duration) {
        long time_to_play_millis = (long) (crotchet_duration * this.duration * 1000);
        mc.noteOn(this.midiNum, this.velocity);
        try {
            Thread.sleep(time_to_play_millis);
        } catch (InterruptedException ex) {
        }
        mc.noteOff(this.midiNum, this.velocity);
    }

    /**
     * Calculate number of lines and spaces between the Note and middle C.
     * 
     * @return the number of lines and spaces between the Note and middle C
     */
    int staffDistFromMidC() {
        char[] order = {'C', 'D', 'E', 'F', 'G', 'A', 'B'};
        int dist = 0;
        for (int i = 0; i < 7; i++) {
            if (Character.toString(order[i]).equals(noteName.replaceAll("#", "").replaceAll("b", ""))) {
                dist = i;
                break;
            }
        }
        dist += 7 * deltaOctave;
        return dist;
    }

    /**
     * Get Note data as a String.
     * 
     * @return a String representation of the Note
     */
    String getName() {
        return noteName + " duration=" + duration + " octave change=" + deltaOctave + " dynamic=" + dynamic_marking;
    }

}
