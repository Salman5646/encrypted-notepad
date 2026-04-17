import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * FileHandler - Handles all saving and opening operations for the notepad.
 */
public class FileHandler {

    public static final String ENC_HEADER = "[ENC]";

    private static String getDefaultDirectory() {
        File desktopPath = new File(System.getProperty("user.home"), "Desktop");
        File defaultFolder = new File(desktopPath, "EncryptedFiles");
        if (!defaultFolder.exists()) {
            defaultFolder.mkdirs(); // Create the folder if it doesn't exist yet
        }
        return defaultFolder.getAbsolutePath();
    }

    public static File openFile(Component parent) {
        FileDialog fileDialog = new FileDialog((Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent), "Open Encrypted File", FileDialog.LOAD);
        
        // Do not restrict visibility strictly to "*.enc" so users can see files saved with other/no extensions
        fileDialog.setDirectory(getDefaultDirectory());
        fileDialog.setVisible(true);

        String fileName = fileDialog.getFile();
        String directory = fileDialog.getDirectory();

        if (fileName != null) {
            return new File(directory, fileName);
        }
        return null;
    }

    public static File saveFileAs(Component parent) {
        FileDialog fileDialog = new FileDialog((Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent), "Save Encrypted File", FileDialog.SAVE);
        fileDialog.setFile("Untitled.enc");
        fileDialog.setDirectory(getDefaultDirectory());
        fileDialog.setVisible(true);

        String fileName = fileDialog.getFile();
        String directory = fileDialog.getDirectory();

        if (fileName != null) {
            return new File(directory, fileName);
        }
        return null;
    }

    public static String readFile(File file, char[] password) throws Exception {
        String content = new String(Files.readAllBytes(file.toPath()));
        
        // Detect if the file is encrypted
        boolean isEncrypted = file.getName().toLowerCase().endsWith(".enc") || content.startsWith(ENC_HEADER);
        
        if (isEncrypted) {
            String dataToDecrypt = content.startsWith(ENC_HEADER) ? content.substring(ENC_HEADER.length()) : content;
            return CipherUtil.decrypt(dataToDecrypt, password);
        } else {
            return content;
        }
    }

    public static void writeFile(File file, String content, char[] password) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            String encryptedContent = CipherUtil.encrypt(content, password);
            writer.write(ENC_HEADER + encryptedContent);
        }
    }
}
