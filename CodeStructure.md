# Overview #
The code is divided into two distinct directory trees:
  * src:  Contains the java source code for the application
  * res:  Contains resources such as XML configuration files describing the GUI, images for icons etc.

Note that this is Graham's interpretation at the moment - please correct anything I have mis-understood!

# Main.java #
Main.java contains the 'Main' class which is the main application 'Activity' class.
The onCreate() method is called when the application is opened to set up the GUI.
Main does not do very much itself directly, instead it uses the 'Logic' class which actually changes things.

# Map.java #
Map.java contains the 'Map' class, which is an android 'View' which deals with drawing the map, including the on-screen statistics etc.

# Logic.java #
Logic.java contains the 'Logic' class which is a link between the various components of Vespucci, and is used to store the application state.