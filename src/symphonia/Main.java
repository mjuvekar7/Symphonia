package symphonia;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import javax.sound.midi.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * The main executing class.
 */
public class Main {

    public static final ArrayList<Note> tune = new ArrayList<>();

    public static double beat_duration;
    public static String current_dynamic;

    private static boolean isAddmode = false;
    private static boolean isFull = false;

    static final int height = Toolkit.getDefaultToolkit().getScreenSize().height - 50;
    static final int width = Toolkit.getDefaultToolkit().getScreenSize().width - 50;

    /**
     * The main executing method.
     * Initializes static variables and instantiates GUI.
     * 
     * @param args Command line arguments. None expected.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getValues();
                createAndShowGUI();
            }
        });
    }

    private static void getValues() {
        // get duration of one beat and store it in "beat_duration"
        String input;
        while (true) {
            input = JOptionPane.showInputDialog("Enter duration of one beat (in seconds)");
            try {
                beat_duration = Double.parseDouble(input);
                if (beat_duration <= 0) {
                    // the duration of a beat cannot be less than or equal to 0
                    JOptionPane.showMessageDialog(null, "Invalid input. Please enter a positive number.");
                } else {
                    break;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid input. Please enter a number.");
            }
        }

        // display a drop-down menu to get the starting dynamic
        Object[] options = {"extremely soft (pianississimo)", "very soft (pianissimo)", "soft (piano)", "medium soft (mezzo piano)", "medium loud (mezzo forte)", "loud (forte)", "very loud (fortissimo)", "extremely loud (fortississimo)"};
        String in = (String) JOptionPane.showInputDialog(null, "How loud do you want your music to start?", "Choose Initial Dynamic (Loudness)", JOptionPane.QUESTION_MESSAGE, null, options, options[4]);
        if (in.equals(options[0])) {
            current_dynamic = "ppp";
        } else if (in.equals(options[1])) {
            current_dynamic = "pp";
        } else if (in.equals(options[2])) {
            current_dynamic = "p";
        } else if (in.equals(options[3])) {
            current_dynamic = "mp";
        } else if (in.equals(options[4])) {
            current_dynamic = "mf";
        } else if (in.equals(options[5])) {
            current_dynamic = "f";
        } else if (in.equals(options[6])) {
            current_dynamic = "ff";
        } else if (in.equals(options[7])) {
            current_dynamic = "fff";
        }
    }

    private static void createAndShowGUI() {
        // initialize main frame
        final JFrame main_frame = new JFrame("Symphonia");
        main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        main_frame.setLayout(new BorderLayout());

        // initialize main and input panels
        final MainPanel p = new MainPanel();
        JPanel panel_bottom = new JPanel();
        panel_bottom.setPreferredSize(new Dimension(width, 40));

        main_frame.setPreferredSize(new Dimension(width, height));

        // initialize feedpck panel
        final JPanel feedback_panel = new JPanel();

        // initialize feedback text area (not editable)
        final JTextArea feedback = new JTextArea();
        feedback.setEditable(false);

        // initialize a scroll pane for the feedback area
        JScrollPane feedbackArea = new JScrollPane(feedback);
        feedbackArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // add the feedback area to the panel and make the panel visible
        feedback_panel.add(feedbackArea);
        feedbackArea.setPreferredSize(new Dimension(width, 180));
        feedback_panel.setVisible(true);

        // set preferred size of main panel
        p.setPreferredSize(new Dimension(width, height - feedback_panel.getPreferredSize().height - panel_bottom.getPreferredSize().height));

        // initialize menu bar
        JMenuBar mb = new JMenuBar();

        // create "File" menu
        JMenu fileMenu = new JMenu("File");

        // create "File" menu items
        final JMenuItem importFile = new JMenuItem("Import command file");
        final JMenuItem exportFile = new JMenuItem("Export tune");
        final JMenuItem toggleAddMode = new JMenuItem("Add mode on");

        toggleAddMode.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // toggle add mode and set the text of the menu item accordingly
                if (!isAddmode) {
                    isAddmode = true;
                    feedback.append("Switched to add mode.\n");
                    ((JMenuItem) e.getSource()).setText("Add mode off");
                } else {
                    isAddmode = false;
                    feedback.append("Add mode is now off.");
                    ((JMenuItem) e.getSource()).setText("Add mode on");
                }
            }
        });

        ActionListener importAction = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // import a command file (file containing commands) from the file system
                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File(System.getProperty("user.home")));
                
                // get file from the file system
                int result = fc.showOpenDialog(main_frame);
                feedback.append("\nSelect file to import.\n");
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fc.getSelectedFile();

                    if (!selectedFile.getAbsolutePath().endsWith(".txt")) {
                        feedback.append("Invalid File: Command files have to have the .txt extension.");
                        return;
                    }

                    // parse input file
                    try {
                        Scanner scan = new Scanner(selectedFile);
                        if (!scan.hasNextLine()) {
                            feedback.append("File is empty.\n");
                            return;
                        }
                        // all command files MUST start with the line "Symphonia Command File"
                        if (!scan.nextLine().equalsIgnoreCase("Symphonia Command File")) {
                            feedback.append("Invalid File: All command files must start with \"Symphonia Command File\".\n");
                            return;
                        }

                        feedback.append("\nImporting Command File: " + selectedFile.getName() + "...\n");

                        while (scan.hasNextLine()) {
                            String line = scan.nextLine();

                            if (line.equals("")) {
                                continue;
                            }

                            String command;
                            Class cmdClass = Symphonia.class;
                            Class[] paramTypes = {String.class};
                            Symphonia mm = new Symphonia();
                            Method m;

                            command = (line.contains(" ")) ? line.substring(0, line.indexOf(" ")) : line;

                            if (!command.equals("exit")) {
                                if (line.equals("addmode on")) {
                                    if (!isAddmode) {
                                        isAddmode = true;
                                        feedback.append("Switched to add mode.\n");
                                    }
                                } else if (line.equals("addmode off")) {
                                    if (isAddmode) {
                                        isAddmode = false;
                                        feedback.append("Add mode is now off.\n");
                                    }
                                } else if (isAddmode) {
                                    try {
                                        m = cmdClass.getDeclaredMethod("add", paramTypes);
                                        String returnVal = (String) m.invoke(mm, "add " + line);
                                        feedback.append(returnVal);
                                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                        ex.printStackTrace();
                                    }
                                } else {
                                    try {
                                        m = cmdClass.getDeclaredMethod(command, paramTypes);
                                        String returnVal = (String) m.invoke(mm, line);
                                        feedback.append(returnVal);
                                    } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                                        feedback.append(command + " - No such command (yet).\n");
                                        return;
                                    }
                                }
                            } else {
                                feedback.append("Cannot exit from a command file.");
                            }
                        }

                        main_frame.repaint();
                        feedback.append("\nFile imported succesfully.\n\n");
                    } catch (FileNotFoundException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };

        ActionListener exportAction = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // export current tune as a command file
                JFileChooser fc = new JFileChooser(new File(System.getProperty("user.home")));
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // allow the user to select ONLY DIRECTORIES
                fc.setDialogTitle("Choose Containing Folder");

                // get path to save output in
                int result = fc.showOpenDialog(main_frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    String dirPath = fc.getSelectedFile().getAbsolutePath();
                    String fileName = JOptionPane.showInputDialog("Enter file name (NO EXTENSION)");
                    String filePath = dirPath + System.getProperty("file.separator") + fileName + ".txt";

                    // write to selected file
                    File f = new File(filePath);
                    FileWriter fw;
                    try {
                        if (f.exists()) {
                            fw = new FileWriter(f);
                        } else {
                            Files.createFile(f.toPath());
                            fw = new FileWriter(f);
                        }

                        fw.write("Symphonia Command File\n");

                        for (Note n : tune) {
                            String deltaOctave = (n.deltaOctave >= 0) ? "+" + n.deltaOctave : Integer.toString(n.deltaOctave);
                            String cmd = "add " + n.noteName + " " + n.duration + " " + deltaOctave + " " + n.dynamic_marking;
                            fw.write(cmd + "\n");
                        }

                        fw.flush();
                        fw.close();

                        feedback.append("\nExported tune succesfully. You can now import the file produced to recover the tune.\n\n");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };

        importFile.addActionListener(importAction);
        exportFile.addActionListener(exportAction);

        // add menu items to file menu
        fileMenu.add(importFile);
        fileMenu.add(exportFile);
        fileMenu.add(toggleAddMode);

        // create "Tune" menu containing graphical interfaces for various actions
        final JMenu tuneMenu = new JMenu("Tune");

        // create menu items for the "Tune" menu
        final JMenuItem addNote = new JMenuItem("Add note");
        final JMenuItem removeNote = new JMenuItem("Remove note");
        final JMenuItem replaceNote = new JMenuItem("Replace note");
        final JMenuItem playTune = new JMenuItem("Play tune");

        addNote.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // add a new note
                if (isFull) {
                    JOptionPane.showMessageDialog(null, "Your tune has reached its maximum size. Please remove or replace notes if you want to change it.");
                    return;
                }

                Object[] noteOptions = {"C", "D", "E", "F", "G", "A", "B"};
                String name = (String) JOptionPane.showInputDialog(null, "Select note name", "Note Name", JOptionPane.QUESTION_MESSAGE, null, noteOptions, noteOptions[0]);

                Object[] modifierOptions = {"flat (b)", "normal", "sharp (#)"};
                String modifier = (String) JOptionPane.showInputDialog(null, "Select modifiers (flat, sharp) if any", "Note Modifiers", JOptionPane.QUESTION_MESSAGE, null, modifierOptions, modifierOptions[1]);

                String noteName = name + ((modifier.equals(modifierOptions[1])) ? "" : (modifier.equals(modifierOptions[0]) ? "b" : "#"));

                Object[] durationOptions = {"0.25", "0.5", "1", "1.5", "2", "3", "4"};
                double duration = Double.parseDouble((String) JOptionPane.showInputDialog(null, "Select note duration (number of beats)", "Note Duration", JOptionPane.QUESTION_MESSAGE, null, durationOptions, durationOptions[2]));

                Object[] octaveChangeOptions = {"-2", "-1", "0", "1", "2"};
                int deltaOctave = Integer.parseInt((String) JOptionPane.showInputDialog(null, "Select octave change (if any)", "Octave Change", JOptionPane.QUESTION_MESSAGE, null, octaveChangeOptions, octaveChangeOptions[2]));

                Object[] dynamicOptions = {"extremely soft (pianississimo)", "very soft (pianissimo)", "soft (piano)", "medium soft (mezzo piano)", "medium loud (mezzo forte)", "loud (forte)", "very loud (fortissimo)", "extremely loud (fortississimo)", "same as previous note"};
                String dynamic = current_dynamic;
                String in = (String) JOptionPane.showInputDialog(null, "How loud do you want your music to start?", "Choose Initial Dynamic (Loudness)", JOptionPane.QUESTION_MESSAGE, null, dynamicOptions, dynamicOptions[8]);
                if (in.equals(dynamicOptions[8])) {
                    dynamic = current_dynamic;
                } else if (in.equals(dynamicOptions[0])) {
                    dynamic = "ppp";
                } else if (in.equals(dynamicOptions[1])) {
                    dynamic = "pp";
                } else if (in.equals(dynamicOptions[2])) {
                    dynamic = "p";
                } else if (in.equals(dynamicOptions[3])) {
                    dynamic = "mp";
                } else if (in.equals(dynamicOptions[4])) {
                    dynamic = "mf";
                } else if (in.equals(dynamicOptions[5])) {
                    dynamic = "f";
                } else if (in.equals(dynamicOptions[6])) {
                    dynamic = "ff";
                } else if (in.equals(dynamicOptions[7])) {
                    dynamic = "fff";
                }

                Note toAdd = new Note(noteName, duration, deltaOctave, dynamic);
                tune.add(toAdd);
                feedback.append("Added Note: " + toAdd.getName() + "\n");

                main_frame.repaint();
            }
        });

        removeNote.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // remove the note at a given index
                String input;
                int index;
                while (true) {
                    input = JOptionPane.showInputDialog("Enter index of note to remove (say \"print tune\" to get the indices of all notes)");
                    try {
                        index = Integer.parseInt(input);
                        if (index < 0 || index >= tune.size()) {
                            JOptionPane.showMessageDialog(null, "Invalid input. Please enter a positive number.");
                        } else {
                            break;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Invalid input. Please enter a number.");
                    }
                }
                tune.remove(index);

                main_frame.repaint();
            }
        });

        replaceNote.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // replace the note at a given index
                String input;
                int index;
                while (true) {
                    input = JOptionPane.showInputDialog("Enter index of note to replace (say \"print tune\" to get the indices of all notes)");
                    try {
                        index = Integer.parseInt(input);
                        if (index < 0 || index >= tune.size()) {
                            JOptionPane.showMessageDialog(null, "Invalid input. Please enter a positive number.");
                        } else {
                            break;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Invalid input. Please enter a number.");
                    }
                }

                Object[] noteOptions = {"C", "D", "E", "F", "G", "A", "B"};
                String name = (String) JOptionPane.showInputDialog(null, "Select note name", "Note Name", JOptionPane.QUESTION_MESSAGE, null, noteOptions, noteOptions[0]);

                Object[] modifierOptions = {"flat (b)", "normal", "sharp (#)"};
                String modifier = (String) JOptionPane.showInputDialog(null, "Select modifiers (flat, sharp) if any", "Note Modifiers", JOptionPane.QUESTION_MESSAGE, null, modifierOptions, modifierOptions[1]);

                String noteName = name + ((modifier.equals(modifierOptions[1])) ? "" : (modifier.equals(modifierOptions[0]) ? "b" : "#"));

                Object[] durationOptions = {"0.25", "0.5", "1", "1.5", "2", "3", "4"};
                double duration = Double.parseDouble((String) JOptionPane.showInputDialog(null, "Select note duration (number of beats)", "Note Duration", JOptionPane.QUESTION_MESSAGE, null, durationOptions, durationOptions[2]));

                Object[] octaveChangeOptions = {"-2", "-1", "0", "1", "2"};
                int deltaOctave = Integer.parseInt((String) JOptionPane.showInputDialog(null, "Select octave change (if any)", "Octave Change", JOptionPane.QUESTION_MESSAGE, null, octaveChangeOptions, octaveChangeOptions[2]));

                Object[] dynamicOptions = {"extremely soft (pianississimo)", "very soft (pianissimo)", "soft (piano)", "medium soft (mezzo piano)", "medium loud (mezzo forte)", "loud (forte)", "very loud (fortissimo)", "extremely loud (fortississimo)", "same as previous note"};
                String dynamic = current_dynamic;
                String in = (String) JOptionPane.showInputDialog(null, "How loud do you want your music to start?", "Choose Initial Dynamic (Loudness)", JOptionPane.QUESTION_MESSAGE, null, dynamicOptions, dynamicOptions[8]);
                if (in.equals(dynamicOptions[8])) {
                    dynamic = current_dynamic;
                } else if (in.equals(dynamicOptions[0])) {
                    dynamic = "ppp";
                } else if (in.equals(dynamicOptions[1])) {
                    dynamic = "pp";
                } else if (in.equals(dynamicOptions[2])) {
                    dynamic = "p";
                } else if (in.equals(dynamicOptions[3])) {
                    dynamic = "mp";
                } else if (in.equals(dynamicOptions[4])) {
                    dynamic = "mf";
                } else if (in.equals(dynamicOptions[5])) {
                    dynamic = "f";
                } else if (in.equals(dynamicOptions[6])) {
                    dynamic = "ff";
                } else if (in.equals(dynamicOptions[7])) {
                    dynamic = "fff";
                }

                Note toPut = new Note(noteName, duration, deltaOctave, dynamic);
                String replaced = tune.get(index).getName();
                tune.set(index, toPut);
                feedback.append("Replaced note: " + replaced + "  with  " + toPut.getName() + "\n");

                main_frame.repaint();
            }
        });

        playTune.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // play the tune
                try {
                    // initialize audio MIDI variables to synthesize sound (and play the tune)
                    Synthesizer synth;
                    synth = MidiSystem.getSynthesizer();
                    synth.open();
                    final MidiChannel[] mcs = synth.getChannels();
                    Instrument[] instrs = synth.getDefaultSoundbank().getInstruments();
                    synth.loadInstrument(instrs[0]);

                    // synthesize each note
                    if (!Main.tune.isEmpty()) {
                        feedback.append("\nPlaying...\n");
                        for (Note n : Main.tune) {
                            n.play(mcs[0], Main.beat_duration);
                        }
                        feedback.append("Done.\n");
                    } else {
                        feedback.append("Tune is empty.\n");
                    }
                } catch (MidiUnavailableException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // add menu items to the "Tune" menu
        tuneMenu.add(addNote);
        tuneMenu.add(removeNote);
        tuneMenu.add(replaceNote);
        tuneMenu.add(playTune);

        // add both menus to the menu bar
        mb.add(fileMenu);
        mb.add(tuneMenu);

        // set the menu bar as the menu bar of the main frame
        main_frame.setJMenuBar(mb);

        // add all the panels to the main frame
        main_frame.add(p, BorderLayout.CENTER);
        main_frame.add(feedback_panel, BorderLayout.NORTH);
        main_frame.add(panel_bottom, BorderLayout.SOUTH);

        // set some more attributes of the main frame
        main_frame.setResizable(false);
        main_frame.pack();
        main_frame.setVisible(true);

        // initialize input field
        final JTextField cmd_field = new JTextField();
        cmd_field.setPreferredSize(new Dimension(1000, 20));

        // create "OK" button to submit the contents of the input field as a command
        JButton ok_button = new JButton("OK");
        ok_button.setPreferredSize(new Dimension(70, 30));

        ActionListener ok_action = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // evaluate and execute the command issued by the user using reflection
                String input, command;
                Class cmdClass = Symphonia.class;
                Class[] paramTypes = {String.class};
                Symphonia mm = new Symphonia();
                Method m;

                input = cmd_field.getText();
                command = (input.contains(" ")) ? input.substring(0, input.indexOf(" ")) : input;

                if (!command.equals("exit")) {
                    String returnVal = "";

                    if (input.equals("addmode on")) {
                        // turn add mode on (if it is not already on)
                        if (isAddmode) {
                            feedback.append("Already in add mode.\n");
                        } else {
                            isAddmode = true;
                            feedback.append("Switched to add mode.\n");
                            toggleAddMode.setText("Addmode off");
                        }
                    } else if (input.equals("addmode off")) {
                        // turn add mode off (if it is not already off)
                        if (isAddmode) {
                            isAddmode = false;
                            feedback.append("Add mode is now off.\n");
                            toggleAddMode.setText("Addmode on");
                        } else {
                            feedback.append("Add mode is already off.\n");
                        }
                    } else if (isAddmode) {
                        if (isFull) {
                            feedback.append("Your tune has reached its maximum size. Remove or replace notes if you want to change it.\n");
                            feedback.append("Note that you are in add mode. Exit add mode to use commands other than \"add\"\n");
                            return;
                        }
                        try {
                            m = cmdClass.getDeclaredMethod("add", paramTypes);
                            returnVal = (String) m.invoke(mm, "add " + input);
                            feedback.append(returnVal);
                            if (returnVal.contains(Symphonia.usage_add)) {
                                // invalid input
                                feedback.append("Note that you are in add mode. Exit add mode to use commands other than \"add\"\n");
                            }
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        if (isFull && command.equals("add")) {
                            feedback.append("Your tune has reached its maximum size. Remove or replace notes it you want to change it.\n");
                            return;
                        }
                        try {
                            // search and execute appropriate method
                            m = cmdClass.getDeclaredMethod(command, paramTypes);
                            returnVal = (String) m.invoke(mm, input);
                            feedback.append(returnVal);
                        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                            feedback.append("No such command (yet).\n");
                        }
                    }

                    cmd_field.setText("");
                    main_frame.repaint();
                } else {
                    System.exit(0);
                }
            }
        };

        // assign action listener
        ok_button.addActionListener(ok_action);
        cmd_field.addActionListener(ok_action);

        // add the input field and the OK button to the bottom panel
        panel_bottom.add(cmd_field);
        panel_bottom.add(ok_button);

        // repaint (update) the main frame to display all the changes to its components
        main_frame.repaint();
    }

    /**
     * Set the current dynamic.
     *
     * @param dynamic the value to set he current dynamic to
     */
    public static void setDynamic(String dynamic) {
        current_dynamic = dynamic;
    }

