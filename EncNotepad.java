import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * EncNotepad - A simple, clean, and premium-feel notepad application using Java Swing.
 */
public class EncNotepad extends JFrame {

    private static final String APP_NAME = "EncNotepad";
    private JTextArea textArea;
    private JMenuBar menuBar;
    private JMenu fileMenu, editMenu, helpMenu;
    private JMenuItem newItem, openItem, saveItem, saveAsItem, exitItem;
    private JMenuItem cutItem, copyItem, pasteItem;
    private JMenuItem aboutItem;

    private File currentFile = null;
    private boolean isModified = false;

    public EncNotepad() {
        // Set the frame properties
        setTitle("EncNotepad - Secure Text Editor");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Try to set System Look and Feel for a more native and premium appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize components
        initComponents();
        
        // Setup Menu Bar
        setupMenuBar();

        // Make the frame visible
        setVisible(true);
    }

    private void initComponents() {
        // Create the text area with some padding and a nice font
        textArea = new JTextArea();
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(10, 10, 10, 10));

        // Track changes to the document
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { setModified(true); }
            public void removeUpdate(DocumentEvent e) { setModified(true); }
            public void changedUpdate(DocumentEvent e) { setModified(true); }
        });

        // Add a scroll pane to the text area
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupMenuBar() {
        menuBar = new JMenuBar();
        menuBar.setBackground(Color.WHITE);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));

        // --- File Menu ---
        fileMenu = new JMenu("File");
        newItem = new JMenuItem("New");
        openItem = new JMenuItem("Open...");
        saveItem = new JMenuItem("Save");
        saveAsItem = new JMenuItem("Save As...");
        exitItem = new JMenuItem("Exit");

        // Add accelerators for common tasks
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // --- Edit Menu ---
        editMenu = new JMenu("Edit");
        cutItem = new JMenuItem("Cut");
        copyItem = new JMenuItem("Copy");
        pasteItem = new JMenuItem("Paste");

        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));

        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);

        // --- Help Menu ---
        helpMenu = new JMenu("Help");
        aboutItem = new JMenuItem("About");
        helpMenu.add(aboutItem);

        // Add menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);

        // Set the menu bar for the frame
        setJMenuBar(menuBar);

        // Add Action Listeners
        newItem.addActionListener(e -> newFile());
        openItem.addActionListener(e -> openFile());
        saveItem.addActionListener(e -> saveFile(false));
        saveAsItem.addActionListener(e -> saveFile(true));
        exitItem.addActionListener(e -> exitApp());

        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this, 
                "EncNotepad v1.1\nA premium, minimal text editor interface.", 
                "About EncNotepad", JOptionPane.INFORMATION_MESSAGE));
        
        // Simple Edit operations
        cutItem.addActionListener(e -> textArea.cut());
        copyItem.addActionListener(e -> textArea.copy());
        pasteItem.addActionListener(e -> textArea.paste());
    }

    private void newFile() {
        if (confirmSave()) {
            textArea.setText("");
            currentFile = null;
            setModified(false);
            updateTitle();
        }
    }

    private void openFile() {
        if (!confirmSave()) return;

        File file = FileHandler.openFile(this);
        if (file != null) {
            char[] password = promptForPassword("Enter Password to Open");
            if (password == null) return; // User cancelled

            try {
                String content = FileHandler.readFile(file, password);
                textArea.setText(content);
                currentFile = file;
                setModified(false);
                updateTitle();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Security Error while opening file:\n" + ex.getMessage(), "Access Denied", JOptionPane.ERROR_MESSAGE);
            } finally {
                // Goal 4: Discarded from memory after use
                Arrays.fill(password, '0');
            }
        }
    }

    private void saveFile(boolean saveAs) {
        if (saveAs || currentFile == null) {
            File file = FileHandler.saveFileAs(this);
            if (file != null) {
                currentFile = file;
            } else {
                return;
            }
        }

        char[] password = promptForPassword("Set Password for Encryption");
        if (password == null) return; // User cancelled

        try {
            FileHandler.writeFile(currentFile, textArea.getText(), password);
            setModified(false);
            updateTitle();
            JOptionPane.showMessageDialog(this, "File securely encrypted and saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving secure file: " + ex.getMessage(), "Fatal Encryption Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Goal 4: Discarded from memory after use
            Arrays.fill(password, '0');
        }
    }

    private boolean confirmSave() {
        if (!isModified) return true;

        int option = JOptionPane.showConfirmDialog(this, 
            "The content has changed. Do you want to save your changes?", 
            "Save Changes", JOptionPane.YES_NO_CANCEL_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            saveFile(false);
            return !isModified; // Return true if save was successful
        } else if (option == JOptionPane.NO_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    private void exitApp() {
        if (confirmSave()) {
            System.exit(0);
        }
    }

    private void setModified(boolean modified) {
        this.isModified = modified;
        updateTitle();
    }

    private void updateTitle() {
        String title = APP_NAME + " - ";
        if (currentFile == null) {
            title += "Untitled";
        } else {
            title += currentFile.getName();
        }
        
        if (isModified) {
            title += " *";
        }
        
        setTitle(title);
    }

    private char[] promptForPassword(String title) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Password:"), BorderLayout.WEST);
        JPasswordField passwordField = new JPasswordField(20);
        panel.add(passwordField, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(this, panel, title, 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            return passwordField.getPassword();
        }
        return null;
    }

    public static void main(String[] args) {
        // Run the GUI creation on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(EncNotepad::new);
    }
}
