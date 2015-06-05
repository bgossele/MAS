#!/usr/bin/python

from numpy import std, mean
from os import listdir
from os.path import isfile, join

throughputs = {}

files = [f for f in listdir('logs') if f.endswith('.parcels')]

for f in files:
	with open(join('logs', f),'r') as log:
		parcel_times = []
		tag = ''
		for line in log:
			if 'dummy' in f:
				(tag, n_parcels, robotId, arrival_time) = line.split(';')
			else:
				(tag, robotId, arrival_time) = line.split(';')
			parcel_times.append(int(arrival_time))
		try:
			throughput = 60 * float(len(parcel_times)) / (max(parcel_times))	
			if 'dummy' in f:
				throughput *= (len(parcel_times) / float(n_parcels))
		except ValueError:
			throughput = 0

		if not tag in throughputs.keys():
			throughputs[tag] = [throughput]
		else:
			throughputs[tag].append(throughput)

for exp in throughputs.keys():
	print "%s\t%.2f ; %.2f" % (exp, mean(throughputs[exp]), std(throughputs[exp]))
