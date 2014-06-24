Requirements:
	1 - Maven - Java project management command line tools. Easily installed with home brew
    	a quick google will give homebrew instructions.
	2 - Eclipse with Android tools installed.

Clone the Repo from Ziftr Github
	One way to do this is to navigate to Ziftr github / OneWallet and
	click Clone to Desktop
	`cd OneWallet`
	`git submodule update --init`
	`cd bitcoinj`
	`git checkout ziftr`

-------- To get bitcoinj projects in eclipse --------

Open up a terminal
	cd to the OneWallet/bitcoinj directory
	run 'mvn eclipse:eclipse' (Will take a minute or two)

In Eclipse, do File->Import...
	Choose 'Existing project into workspace'
	Click the button to choose a folder to import from
	Open the OneWallet/bitcionj folder - these
	
Set bitcoinj project to use Java 6
	Select project
	Cmd-i to open project properties (or right click and select properties)
	Select Library tab
	Click Add Library
	Select JRE System Library
	Click 'Alternate JRE' and Choose Java SE 6
	Click Finish
	Remove any other Java Libraries being used

Set the M2_REPO Classpath variable
	In Eclipse->Preferences->Java->Build Path->Classpath Variables
	Add a new classpath variable that has name M2_REPO and value to be
	where ever the .m2/repository folder is on your machine. For me it was
	/Users/stephenmorse/.m2/repository.

At this point, the 3 bitcoinj projects shouldn't have any errors.

-------- To get OneWallet projects in eclipse --------

Do the above first, as the OneWallet Project references the bitcoinj project.

In Eclipse, do File->Import...
	Choose 'Existing project into workspace'
	Import the HOME/android-sdks/extras
	Make sure only the appcompat project is imported, you don't need all three
	This project shouldn't have any errors
	
In Eclipse, do File->Import...
	Choose 'Existing android project into workspace' (notice the android)
	Select the OneWallet folder which was cloned from Github and click finish
	Right click on the build_dependencies.xml file and do Run As -> Ant Build

For the OneWallet App:
	Go into project properties-> Android
	Remove the Appcompat library (bottom right)
	Click add and select the appcompat Project previously imported
	
	
