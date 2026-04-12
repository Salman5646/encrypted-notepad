# 🔒 EncNotepad

**EncNotepad** is an ultra-secure, minimalist, purely native Java Desktop text editor. Designed with high-stakes data privacy in mind, it transparently handles cryptographic data scrambling, digital signature authentication, and dynamic integrity checking seamlessly in the background without burdening the user.

What appears to be a flawlessly familiar white native Windows note-taking app is actually a military-grade cryptographic engine.

---

## 🛡️ Advanced Security Architecture

### 1. `DES` Encryption (Cipher Block Chaining Mode)
Instead of saving your private text raw or using basic substitution ciphers (like Caesar algorithms), EncNotepad utilizes standard `DES` encryption upgraded heavily into **CBC (Cipher Block Chaining)** mode. 
- The very first block of text heavily influences every subsequent block of text, meaning patterns in your writing are completely eradicated, rendering frequency analysis attacks entirely useless.

### 2. Randomized Initialization Vectors (IVs)
Every single time you click "Save", the system activates Java's `SecureRandom` cryptographically-secure generator to spin up a completely brand-new, 8-byte **IV (Initialization Vector)**.
- **The Result:** If you save the exact same sentence 1,000 times, the physical `.enc` files on your hard drive will mathematically look 100% different 1,000 times. This prevents hackers from deducing the context or contents of similar texts.

### 3. The Encrypt-then-MAC Authentication Paradigm
This application is strictly mathematically impenetrable against tampering. Older systems verify data by simply hashing the plain text (which is susceptible to padding oracle attacks).
- **HMAC-SHA256:** We strictly adhere to the gold-standard `Encrypt-then-MAC` paradigm. After text is scrambled using DES and the IV, a highly restricted **Secret Key** (`HmacAuthKey99`) forces the data through a one-way `HmacSHA256` press.
- **Forgery Detection:** When you open a file, **before** the system even *attempts* to decrypt the file, it generates a fresh expected HMAC fingerprint and compares it to the wax seal on the file. If an attacker manually edited a single byte of your encrypted file, perfectly forged a standard SHA-256 seal, or flipped a bit while stealing the file, EncNotepad instantly throws a hard forgery error and aggressively refuses to process the data.

---

## ⚙️ Core Technical Specifications

| Feature | Implementation Component | Function |
|---------|---------|------|
| **Core UI Engine** | Java Swing + AWT | Purely native, lighting-fast text area rendering with `SystemLookAndFeel`. |
| **Cryptography API** | `javax.crypto.*` | Leverages native Java internal security libraries (`Cipher`, `Mac`, `SecretKeyFactory`). |
| **File I/O Streams** | `java.nio.file` | Buffers characters cleanly and detects `[ENC]` header payloads dynamically. |
| **Error Handling Shield** | Deep Catch Boundaries | Prevents cryptic IO failures or HMAC Forgery detections from dumping logs onto the notepad canvas. Intercepts all cryptographic errors and outputs them gracefully via `JOptionPane` alerts. |

---

## 🚀 Installation & Usage

Because the application logic is modularized across `CipherUtil`, `FileHandler`, and `EncNotepad`, you **cannot** run this using Java 11's single-file direct execution (`java EncNotepad.java`). You must formally compile the module structure.

### 1. Prerequisites
- **Java SE Development Kit (JDK) 11** or higher.

### 2. Compile the Project
Open your Command Prompt or Terminal inside the cloned repository folder and compile the `.java` structural files into `.class` files natively:
```bash
javac *.java
```

### 3. Execute the Environment
Run the main UI thread via standard execution:
```bash
java EncNotepad
```

---

## 💻 App Features
* **Auto-Routing Storage:** Saving an encrypted file effortlessly targets `C:\Users\[User]\Desktop\EncryptedFiles`. It will automatically spin up the physical directory if this is your first time using it!
* **Transparent Backward Compatibility:** Legacy files stored using older standards (Plain ECB or basic SHA-256 integrity hashes) are detected algorithmically by array-splitting length mapping, and decrypted safely without causing authentication panic loops!

---
*Built from scratch with advanced Java Cryptographic Libraries.*
