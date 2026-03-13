# CS 4240 Compilers and Interpreters (Spring 2026) Project 2

## Building source files

First, create a directory for the target bytecode to be stored
```
mkdir build
```
Store paths to each source code file in a sources.txt file. Specify the src directory and look for specific file names that end in .java using the find command
```
find src -name "*.java" > sources.txt
```
Compile and specify the target directory (using the `-d` flag) for compiled bytecode .class files and read the contents of sources.txt as command line arguments
```
javac -d build @sources.txt
```

## Running the Optimizer
Specify the classpath (`-cp`) to be the compiled classes just placed into the `build` directory, followed by your main Optimizer class 
- The first command line argument is the path to the file which has the generated IR
- The second command line argument is the path to the file which has the optimized IR
```
java -cp ./build <optimizer file name> <path/to/input.ir> <path/to/output.ir>
```
