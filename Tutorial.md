# Vespucci Quick Start Guide #
(Updated for version 0.9.4)

## Installation ##

Just search for "vespucci" on the android market place and install it. Vespucci 0.9 is known to work with devices running Android 2.3 and later, 2.2 has been tested on an emulator. Earlier versions have not been tested. Pre-2.0 Android versions are not supported.

## Initial Set-Up ##

On start up Vespucci will show you the "Download other location"/"Load Area" dialog. If you have coordinates displayed and want to download immediately, you can select the appropriate option and set the radius around the location that you want to download, otherwise you can simply go to the map and zoom and pan to the area you want to edit. Do not select a large area on slow devices.

Alternatively you can dismiss the dialog by pressing the back button and pan and zoom to a location you want to edit and download the data then (see below: "Editing Using Vespucci").

#### Settings that you might want to change ####

  * Background layer
  * Overlay layer (note adding an overlay layer is not recommended on older devices with a small amount of memory).
  * Notes display (open Notes will be displayed as a red filled circle, closed Notes the same in blue). Default: off.
  * Node icons. Default: off.
  * Keep screen on. Default: off.
  * Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.
  * "Map profile": the default rendering of elements and the touch areas is suitable for a medium pixel density display. You may need to either switch to one of the other "pre-made" profiles (for example to the "color round nodes hires" profile) or customize one yourself, if you have a device with different display parameters or if you want to add special rendering for specific elements.

Advanced preferences
  * Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
  * Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).


## Using GPS ##

You can use Vespucci to create a GPX track and display it on your device. Further you can display the current GPS position (set "Show location" in the GPS menu) and/or have the screen center around and follow the position (set "Follow GPS Position" in the GPS menu). If you have the later set
moving the screen manually or editing will cause the follow mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch the arrow.

## Editing with Vespucci ##
Depending on screen size and age of your device editing actions may either be accessible directly via icons in the top bar, via a drop down menu on the right of the top bar, or via the menu key.

### Downloading OSM Data ###

Select either the transfer icon (up- and down arrows) or the "Transfer" menu item. This will display four options:

  * Download current view
  * Download other location
  * Upload data to OSM server
  * Export changes

"Download current view" will do what it says, "Download other location" will allow you to use GPS, manually entered coordinates or such obtained from Nominatim by an online search to select the area to be downloaded.

Vespucci should download data for the area and center the map on your current location. No authentication is required for downloading data to your device.

Zoom in using gestures, the zoom buttons or the volume control buttons on the telephone.

### Editing ###

To avoid accidental edits Vespucci 0.9 will start up in "locked" mode, a mode that only allows zooming and moving the map. Pressing the red lock icon will "unlock" Vespucci and allow editing. Note: the old drop down menu can be enabled from the Preferences.

The default editing mode is now what used to be called [EasyEdit](EasyEdit.md) mode. With the default settings nodes and ways that are selectable will have an orange area around them indicating roughly where you have to touch to select an object. If you try to select an object and Vespucci determines that the selection could mean multiple object it will present a selection menu. Selected objects are highlighted in yellow.

Vespucci has a good "undo/redo" system so don't be afraid of experimenting on your device, however please do not upload and save pure test data.

#### Selecting / De-selecting ####

A single click on an object will select the object and highlight it, a second click on the same object will open the tag editor on the element. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else simply touch the object in question, there is no need to de-select first.

#### Adding a new Node/Point or Way ####

Long press where you want the node to be or the way to start. You will see a black "cross hairs" symbol, touching the same location again will create a new node, touching a location outside of the touch tolerance zone will add a way segment from the original position to the current position.
Simply touch the screen where you want further nodes of the way to be created and to finish touch the final node twice. If the initial node is located on a way, the node will be inserted in to the way automatically.

#### Improving Way Geometry ####

If you zoom in far enough you will see a small "x" in the middle of way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note to avoid accidental creation of nodes the touch tolerance for this operation is fairly small.

#### Cut, Copy & Paste ####

Selected nodes and ways can be copied or cut and then pasted one or multiple times to a new location. Cutting will retain the osm id and version. To paste long click the location you want to paste to, you will see a cross hair symbol marking the location, select "Paste" from the menu.

#### Efficiently Adding Addresses ####

The tag editor has a repeat last tag function which will merge the tags of the last objects tags displayed in the tag editor with the current ones. Example use: add address tags to first building on a street, continue a long the street using the last tags function to add the address and then edit the house number.

#### Adding Turn Restrictions ####

Vespucci 0.9 has a fast way to add turn restrictions. Note: if you need to split a way for the restriction you need to do this before starting.

  * select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, pleas use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
  * select "Add restriction" from the menu
  * select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
  * select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no\_u\_turn restriction)
  * set the restriction type in the tag menu

### Saving Your Changes ###

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci 0.9 supports OAuth authorization besides the classical username and password method. OAuth is preferable, particularly for mobile applications since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website will be loaded. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. Once you have done that you will be returned to Vespucci and should retry the upload, which now should succeed.

### Resolving conflicts on uploads ###

Currently Vespucci doesn't have a built in conflict resolver. If you do get a conflict on upload, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and upload them with JOSM.