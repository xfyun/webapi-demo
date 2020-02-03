#/bin/bash

currentDir := $(shell pwd)
$(info ${currentDir})
export LD_LIBRARY_PATH=${currentDir}:${currentDir}/lib
 g++ -Iinclude -Llib  main.cpp -ljsoncpp -lcurl -lssl -lcrypto  -std=c++11
./a.out
