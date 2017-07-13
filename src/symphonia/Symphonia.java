package symphonia;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

/**
 * Class containing implementations of valid commands in Symphonia.
 * 
 * All command implementations must be public methods, returning a String (the
 * feedback given to the user), and taking a single String parameter (the full
 * command statement as entered by the user). The name of the method must
 * exactly match the name of the command as entered by the user.
 * 
 * Methods in this class are searched for using reflection, and are called when
 * the user enters the corresponding command.
 */
public class Symphonia {

    // define usage messages to display if a comand is entered with invalid syntax
    public static final String usage_add = "Usage: add <note name> <duration> [+/-<octave change>] [<dynamic marking>]";
    private final String usage_remove = "Usage: remove <index>|last|all";
    private final String usage_print = "Usage: print <index>|tune";
    private final String usage_replace = "Usage: replace <index>|last <note name> <duration> [+/-<octave change>] [<dynamic marking>]";

    /**
     * The implementation of the 'add' command. This method represents the 'add'
     * command, used to add new notes to the tune.
     * 
     * In the command statement, the user must specify (in this order) the note
     * name and the duration. Additionally, the user may also specify the octave
     * shift (from the middle octave) and the dynamic marking (if not specified,
     * the dynamic of the previous note will be used).
     *
     * Examples (as entered by user into the application):
     * add C 1 -- adds a 'C' note with duration 1 with no octave shift
     * add F# 3 +1 -- adds a 'F#' note with duration 3 shifted one octave higher
     * add Eb 1 ff -- adds a 'Eb' note with duration 1. This note will be fortissimo (very loud)
     * add B 2 -2 mp -- adds a 'B' with duration 2, shifted 2 octaves lower, played mezzo piano (moderately soft)
     */
    String add(String cmdStatement) {
        // parse regex
        Pattern p = Pattern.compile("add ([A-G][#b]*) (\\d[.]*\\d*)\\s?([-+][0-9]+)?\\s?(p+|f+|m[pf])?");
        Matcher m = p.matcher(cmdStatement);
        if (!m.matches()) {
            return "Invalid command.\n" + usage_add + "\n";
        }

        // use parsed data to add new note
        if (nonNullGroupCount(m) < 2 || nonNullGroupCount(m) > 4) {
            return "Invalid command.\n" + usage_add + "\n";
        } else {
            if (!Note.noteToMidiNum.containsKey(m.group(1).replaceAll("#", "").replaceAll("b", ""))) {
                return "Invalid note name.\n";
            }
            
            if (!isAllowedDuration(m.group(2))) {
                return "Invalid duration.\n";
            }
            
            if (nonNullGroupCount(m) == 3 || nonNullGroupCount(m) == 4) {
                int deltaOctave = (m.group(3) != null) ? Integer.parseInt(m.group(3)) : 0;
                if (deltaOctave > 2) {
                    return "Note is too high. Maximum +2 octave change is allowed.\n";
                } else if (deltaOctave < -2) {
                    return "Note is too low. Maximum -2 octave lowering is allowed.\n";
                }
                
                if (m.group(4) != null) {
                    if (Note.dynamicToVelocity.containsKey(m.group(4))) {
                        Main.setDynamic(m.group(4));
                    } else {
                        return "Invalid dynamic.\n";
                    }
                }

                Note n = new Note(m.group(1), Double.parseDouble(m.group(2)), deltaOctave, Main.current_dynamic);
                Main.tune.add(n);
                return "Added note: " + n.getName() + "\n"; // return feedback
            } else {
                Note n = new Note(m.group(1), Double.parseDouble(m.group(2)), 0, Main.current_dynamic);
                Main.tune.add(n);
                return "Added note: " + n.getName() + "\n";
            }
        }
    }

    /**
     * The implementation of the 'remove' command. This method represents the
     * 'remove' command, used to remove a particular note from the tune.
     * 
     * In the command statement, the user must specify a single argument
     * containing the index of the note to be removed (starting from 0). Instead
     * of the index, the user may also enter the word 'last' to remove the last
     * note in the tune, or the word 'all' to clear the entire tune.
     * 
     * Note that these changes cannot be undone.
     * 
     * Examples (as entered by the user):
     * remove 3 -- removes the note at index 3 (counting from 0)
     * remove last -- removes the last note
     */
    String remove(String cmdStatement) {
        // parse regex
        Pattern p = Pattern.compile("remove ([0-9]+|last|all)");
        Matcher m = p.matcher(cmdStatement);
        if (!m.matches()) {
            return usage_remove + "\n";
        }
        
        // use parsed data to remove note(s)
        if (Main.tune.isEmpty()) {
            return "Tune is empty.\n";
        } else if (m.group(1).equals("all")) {
            Main.tune.clear();
            return "Cleared tune.\n";
        } else {
            
            int index = (m.group(1).equals("last")) ? Main.tune.size() - 1 : Integer.parseInt(m.group(1));
            if (index < 0 || index >= Main.tune.size()) {
                return "Index out of bounds.";
            }
            String removed = Main.tune.get(index).getName();
            Main.tune.remove(index);
            return "Removed note: " + removed + "\n";
        }
    }

