# 📦 Application Setup & Usage Guide

This guide walks you through the full process of installing and running the application.

---

## 📥 1. Download and Extract the ZIP File

1. Download the provided `Launchpad Studio.zip` file.
2. Extract it to a location of your choice (e.g., Desktop or Documents).

After extraction, the folder should look like this:
```
/Launchpad Studio
│── launchpad-studio-1.0.0.jar
│── launchpad-config.json
│── launchpad-config Backup.json
│── /sounds
│── start.bat
```
---
# Prerequisites
## ☕ 2. Install Java (Required)

This application requires the **latest Java SDK (JDK)**.

### Steps:

1. Download the JAVA SDK directly from Oracle:  
   https://www.oracle.com/java/technologies/downloads

2. Download the latest version for your operating system.

3. Run the installer and follow the setup instructions.

### ⚠️ Important

If you installed Java just now:

- Close all open command line / terminal windows
- Open a new one afterward

This ensures Java is properly recognized by your system.

---

## 📂 3. Execute
### ⚠️ Important
Once you have started the program for the first time, it is bound to this location. Do not move the extracted directory anywhere after the first execution as the sounds will stop
working. Do not move any of the files in the extracted directory. Should you need to move the extracted directory, then the contents of `launchpad-config.json` have 
to be replaced with the contents of `launchpad-config Backup.json`
### How to execute
You must execute the application **from inside the extracted folder**.
if you are a windows user double click ```start.bat``` and if not, execute the command ```java -jar .\launchpad-studio-1.0.0.jar``` in a terminal of your choice in the extracted folder