    /**
     * Setter for isFull.
     *
     * @param full the value to set isFull to
     */
    public static void setFull(boolean full) {
        isFull = full;
    }
}

/**
 * The main panel of the application.
 */
class MainPanel extends JPanel {

    // initialize constants used in the paint method
    private final double staffStartX = 50;
    private final double staffEndX = Main.width - 50;
    private final double firstStaffY = 50;

    private final int staffDistance = 10;
    private final int beatDistance = 30;

    private final int sharpLineLength = 13; // width of '#' (sharp) sign
    private final int flatWidth = 10; // width of 'b' (flat) sign

    private final int noteHeadBreadth = 10;
    private final int noteHeadHeight = 7;
    private final double dotRadius = 2.5; // for dotted notes

    private final int noteStalkHeight = 30;

    private final int maxNewLines = 2;

    /**
     * Default constructor.
     */
    public MainPanel() {
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }

    /**
     * The paint method for the main panel. This method paints the tune in sheet
     * music form. Called automatically during runtime.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;

        // paint tune
        if (Main.tune.isEmpty()) {
            return;
        }

        double prevCenterX = staffStartX;
        double prevDuration = 1;
        double centerX;
        double centerY;
        ArrayList<Integer> newLines = new ArrayList<>();

        // calculate the number of stave required
        for (int index = 0; index < Main.tune.size(); index++) {
            centerX = prevCenterX + prevDuration * beatDistance;
            if (prevDuration == 0.25 || prevDuration == 0.5) {
                centerX = prevCenterX + 20;
            }
            centerX += (Main.tune.get(index).noteName.contains("#")) ? sharpLineLength : (Main.tune.get(index).noteName.contains("b")) ? flatWidth : 0;
            
            if (centerX > staffEndX) {
                newLines.add(index);
                centerX = staffStartX + beatDistance;
                centerX += (Main.tune.get(index).noteName.contains("#")) ? sharpLineLength : (Main.tune.get(index).noteName.contains("b")) ? flatWidth : 0;
            }
            
            prevCenterX = centerX;
            prevDuration = Main.tune.get(index).duration;
        }

        ArrayList<Double> offsets = new ArrayList<>();
        
        // calculate required distances/offsets
        for (int i = 0; i < newLines.size(); i++) {
            int start = newLines.get(i);
            int end = (i == newLines.size() - 1) ? Main.tune.size() - 1 : newLines.get(i + 1) - 1;
            int[] dists = new int[end - start + 1];
            double offset = 0;
            for (int j = 0; j <= end - start; j++) {
                dists[j] = Main.tune.get(start + j).staffDistFromMidC();
            }

            int max = dists[0];
            for (int j = 0; j <= end - start; j++) {
                if (dists[j] > max) {
                    max = dists[j];
                }
            }

            offset = 0;
            int topStaffDist = new Note("F", 1, 1, "f").staffDistFromMidC();
            if (max > topStaffDist) {
                for (int j = topStaffDist; j <= max; j += 2) {
                    offset += staffDistance; 
                }
            }
            offsets.add(offset);
        }

        double offset = 0;
        int topStaffDist = new Note("F", 1, 1, "f").staffDistFromMidC();
        int end = (newLines.isEmpty()) ? Main.tune.size() : newLines.get(0);

        int[] dists = new int[end];
        for (int i = 0; i < end; i++) {
            dists[i] = Main.tune.get(i).staffDistFromMidC();
        }

        int max = dists[0];
        for (int i = 0; i < end; i++) {
            if (dists[i] > max) {
                max = dists[i];
            }
        }

        if (max > topStaffDist) {
            for (int i = topStaffDist; i <= max; i += 2) {
                offset += staffDistance;
            }
        }

        // draw the stave
        double staffStartY = firstStaffY + offset;
        for (int i = 0; i < 5; i++) {
            g2.draw(new Line2D.Double(staffStartX, staffStartY + i * staffDistance, staffEndX, staffStartY + i * staffDistance));
        }

        double middleEY = staffStartY + 4 * staffDistance;
        
        double[] centerYs = new double[end];
        for (int i = 0; i < end; i++) {
            Note n = Main.tune.get(i);
            centerYs[i] = middleEY - (n.staffDistFromMidC() - 2) * (staffDistance / 2);
        }
        double maxY = centerYs[0];
        for (double d : centerYs) {
            if (d > maxY) {
                maxY = d;
            }
        }

        // draw the notes and dynamic markings
        prevCenterX = staffStartX;
        prevDuration = 1;
        double cornerX;
        double cornerY;
        String prevDynamic = "";
        int newLineNo = 0;

        for (int index = 0; index < Main.tune.size(); index++) {
            Note n = Main.tune.get(index);

            // draw new set of staff lines if required
            if (newLines.contains(index)) {
                if (newLineNo >= maxNewLines) {
                    return;
                }

                staffStartY = Math.max(middleEY, maxY) + 3 * staffDistance + offsets.get(newLineNo);
                for (int i = 0; i < 5; i++) {
                    g2.draw(new Line2D.Double(staffStartX, staffStartY + i * staffDistance, staffEndX, staffStartY + i * staffDistance));
                }
                prevCenterX = staffStartX;
                prevDuration = 1;
                middleEY = staffStartY + 4 * staffDistance;

                end = (index == newLines.get(newLines.size() - 1)) ? Main.tune.size() - 1 : newLines.get(newLines.indexOf(index) + 1);
                centerYs = new double[end - index + 1];
                for (int j = 0; j < end - index; j++) {
                    Note tmp = Main.tune.get(index + j);
                    centerYs[j] = middleEY - (tmp.staffDistFromMidC() - 2) * (staffDistance / 2);
                }
                maxY = centerYs[0];
                for (double d : centerYs) {
                    if (d > maxY) {
                        maxY = d;
                    }
                }

                newLineNo++;
            }

            centerX = prevCenterX + prevDuration * beatDistance;
            if (prevDuration == 0.25 || prevDuration == 0.5) {
                centerX = prevCenterX + 20;
            }
            centerY = middleEY - (n.staffDistFromMidC() - 2) * (staffDistance / 2);

            // draw accidentals
            if (n.noteName.contains("#")) {
                double sharpCenterX = centerX; // the center calculated should be the center of the sharp symbol to be drawn
                centerX += sharpLineLength; // shift the note's center ahead to accomodate for the sharp symbol
                // draw the '#' (sharp) shymbol
                g2.draw(new Line2D.Double(sharpCenterX - (sharpLineLength / 2), centerY - (staffDistance / 2) + (staffDistance / 4.5), sharpCenterX + (sharpLineLength / 2), centerY - (staffDistance / 2) + (staffDistance / 4.5)));
                g2.draw(new Line2D.Double(sharpCenterX - (sharpLineLength / 2), centerY + (staffDistance / 2) - (staffDistance / 4.5), sharpCenterX + (sharpLineLength / 2), centerY + (staffDistance / 2) - (staffDistance / 4.5)));
                g2.draw(new Line2D.Double(sharpCenterX - (sharpLineLength / 4), centerY + (staffDistance / 2), sharpCenterX - (sharpLineLength / 4), centerY - (staffDistance / 2)));
                g2.draw(new Line2D.Double(sharpCenterX + (sharpLineLength / 4), centerY + (staffDistance / 2), sharpCenterX + (sharpLineLength / 4), centerY - (staffDistance / 2)));
            } else if (n.noteName.contains("b")) {
                double flatCenterX = centerX; // center calculated is the center of the flat symbol to be drawn
                centerX += flatWidth; // shift the note's center ahead to accomodate for the flat symbol

                // draw straight line of flat symbol
                g2.draw(new Line2D.Double(flatCenterX - 0.5 * flatWidth, centerY - 1.5 * staffDistance, flatCenterX - 0.5 * flatWidth, centerY + 0.5 * staffDistance));

                // draw the curved part of the flat symbol (b) using a cubic curve
                CubicCurve2D curve = new CubicCurve2D.Double();
                double startX = flatCenterX - 0.5 * flatWidth;
                double startY = centerY - 0.25 * staffDistance;
                double endX = flatCenterX - 0.5 * flatWidth;
                double endY = centerY + 0.5 * staffDistance;
                double ctrl1X = flatCenterX;
                double ctrl1Y = centerY - 0.5 * staffDistance;
                double ctrl2X = flatCenterX + 0.5 * flatWidth;
                double ctrl2Y = centerY - 0.25 * staffDistance;
                curve.setCurve(startX, startY, ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY);
                g2.draw(curve);
            }
            cornerX = centerX - (noteHeadBreadth / 2); // calculate the X value of the top left corner of the note head
            cornerY = centerY - (noteHeadHeight / 2); // calculate the Y value of the top left corner of the note head

            // draw the note based on its duration
            if (n.duration == 0.25) { // for semi-quavers
                // draw note head
                g2.fill(new Ellipse2D.Double(cornerX, cornerY, noteHeadBreadth, noteHeadHeight));
                g2.draw(new Line2D.Double(centerX + noteHeadBreadth / 2, centerY, centerX + noteHeadBreadth / 2, centerY - noteStalkHeight)); // draw the note's stalk
                
                // draw the first curve of the flag
                double ctrl1X = centerX + noteHeadBreadth * (7 / 10);
                double ctrl1Y = centerY - 0.75 * noteStalkHeight;
                double ctrl2X = centerX + 1.25 * noteHeadBreadth;
                double ctrl2Y = centerY - noteStalkHeight * (5 / 8);
                double startX = centerX + noteHeadBreadth / 2;
                double startY = centerY - noteStalkHeight;
                double endX = centerX + noteHeadBreadth;
                double endY = centerY - noteStalkHeight / 2;
                CubicCurve2D curve = new CubicCurve2D.Double();
                curve.setCurve(startX, startY, ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY);
                g2.draw(curve);

                // draw the second curve of the flag
                startY += 7.5;
                ctrl1Y += 7.5;
                ctrl2Y += 7.5;
                endY += 7.5;
                CubicCurve2D curve2 = new CubicCurve2D.Double();
                curve2.setCurve(startX, startY, ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY);
                g2.draw(curve2);
            } else if (n.duration == 0.5) { // for quavers
                // draw note head
                g2.fill(new Ellipse2D.Double(cornerX, cornerY, noteHeadBreadth, noteHeadHeight));
                g2.draw(new Line2D.Double(centerX + noteHeadBreadth / 2, centerY, centerX + noteHeadBreadth / 2, centerY - noteStalkHeight)); // draw the note's stalk
                
                // draw flag
                double ctrl1X = centerX + noteHeadBreadth * (7 / 10);
                double ctrl1Y = centerY - 0.75 * noteStalkHeight;
                double ctrl2X = centerX + 1.25 * noteHeadBreadth;
                double ctrl2Y = centerY - noteStalkHeight * (5 / 8);
                double startX = centerX + noteHeadBreadth / 2;
                double startY = centerY - noteStalkHeight;
                double endX = centerX + noteHeadBreadth;
                double endY = centerY - noteStalkHeight / 2;
                CubicCurve2D curve = new CubicCurve2D.Double();
                curve.setCurve(startX, startY, ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY);
                g2.draw(curve);
            } else if (n.duration == 1) { // for crotchets
                g2.fill(new Ellipse2D.Double(cornerX, cornerY, noteHeadBreadth, noteHeadHeight));
                g2.draw(new Line2D.Double(centerX + noteHeadBreadth / 2, centerY, centerX + noteHeadBreadth / 2, centerY - noteStalkHeight));
            } else if (n.duration == 1.5) { // for dotted crotchets
                g2.fill(new Ellipse2D.Double(cornerX, cornerY, noteHeadBreadth, noteHeadHeight));
                g2.draw(new Line2D.Double(centerX + noteHeadBreadth / 2, centerY, centerX + noteHeadBreadth / 2, centerY - noteStalkHeight));
                g2.fill(new Ellipse2D.Double(cornerX + 1.35 * noteHeadBreadth, cornerY + 0.15 * noteHeadHeight, dotRadius, dotRadius)); // draw the dot in front of the note
            } else if (n.duration == 2) { // for minims
                g2.draw(new Ellipse2D.Double(cornerX, cornerY, noteHeadBreadth, noteHeadHeight));
                g2.draw(new Line2D.Double(centerX + noteHeadBreadth / 2, centerY, centerX + noteHeadBreadth / 2, centerY - noteStalkHeight));
            } else if (n.duration == 3) { // for dotted minims
                g2.draw(new Ellipse2D.Double(cornerX, cornerY, noteHeadBreadth, noteHeadHeight));
                g2.draw(new Line2D.Double(centerX + noteHeadBreadth / 2, centerY, centerX + noteHeadBreadth / 2, centerY - noteStalkHeight));
                g2.fill(new Ellipse2D.Double(cornerX + 1.35 * noteHeadBreadth, cornerY + 0.15 * noteHeadHeight, dotRadius, dotRadius)); // draw the dot in front of the note
            } else if (n.duration == 4) { // for semibreves
                g2.draw(new Ellipse2D.Double(cornerX, cornerY, noteHeadBreadth, noteHeadHeight));
            }

            // draw leger lines if required
            int dist = n.staffDistFromMidC();
            if (dist <= 0) {
                for (double y = middleEY; y <= centerY; y += staffDistance) {
                    g2.draw(new Line2D.Double(centerX - 10, y, centerX + 10, y));
                }
            } else if (dist > topStaffDist) {
                for (double y = staffStartY; y >= centerY; y -= staffDistance) {
                    g2.draw(new Line2D.Double(centerX - 10, y, centerX + 10, y));
                }
            }

            // draw dynamic markings as strings (only if the dynamic marking is not the same as that of the previous note)
            if (!n.dynamic_marking.equals(prevDynamic)) {
                g2.drawString(n.dynamic_marking, (int) centerX, (int) (Math.max(centerY, middleEY) + 1.5 * staffDistance));
            }

            // prepare variables for next iteration
            prevCenterX = centerX;
            prevDuration = n.duration;
            prevDynamic = n.dynamic_marking;

            // set isFull based on position of last note
            if (index == Main.tune.size() - 1) {
                double nextX = centerX + ((n.duration == 0.5 || n.duration == 0.25) ? 20 : n.duration * beatDistance);
                if (nextX > staffEndX && newLineNo == maxNewLines) {
                    Main.setFull(true);
                } else {
                    Main.setFull(false);
                }
            }
        }
    }
}
