# EncNotepad

A secure, minimalist, and beautifully designed native Java text editor that transparently encrypts and decrypts your notes on the fly using military-grade cryptography standards.

## Core Security Features
- **Military-Grade Encryption**: Replaces typical plain-text saving with advanced DES/CBC mode encryption, shielding your notes behind impenetrable bytes.
- **Unpredictable Chaos (IVs)**: Uses fully randomized Initialization Vectors from a `SecureRandom` engine on every save. Hitting save 100 times on the exact same text produces 100 completely unique physical files on disk.
- **Unforgeable Authenticity**: Employs the gold-standard "Encrypt-then-MAC" paradigm (via HMAC-SHA256). It is actively mathematically impossible for a hacker to intercept and modify your ciphertext without the notepad aggressively locking them out and detecting the forgery.
- **Backward Compatibility**: Read older files safely thanks to built-in fallback modes.

## How to Run

Because `EncNotepad` is separated cleanly into modular files (`FileHandler.java` and `CipherUtil.java`), you cannot run it using Java's quick single-file launch mode. You must compile the application first.

1. Ensure you have Java 11 or higher installed on your computer.
2. Open your Command Prompt or PowerShell inside this directory.
3. **Compile everything**:
   ```bash
   javac *.java
   ```
4. **Run the compiled application**:
   ```bash
   java EncNotepad
   ```
*(Notice the deliberate lack of the `.java` extension when executing!)*
