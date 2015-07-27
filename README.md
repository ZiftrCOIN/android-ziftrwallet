


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

    When you are done, run update_index.sh so that your local project files and API key files do not get pushed to github.

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




-------- Developing Against Your Local Devenv --------

	Clone and setup https://github.com/Ziftr/devenv according to that project's README:
		Use the `--with-coin=bitcoin --regtest --with-fpa` options for ./shell/up
		After devenv is setup
		Go into ./shell/db and run:
			USE sandbox;
			DELETE FROM blockchains;
			INSERT INTO blockchains (id, name, type, chain, height, p2pkh_byte, p2sh_byte, priv_byte, recommended_confirmations, default_fee_per_kb, default_fee_per_kb_divisor, seconds_per_block_generated, created_at, updated_at, is_enabled, health, hd_id) VALUES (1, 'Bitcoin Regtest', 'btc', 'testnet3', 0, 111, 196, 239, 3, 10000, 100000000, 600, NOW(), NOW(), 0, "ok", 100);

	When your local machine is setup to accept API requests:
		We first need to forward API requests to the boot2docker VM:
		Make sure the Mobile phone and your computer are on the same Wifi network
		Run `docker ps -a | grep ziftr_fpa` to see what the port forwarding between boot2docker and your docker containers is
			Example: deafbab887ed        fpa-api:latest                   "/etc/init.d/docker_   28 minutes ago      Up 28 minutes       0.0.0.0:8080->3000/tcp, 0.0.0.0:8443->8443/tcp		ziftr_fpa
			The important information from this line is that 3000 is the API port of the docker container, and 8080 is the boot2docker VM's port
		Run `brew install socat` (if you don't already have it. socat is a port forwarding utility for OS X)
		Get your `boot2docker ip`:
			Example: 192.168.59.103
		To forward 8080 on our local machine to the boot2docker VM's port 8080 (assuming your machine isn't already using 8080, use something else if it is):
			`socat TCP-LISTEN:8080,fork TCP:192.168.59.103:8080`
		Leave socat running, and open a new terminal shell to continue

	Start the wallet in debug mode with your local IP:
		Hold down the alt key and hit the wifi icon in the Mac Bar at the top of the screen
		Right underneath 'Disconnect from Ziftr-Corporate', it will tell you your IP address
			Example: `IP Address: 192.168.80.215`
		Start the app, go to settings and Enable debug mode.
		Click the "Change API server button"
			Enter '{your ip}:8080'
		Press continue
		Close the app (full swipe-right close) and then re-start it
		Click add coin and you should see Bitcoin Regtest listed as an option








