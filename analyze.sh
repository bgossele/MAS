#!/bin/bash

clear
echo "throughput"

python analyze_throughput.py

echo
echo "distance covered"

python analyze_distance.py
