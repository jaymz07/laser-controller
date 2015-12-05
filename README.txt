Description
--------------------------
Analog signal generator for driving laser galvo.

Program for generating X/Y parametric signals from an on-screen drawing through the sound card.
Uses the sound card to generate analog voltages which are suitable, after amplification, for driving a laser scanner galvo.
Also works to generate pictures on an oscilloscope set to X/Y channel mode.

The right channel outputs voltages corresponding to X position, while the left channel gives voltages corresponding to Y position.

Also has the ability to load signals from a text file and output them.


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