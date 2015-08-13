SuperNet java agent

Libraries:

bcprov-jdk15on-151.jar

jna-4.1.0.jar

jna-platform-4.1.0.jar

json-simple-1.1.1.jar

How to build

Linux:

- ./compile_native.sh
- ./compile.sh
- Fix run.sh and echodemo to use valid path (will be fixed soon)

Windows:

- Make sure cmake and Visual Studio is installed on the system
- Add path to Visual Studio cl.exe eg D:\Microsoft Visual Studio 11.0\VC\bin\ to system PATH variable
- Run compile_native_win32.bat
- Build nanomsg:
  - git clone https://github.com/nanomsg/nanomsg.git
  - mkdir build
  - cd build
  - cmake ..
  - open nanomsg.sln and build the project with Visual Studio
- Copy nanomsg.dll to lib/win32-x86

Or simply copy all dlls from prebuilt/win32 to lib/win32-x86...

- Compile Java files (make sure JDK 1.7 or higher is installed and in the PATH variable): compile_win32.bat

Or use Eclipse IDE

