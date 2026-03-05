# Setup Guide - Installing Java and Running the Application

## Current Status
❌ Java is not installed on your system  
❌ Maven is not installed on your system

## Prerequisites Installation

### Step 1: Install Java JDK 17 or Higher

You need to install Java Development Kit (JDK) to run Spring Boot applications.

#### Option A: Using Chocolatey (Recommended for Windows)

If you have Chocolatey package manager installed:

```powershell
choco install openjdk17
```

#### Option B: Manual Download

1. **Download OpenJDK 17**:
   - Visit: https://adoptium.net/
   - Click "Download Latest LTS" (Java 17)
   - Choose Windows x64 installer (.msi)

2. **Install the downloaded file**:
   - Run the `.msi` installer
   - **IMPORTANT**: Check "Set JAVA_HOME variable" during installation
   - **IMPORTANT**: Check "Add to PATH" during installation

3. **Verify installation**:
   Open a **NEW** PowerShell window and run:
   ```powershell
   java -version
   ```
   You should see something like:
   ```
   openjdk version "17.0.x"
   ```

### Step 2: Using Maven Wrapper (No Maven Installation Needed!)

I've included the Maven Wrapper in your project, which means you **DON'T need to install Maven separately**. The wrapper will download Maven automatically on first use.

## Running the Application

### Once Java is Installed:

1. **Open a NEW PowerShell window** (to refresh environment variables)

2. **Navigate to the project directory**:
   ```powershell
   cd c:\Users\azizb\Desktop\spring
   ```

3. **Run the application using Maven Wrapper**:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   On first run, it will download Maven and all dependencies (this may take a few minutes).

4. **Access the application**:
   - API: http://localhost:8080/api/projects
   - H2 Console: http://localhost:8080/h2-console

## Alternative: Using an IDE

If you prefer using an IDE instead of command line:

### IntelliJ IDEA (Recommended)
1. Download IntelliJ IDEA Community Edition (free): https://www.jetbrains.com/idea/download/
2. Open the project folder: `c:\Users\azizb\Desktop\spring`
3. Wait for dependency download
4. Right-click on `BackofficeApplication.java` → Run

### Eclipse
1. Download Eclipse IDE for Java Developers: https://www.eclipse.org/downloads/
2. Import the project as "Existing Maven Project"
3. Right-click on `BackofficeApplication.java` → Run As → Java Application

### VS Code
1. Install "Extension Pack for Java" from Microsoft
2. Install "Spring Boot Extension Pack"
3. Open the project folder
4. Press F5 to run

## Testing the API

Once the application is running, test it using PowerShell:

### Create a project:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/projects" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"title":"Website Development","description":"Build a corporate website","budget":3000.0,"duration":20}'
```

### Get all projects:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/projects" -Method GET
```

### View in browser:
Open your browser and go to:
- http://localhost:8080/api/projects

## Troubleshooting

### "JAVA_HOME is not set" error
After installing Java, set JAVA_HOME manually:
```powershell
# Replace with your actual Java installation path
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot", "Machine")
```
Then restart PowerShell.

### Port 8080 already in use
Change the port in `src/main/resources/application.properties`:
```properties
server.port=8081
```

### Need help?
Let me know if you encounter any issues during installation!
