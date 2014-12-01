


This is the repo for the Android version of Ziftr's mobile wallet app.

Please note that while we make the source publicly available for a variety of reasons, this project is NOT "open source."
<insert licensing notes>'




-------- Project Setup --------

Requirements:

    Maven and Ant are used for easy dependency management but are not used for Eclipse project setup or builds.

    If using Eclipse, M2_REPO Classpath variable must be set.
        In Eclipse->Preferences->Java->Build Path->Classpath Variables
        Add a new classpath variable that has name M2_REPO and value to be
        where ever the .m2/repository folder is on your machine. For me it was
        /Users/stephenmorse/.m2/repository.


Setup:

    Download this repo into a local folder.

    In Eclipse, do File->Import...
        Choose 'Existing Projects Into Workspace'
        Click the "Browse..." button in the upper right next to "Select root directory"
        Browse to the folder which contains this source and click finish
        When the list of projects to import is displayed click "Deselect All"
        Now click to check box to import the following projects:
            ZiftrWallet
            ZWCaptureActivity
            android-support-v7-appcompat

    In the ZiftrWallet project right-click build_dependencies.xml and select Run As->Ant Build.
        If Ant is not installed or configured correctly, you can also simply use maven and the projects pom.xml via
        "mvn dependency:resolve"

    You should now be able to Run/Debug the ZiftrWallet project.



-------- Possible Setup Issues --------

    ZWCaptureActivity isn't an option when importing projects.
        This project depends on the zxing submodule.
        You may need to manually update/pull the zxing module.
        Make sure there is a zxing folder locally and it contains files.
            If it is empty, do a git pull, checkout, or clone from within the zxing folder
            Then attempt the project import again.


    The project has many errors.
        This can happen when the project is missing (or can't find) required source files or projects.
        See next issue.


    The project says it is missing a dependency, source folder, or sub project.
        First make sure that Maven correctly downloaded the dependencies and that the ZiftrWallets project is correctly referencing them.
            Right-click the ZiftrWallet project. Select properties. Select "Java Build Path." Select the Libraries tab. 
            Make sure the M2_REPO variable is working correctly and that the libraries are actually in the right place.
        It's also possible that eclipse envirenment variables have become removed and the project is not correctly using relative paths.
            Right-click the ZiftrWallet project. Select properties. Select "Android." 
            Make sure both the support library and the ZWCaptureActivity library are there and have green check marks.
                If not, you may need to find and add them manually.
        If the ZWCaptureActivy project is showing errors it may be missing it's zxing dependencies.
            Right-click ZWCaptureActivity. Select properties. Select "Java Build Path." Select the source tab.
            There should be two additional source folders, android-core and core.
                If those folders are missing or have errors, correct them and clean the project.
        
	
	
