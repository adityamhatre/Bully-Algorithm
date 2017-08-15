# Bully-Algorithm

Bully Algorithm Implementation
-------------------------------------------------------------------------
This is a bully algorithm simulator program. The processes are loaded and whenever the start election button is clicked, the election is initiated, and a coordinator process is elected according to the bully algorithm


Getting Started
--------------------------------------------------------------------------
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.
1. Download the mhatre_9814.zip
2. Unzip the file to get two folders viz., source and binary.
3. Open the binary folder to get the .jar file of the project.
	3a. Run the file to open up the server window
	3b. Rest of the instructions are below
4. Open the source folder to get the project folder which contains the source files of the project.
	4a. Download the IntelliJ IDEA Community Edition IDE to open the project from the Open Menu of the IDE from https://www.jetbrains.com/idea/download/#section=windows
	4b. Make sure it includes the "Kotlin" plugin since the source code is in Kotlin language.

Prerequisites
---------------------------------------------
1. Java 1.8
2. Windows 10

Instructions to run the project
--------------------------------------------
1. Select a process from the drop-down.
2. Select the "Start Election" button to start the election.
3. Wait for the election to finish.
4. The ongoing communication is shown in the log window.
5. Exit the GUI.

Bugs
-----------------------
1. The threads are not synchronized and may sometimes lead to an erratic behaviour.

Assumptions
---------------------------------
1. The maximum number of processes is arbitrarily declared as 3 to make the program memory-safe and to avoid any stack overflow errors.
