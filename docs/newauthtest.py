# -*- coding: utf-8 -*-

TESTDATA = [
	[197, 110, 6, 217, 163, 191, 232, 201, 206, 33, 94, 3, 126, 221, 153, 80, 123, 86, 250, 213, 122, 124, 8, 23, 250, 184, 74, 154, 100, 16, 41, 117],
	[169, 205, 102, 160, 131, 3, 1, 9, 237, 32, 168, 154, 12, 88, 75, 244, 24, 26, 180, 254, 64, 233, 254, 87, 10, 154, 226, 47, 56, 21, 121, 138],
	[83, 181, 51, 197, 83, 213, 85, 113, 200, 40, 78, 77, 112, 155, 213, 178, 131, 143, 11, 188, 158, 250, 108, 87, 223, 100, 242, 126, 165, 27, 20, 84],
]

TESTPASSWORDS = [
	"a",
	"c",
	"ab",
	"abC",
	"adC",
	"watermelon",
]

for dataCopy in TESTDATA:
	print "Testing %s:" % ''.join("%02X" % d for d in dataCopy)
	for pw in TESTPASSWORDS:
		data = dataCopy[:]
		si = 0
		fi = 1
		pw = pw * 5
		for b in xrange(len(pw)):
			pwb = ord(pw[b])
			for q in xrange(8):
				if pwb&(1<<q):
					data[si], data[(si+b+1)&31] = data[(si+b+1)&31], data[si]
				else:
					data[si] += data[(si+b+1)&31]
				
				si = (si + (b+2)) & 31
			
			si = (si + 1) & 31
			
			# This part is tricky.
			odata = data[:]
			c = 0
			for i in xrange(32):
				k = (
					data[i]
					+ ((odata[(i+(fi>>3))&31]>>(fi&7))&0xFF)
					+ ((odata[(i+1+(fi>>3))&31]<<(8-(fi&7)))&0xFF)
					+ c
				)
				data[i] = k & 0xFF
				c = k >> 8
			
			fi += b + 1
		
		print "%s: %s" % (''.join("%02X" % d for d in data), pw)
		print "| %s" % ''.join(("%02X" % (data[i]^dataCopy[i])) for i in xrange(32))
		