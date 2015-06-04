#!/usr/bin/python

from numpy import std, mean

parcels_per_robot = {}
parcel_times = []

with open("parcel_delivery_log.txt",'r') as log:
	for line in log:
		(robotId, arrival_time) = line.split(':')
		parcel_times.append(int(arrival_time))
		if not robotId in parcels_per_robot.keys():
			parcels_per_robot[robotId] = 1
		else:
			parcels_per_robot[robotId] += 1


for robot in sorted(parcels_per_robot.keys()):
	print "%s\t%d" % (robot, parcels_per_robot[robot])

print "%d parcels in %d s. Throughput = %s parcels/min" % (len(parcel_times), max(parcel_times), 60 * float(len(parcel_times)) / (max(parcel_times)))
