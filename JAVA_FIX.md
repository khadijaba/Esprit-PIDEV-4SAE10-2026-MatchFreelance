# Quick Fix: Setting JAVA_HOME

The Java JDK is installed but the `JAVA_HOME` environment variable needs to be set.

## Option 1: Restart Your Terminal (Recommended)

The installer may have set JAVA_HOME, but you need to restart PowerShell to load the new environment variables.

1. **Close this PowerShell/Terminal window completely**
2. **Open a NEW PowerShell window**
3. **Navigate to the project**:
   ```powershell
   cd c:\Users\azizb\Desktop\spring
   ```
4. **Run the backend**:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

## Option 2: Find and Set JAVA_HOME Manually

If restarting doesn't work, find where Java was installed:

### Find Java Installation

Run this in PowerShell to find Java:
```powershell
Get-ChildItem "C:\Program Files" -Filter "java.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object FullName
```

Or check common locations:
- `C:\Program Files\Java\jdk-17*`
- `C:\Program Files\Java\jdk-21*`
- `C:\Program Files\Eclipse Adoptium\jdk-17*`
- `C:\Program Files\Eclipse Adoptium\jdk-21*`

### Set JAVA_HOME (PowerShell as Administrator)

Once you find the Java installation path (e.g., `C:\Program Files\Java\jdk-17.0.10`):

```powershell
# Set for current session
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.10"
$env:Path += ";$env:JAVA_HOME\bin"

# Set permanently (requires admin)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17.0.10", "Machine")
[System.Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Program Files\Java\jdk-17.0.10\bin", "Machine")
```

**Replace** `C:\Program Files\Java\jdk-17.0.10` with your actual Java path!

### Verify Java Works

After setting JAVA_HOME:
```powershell
java -version
```

You should see output like:
```
openjdk version "17.0.x" or "21.0.x"
```

## Option 3: Reinstall Java with Correct Settings

If you're still having issues:

1. **Uninstall Java** from Windows Settings → Apps
2. **Download fresh installer**: https://adoptium.net/
3. **During installation**, make sure to check:
   - ✅ **Set JAVA_HOME variable**
   - ✅ **Add to PATH**
4. **Restart your computer** (or at least PowerShell)
5. **Try again**:
   ```powershell
   cd c:\Users\azizb\Desktop\spring
   .\mvnw.cmd spring-boot:run
   ```

## After Fixing JAVA_HOME

Once Java is working, the backend will start:

1. You'll see Maven downloading dependencies (first time only)
2. Spring Boot will start and show logs
3. Look for: `Started BackofficeApplication in X seconds`
4. The API will be available at: **http://localhost:8080/api/projects**

## Need Help?

Let me know:
- What Java installer you used
- Where Java was installed
- What happens when you try `java -version`

I can help troubleshoot further!
