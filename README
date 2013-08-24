This utility is designed to format the C/C++ source code using the formatting tool built into eclipse CDT (CCodeFormatter).

###############################################################################

How to use?

Just run command with list of files or dir names, see examples:
CCodeFormatterApp.cmd -config org.eclipse.cdt.core.prefs file.c
CCodeFormatterApp.cmd -config org.eclipse.cdt.core.prefs dir_name

Or if you won't use windows batch-files or you use linux, you can run:
java -jar CCodeFormatterApp.jar -config org.eclipse.cdt.core.prefs {list of files or folders}


###############################################################################

Where to get config file org.eclipse.cdt.core.prefs?

Generating a Config File for the Formatter Application:
Generating a config file for the formatter application involves
modifying the code formatter settings for a C/C++ project and
copying org.eclipse.cdt.core.prefs out of the .settings directory for that project.

Select a C/C++ project, open the pop-up menu and choose Properties.
Select the "C/C++ General" > "Code Style" page and check "Enable project specific settings".
Select or edit a profile.
Click OK when you are done.
Use either a file manager or the command line
to copy workspace/YourProject/.settings/org.eclipse.cdt.core.prefs to a new location.

###############################################################################

OPTIONS:
   -help                Display help message.
   -quiet               Only print error messages.
   -verbose             Be verbose about the formatting job.

###############################################################################

How to build?

1. Download and install "Eclipse IDE for Java Developers"
2. Download source code of Eclipse CDT 8.2.0 from http://git.eclipse.org/c/cdt/org.eclipse.cdt.git/
3. Copy source code of EclipseCCodeFormatter to appropriate directory (or perform Import in Eclipse Project Explorer).
4. In Eclipse Project Explorer select org.eclipse.cdt.core -> utils -> org.eclipse.cdt.utils.CCodeFormatter
5. Perform Export: Java -> Runnable JAR file.
