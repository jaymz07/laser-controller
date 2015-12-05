Description
--------------------------
Analog signal generator for driving laser galvo.
Allows the laser galvo kits that can be inexpensively purchashed to be controlled by a computer's audio jack.

This program generates X/Y parametric signals from an on-screen drawing through the sound card.
Uses the sound card to generate analog voltages which are suitable, usually after amplification, for driving a laser scanner galvo.
This program also works to generate pictures on an oscilloscope set to X/Y channel mode.

The right channel outputs voltages corresponding to X position, while the left channel gives voltages corresponding to Y position.

Also has the ability to load signals from a text file and output them.

WARNING: This program can generate signals which are very harsh if played though audio speakers. 
	 The signals may even be damaging if played at high volume.
	 Turn system volume almost all the way down if not connected to a laser scanner.

Compile instructions
------------------------
If the make system is installed, the package can be compiled by the command:
make

Otherwise the standard java compiler can be used directly:
javac GraphicalLaserOutput.java



Usage
--------------------------
To run program:
java GraphicalLaserOutput


  User inputs:
  -------------------
  
    Mouse:
      Click or drag to add points to laser path.
    
    Keyboard:
      Backspace --	Remove last point drawn
      C		--	Clear entire drawing
      S		--	Sine wave animation mode
      

Current Features
------------------------------------
 - Able to output drawings as they are being done by user
 - Load a drawing from a file to be output through sound card
 - Rotation of current drawing
 - Generate sine wave animation (very cool if laser scanner is used with a fog machine :D )
 - User controlled speed reduction.

 
Compatability
------------------------------------
 - Should run on anything that supports the Java Runtime Environment
 - Should work with laser scanners up to 44K points/sec (higher can be achieved with higher sample rate audio)
    - Note: this will work regardless if scanner is connected. 
      Since it is attached to computer via audio jack, the computer has no way of knowing if it is actually connected