    /**
     * The implementation of the 'replace' command. This method represents the
     * 'replace' command, used to replace a particular note in the tune with
     * another.
     * 
     * In the command, the user must enter the index (starting from 0)
     * of the note to be replaced (or the word 'last') followed by arguments the
     * user would pass to the 'add' command to add the note to be inserted.
     * 
     * Examples (as entered by the user):
     * replace 4 C# 3 -1: will replace the note with index 4 with a C# with duration 3 lowered by one octave
     * replace last G 1 f: will replace the last note with a G with duration 1 played forte (very loudly)
     */
    String replace(String cmdStatement) {
        // parse regex
        Pattern p = Pattern.compile("replace ([0-9]+|last) ([A-G][#b]*) (\\d[.]*\\d*)\\s?([-+]\\d)?\\s?(p+|f+|m[pf])?");
        Matcher m = p.matcher(cmdStatement);
        if (!m.matches()) {
            return "Invalid command.\n" + usage_replace + "\n";
        }

        // use parsed data to perform the required replacement
        int index;
        if (m.group(1).equals("last")) {
            index = Main.tune.size() - 1;
        } else {
            index = Integer.parseInt(m.group(1));
        }
        if (index >= Main.tune.size() || index < 0) {
            return "Index out of bounds.\n";
        }
        Note replaced = Main.tune.get(index);

        if (nonNullGroupCount(m) < 3 || nonNullGroupCount(m) > 5) {
            return "Invalid command.\n" + usage_replace + "\n";
        } else {
            if (!Note.noteToMidiNum.containsKey(m.group(2).replaceAll("#", "").replaceAll("b", ""))) {
                return "Invalid note name.\n";
            }
            
            if (!isAllowedDuration(m.group(3))) {
                return "Invalid duration.\n";
            }
            
            if (nonNullGroupCount(m) == 4 || nonNullGroupCount(m) == 5) {
                int deltaOctave = (m.group(4) != null) ? Integer.parseInt(m.group(4)) : 0;
                if (deltaOctave > 2) {
                    return "Note is too high. Maximum +2 octave change is allowed.\n";
                } else if (deltaOctave < -2) {
                    return "Note is too low. Maximum -2 octave lowering is allowed.\n";
                }
                
                String dynamicToSet = replaced.dynamic_marking;
                if (m.group(5) != null) {
                    if (Note.dynamicToVelocity.containsKey(m.group(5))) {
                        dynamicToSet = m.group(5);
                        if (m.group(1).equals("last")) {
                            Main.setDynamic(m.group(5));
                        }
                    } else {
                        return "Invalid dynamic.\n";
                    }
                }
                
                Note n = new Note(m.group(2), Double.parseDouble(m.group(3)), deltaOctave, dynamicToSet);
                Main.tune.set(index, n);
                return "Replaced note: " + replaced.getName() + "  with  " + n.getName() + "\n"; // return feedback
            } else {
                if (!Note.noteToMidiNum.containsKey(m.group(2).replaceAll("#", "").replaceAll("b", ""))) {
                    return "Invalid note name.\n";
                }
                if (!isAllowedDuration(m.group(3))) {
                    return "Invalid duration.\n";
                }
                
                Note n = new Note(m.group(2), Double.parseDouble(m.group(3)), 0, replaced.dynamic_marking);
                Main.tune.set(index, n);
                return "Replaced note: " + replaced.getName() + "  with  " + n.getName() + "\n";
            }
        }
    }

    /**
     * The implementation of the 'play' command. This method represents the
     * 'play' command, which synthesizes a piano playing the whole tune using
     * the Java MIDI framework. This command takes no arguments.
     */
    String play(String cmdStatement) throws MidiUnavailableException {
        // initialize audio MIDI synthesis objects
        Synthesizer synth;
        synth = MidiSystem.getSynthesizer();
        synth.open();
        final MidiChannel[] mcs = synth.getChannels();
        Instrument[] instrs = synth.getDefaultSoundbank().getInstruments();
        synth.loadInstrument(instrs[0]);

        // synthesize each note
        if (!Main.tune.isEmpty()) {
            for (Note n : Main.tune) {
                n.play(mcs[0], Main.beat_duration);
            }
            return "Done.\n";
        } else {
            return "Tune is empty.\n";
        }
    }

    /**
     * The implementation of the 'print' command. This method represents the
     * 'print' command, which can be used to print information on any note in
     * the tune, or for all notes in the tune.
     * 
     * In the command statement, the user must specify the index of the required
     * note (starting from 0), or the word 'tune' to print out all notes.
     */
    String print(String cmdStatement) {
        Pattern p = Pattern.compile("print ([0-9]+|tune)");
        Matcher m = p.matcher(cmdStatement);
        if (!m.matches()) {
            return "\n" + usage_print;
        }
        if (Main.tune.isEmpty()) {
            return "\nTune is empty.";
        } else {
            if (m.group(1).equals("tune")) {
                String feedback = "\n";
                for (int i = 0; i < Main.tune.size(); i++) {
                    feedback += i + " -- " + Main.tune.get(i).getName() + "\n";
                }
                return feedback + "\n";
            } else {
                int index = Integer.parseInt(m.group(1));
                if (index < 0 || index >= Main.tune.size()) {
                    return "Index out of bounds.";
                }
                return "\n" + index + " -- " + Main.tune.get(index).getName() + "\n\n";
            }
        }
    }

    private int nonNullGroupCount(Matcher m) {
        // a private helper function to calculate the number of regex groups in a matcher that are not null
        int count = 0;
        for (int i = 1; i <= m.groupCount(); i++) {
            if (m.group(i) != null) {
                count++;
            }
        }
        return count;
    }

    private boolean isAllowedDuration(String d) {
        // private helper function to check if a given double (as a String) is an allowed note duration
        return (Arrays.binarySearch(Note.allowedDurations, Double.parseDouble(d)) >= 0);
    }
}
