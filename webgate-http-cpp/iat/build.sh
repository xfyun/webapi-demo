#!/bin/bash

g++ -Iinclude -Llib  main.cpp -ljsoncpp -lcurl -lssl -lcrypto  -std=c++11

export LD_LIBRARY_PATH=./:./lib

./a.out
