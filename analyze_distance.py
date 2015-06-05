#!/usr/bin/python

from numpy import std, mean
from os import listdir
from os.path import isfile, join

total_distances = {}
parcel_distances = {}
ca_distances = {}

files = [f for f in listdir('logs') if f.endswith('.distances')]

for f in files:
	with open(join('logs', f),'r') as log:
		tag = ""
		robot_dist = {}
		parcel_dist = {}
		ca_dist = {}
		for line in log:
			(tag, robotId, timestamp, dist, parcel) = line.split(';')
			
			fdist = float(dist)
			if not robotId in robot_dist.keys():
				robot_dist[robotId] = fdist
				parcel_dist[robotId] = 0
				ca_dist[robotId] = 0
			else:
				robot_dist[robotId] += fdist
	
			if int(parcel) == 1:
				parcel_dist[robotId] += fdist
			else:
				ca_dist[robotId] += fdist
		if not tag in total_distances.keys():
			total_distances[tag] = [sum(robot_dist.values())]
			parcel_distances[tag] = parcel_dist.values()
			ca_distances[tag] = ca_dist.values()
		else:
			total_distances[tag].append(sum(robot_dist.values()))
			parcel_distances[tag].extend(parcel_dist.values())
			ca_distances[tag].extend(parcel_dist.values())


for exp in total_distances.keys():
	print "%s\t%.2f;%.2f \t %.2f;%.2f \t %.2f;%.2f" % (exp,mean(total_distances[exp]), std(total_distances[exp]), mean(parcel_distances[exp]), std(parcel_distances[exp]), mean(ca_distances[exp]), std(ca_distances[exp]))
