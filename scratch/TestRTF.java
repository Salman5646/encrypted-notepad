import java.io.*;
import java.nio.file.Files;

public class TestRTF {
    public static void main(String[] args) throws Exception {
        String rtfContent = "{\\rtf1\\ansi\\ansicpg1252\\cocoartf2580\n" +
                            "{\\fonttbl\\f0\\fswiss\\fcharset0 Helvetica;}\n" +
                            "{\\colortbl;\\red255\\green255\\blue255;}\n" +
                            "Hello World}";
        
        File tempFile = new File("test.rtf");
        Files.write(tempFile.toPath(), rtfContent.getBytes());
        
        // This is what the app does
        String content = new String(Files.readAllBytes(tempFile.toPath()));
        System.out.println("--- Opened File Content ---");
        System.out.println(content);
        System.out.println("---------------------------");
        
        tempFile.delete();
    }
}
