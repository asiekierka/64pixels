# this should hopefully work --GM

import random

class Dungen:
	# constructor
	def __init__(self, w, h):
		if w < 9 or h < 9:
			raise Exception("map too small, must be at least 9x9, should be more")
			# throw new RuntimeException("map too small, must be at least 9x9, should be more");
		
		self.w = w
		self.h = h
		self.g = [[False for x in xrange(self.w)] for y in xrange(self.h)]
		# g = new boolean[h][w];
		
		self.threshold = (self.w*self.h*40)//100 # aim for 40% roominess
		self.maxarea = 100
		self.cover = 0
		
		self.dorect(self.w//2-3, self.h//2-3, self.w//2+3, self.h//2+3)
		self.scrawl()
		
		self.trim()
	
	# private
	def trim(self):
		# for(int y = 1; y < this.h-1; y++)
		for y in xrange(1,self.h-1,1):
			for x in xrange(1,self.w-1,1):
				if (not self.g[y][x]) and ((self.g[y-1][x] and self.g[y+1][x]) or (self.g[y][x-1] and self.g[y][x+1])):
					self.g[y][x] = True
	
	# private
	def scrawl(self):
		while self.cover < self.threshold:
			x1 = random.randint(1,self.w-2)
			# x1 = (int)(Math.random()*self.w);
			x2 = random.randint(1,self.w-2)
			y1 = random.randint(1,self.h-2)
			y2 = random.randint(1,self.h-2)
			
			if x1 > x2:
				x1, x2 = x2, x1
				# int t = x1;
				# x1 = x2;
				# x2 = t;
			if y1 > y2:
				y1, y2 = y2, y1
			
			w = x2-x1+1
			h = y2-y1+1
			
			#if w <= 1 or h <= 1 or w*h > (self.w*self.h)//16:
			if w <= 1 or h <= 1 or w*h > self.maxarea:
				continue
			
			self.dorect(x1,y1,x2,y2)
	
	# private
	def dorect(self, x1, y1, x2, y2):
		assert (x1 >= 1)
		assert (y1 >= 1)
		assert (x2 < self.w-1)
		assert (y2 < self.h-1)
		
		# check if any of this overlaps what we have - it needs to
		# CHECK CHANGED: checking if we're at the fringe of it, but not in it
		# NOTE: if nothing is covered, skip this check! otherwise we'll never get anywhere
		if self.cover != 0:
			notok = True
			for y in xrange(y1,y2+1,1):
				for x in xrange(x1,x2+1,1):
					if self.g[y][x]:
						return
			
			for y in xrange(y1,y2+1,1):
				if self.g[y][x1-1] or self.g[y][x2+1]:
					notok = False
					break
			
			if notok:
				for x in xrange(x1,x2+1,1):
					if self.g[y1-1][x] or self.g[y2+1][x]:
						notok = False
						break
			
			# didn't overlap? skip it
			if notok:
				return
		
		# OK let's draw.
		for y in xrange(y1,y2+1,1):
			for x in xrange(x1,x2+1,1):
				# don't doublecount anything!
				if not self.g[y][x]:
					self.g[y][x] = True
					self.cover += 1
	
	# private
	# note: just for testing!
	def drawme(self):
		# you're on your own for this one.
		# should be easy enough to do.
		for l in self.g:
			print ''.join(' ' if v else '#' for v in l)

Dungen(64,64).drawme()
