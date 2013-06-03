package com.googlecode.javaewah;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Fast algorithms to aggregate many bitmaps. These algorithms are just given as
 * reference. They may not be faster than the corresponding methods in the
 * EWAHCompressedBitmap class.
 * 
 * @author Daniel Lemire
 * 
 */
public class FastAggregation {


	/**
	 * Compute the or aggregate using a temporary uncompressed bitmap.
	 * @param bitmaps the source bitmaps
	 * @return the or aggregate.
	 */
	public static EWAHCompressedBitmap bufferedor(
			final EWAHCompressedBitmap... bitmaps) {
		EWAHCompressedBitmap answer = new EWAHCompressedBitmap();
		bufferedorWithContainer(answer, bitmaps);
		return answer;
	}

	/**
	 * Compute the or aggregate using a temporary uncompressed bitmap.
	 * 
	 * @param container where the aggregate is written
	 * @param bitmaps the source bitmaps
	 */
	public static void bufferedorWithContainer(final BitmapStorage container,
			final EWAHCompressedBitmap... bitmaps) {
		int range = 0;
		EWAHCompressedBitmap[] sbitmaps = bitmaps.clone();
		Arrays.sort(sbitmaps, new Comparator<EWAHCompressedBitmap>() {
			public int compare(EWAHCompressedBitmap a, EWAHCompressedBitmap b) {
				return b.sizeinbits - a.sizeinbits;
			}
		});

		java.util.ArrayList<IteratingBufferedRunningLengthWord> al = new java.util.ArrayList<IteratingBufferedRunningLengthWord>();
		for (EWAHCompressedBitmap bitmap : sbitmaps) {
			if (bitmap.sizeinbits > range)
				range = bitmap.sizeinbits;
			al.add(new IteratingBufferedRunningLengthWord(bitmap));
		}
		final int MAXBUFSIZE = 65536;
		long[] hardbitmap = new long[MAXBUFSIZE];
		int maxr = al.size();
		while (maxr > 0) {
			long effective = 0;
			for (int k = 0; k < maxr; ++k) {
				if (al.get(k).size() > 0) {
					int eff = IteratorAggregation.inplaceor(hardbitmap, al.get(k));
					if (eff > effective)
						effective = eff;
				} else
					maxr = k;
			}
			for (int k = 0; k < effective; ++k)
				container.add(hardbitmap[k]);
			Arrays.fill(hardbitmap, 0);

		}
		container.setSizeInBits(range);
	}
	/**
	 * @param bitmaps
	 *            bitmaps to be aggregated
	 * @return the and aggregate
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends LogicalElement> T and(T... bitmaps) {
		// for "and" a priority queue is not needed, but
		// overhead ought to be small
		PriorityQueue<T> pq = new PriorityQueue<T>(bitmaps.length,
				new Comparator<T>() {
					public int compare(T a, T b) {
						return a.sizeInBytes() - b.sizeInBytes();
					}
				});
		for (T x : bitmaps)
			pq.add(x);
		while (pq.size() > 1) {
			T x1 = pq.poll();
			T x2 = pq.poll();
			pq.add((T) x1.and(x2));
		}
		return pq.poll();
	}

	/**
	 * @param bitmaps
	 *            bitmaps to be aggregated
	 * @return the or aggregate
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends LogicalElement> T or(T... bitmaps) {
		PriorityQueue<T> pq = new PriorityQueue<T>(bitmaps.length,
				new Comparator<T>() {
					public int compare(T a, T b) {
						return a.sizeInBytes() - b.sizeInBytes();
					}
				});
		for (T x : bitmaps) {
			pq.add(x);
		}
		while (pq.size() > 1) {
			T x1 = pq.poll();
			T x2 = pq.poll();
			pq.add((T) x1.or(x2));
		}
		return pq.poll();
	}

	/**
	 * @param bitmaps
	 *            bitmaps to be aggregated
	 * @return the xor aggregate
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends LogicalElement> T xor(T... bitmaps) {
		PriorityQueue<T> pq = new PriorityQueue<T>(bitmaps.length,
				new Comparator<T>() {

					public int compare(T a, T b) {
						return a.sizeInBytes() - b.sizeInBytes();
					}
				});
		for (T x : bitmaps)
			pq.add(x);
		while (pq.size() > 1) {
			T x1 = pq.poll();
			T x2 = pq.poll();
			pq.add((T) x1.xor(x2));
		}
		return pq.poll();
	}
	

	  /**
	   * For internal use. Computes the bitwise or of the provided bitmaps and
	   * stores the result in the container.
	   * 
	   * @deprecated
	   * @since 0.4.0
	   * @param container where store the result
	   * @param bitmaps to be aggregated
	   */
	  public static void legacy_orWithContainer(final BitmapStorage container,
	    final EWAHCompressedBitmap... bitmaps) {
	    if (bitmaps.length == 2) {
	      // should be more efficient
	      bitmaps[0].orToContainer(bitmaps[1], container);
	      return;
	    }

	    // Sort the bitmaps in descending order by sizeinbits. We will exhaust the
	    // sorted bitmaps from right to left.
	    final EWAHCompressedBitmap[] sortedBitmaps = bitmaps.clone();
	    Arrays.sort(sortedBitmaps, new Comparator<EWAHCompressedBitmap>() {
	      public int compare(EWAHCompressedBitmap a, EWAHCompressedBitmap b) {
	        return a.sizeinbits < b.sizeinbits ? 1
	          : a.sizeinbits == b.sizeinbits ? 0 : -1;
	      }
	    });

	    final IteratingBufferedRunningLengthWord[] rlws = new IteratingBufferedRunningLengthWord[bitmaps.length];
	    int maxAvailablePos = 0;
	    for (EWAHCompressedBitmap bitmap : sortedBitmaps) {
	      EWAHIterator iterator = bitmap.getEWAHIterator();
	      if (iterator.hasNext()) {
	        rlws[maxAvailablePos++] = new IteratingBufferedRunningLengthWord(
	          iterator);
	      }
	    }

	    if (maxAvailablePos == 0) { // this never happens...
	      container.setSizeInBits(0);
	      return;
	    }

	    int maxSize = sortedBitmaps[0].sizeinbits;

	    while (true) {
	      long maxOneRl = 0;
	      long minZeroRl = Long.MAX_VALUE;
	      long minSize = Long.MAX_VALUE;
	      int numEmptyRl = 0;
	      for (int i = 0; i < maxAvailablePos; i++) {
	        IteratingBufferedRunningLengthWord rlw = rlws[i];
	        long size = rlw.size();
	        if (size == 0) {
	          maxAvailablePos = i;
	          break;
	        }
	        minSize = Math.min(minSize, size);

	        if (rlw.getRunningBit()) {
	          long rl = rlw.getRunningLength();
	          maxOneRl = Math.max(maxOneRl, rl);
	          minZeroRl = 0;
	          if (rl == 0 && size > 0) {
	            numEmptyRl++;
	          }
	        } else {
	          long rl = rlw.getRunningLength();
	          minZeroRl = Math.min(minZeroRl, rl);
	          if (rl == 0 && size > 0) {
	            numEmptyRl++;
	          }
	        }
	      }

	      if (maxAvailablePos == 0) {
	        break;
	      } else if (maxAvailablePos == 1) {
	        // only one bitmap is left so just write the rest of it out
	        rlws[0].discharge(container);
	        break;
	      }

	      if (maxOneRl > 0) {
	        container.addStreamOfEmptyWords(true, maxOneRl);
	        for (int i = 0; i < maxAvailablePos; i++) {
	          IteratingBufferedRunningLengthWord rlw = rlws[i];
	          rlw.discardFirstWords(maxOneRl);
	        }
	      } else if (minZeroRl > 0) {
	        container.addStreamOfEmptyWords(false, minZeroRl);
	        for (int i = 0; i < maxAvailablePos; i++) {
	          IteratingBufferedRunningLengthWord rlw = rlws[i];
	          rlw.discardFirstWords(minZeroRl);
	        }
	      } else {
	        int index = 0;

	        if (numEmptyRl == 1) {
	          // if one rlw has literal words to process and the rest have a run of
	          // 0's we can write them out here
	          IteratingBufferedRunningLengthWord emptyRl = null;
	          long minNonEmptyRl = Long.MAX_VALUE;
	          for (int i = 0; i < maxAvailablePos; i++) {
	            IteratingBufferedRunningLengthWord rlw = rlws[i];
	            long rl = rlw.getRunningLength();
	            if (rl == 0) {
	              assert emptyRl == null;
	              emptyRl = rlw;
	            } else {
	              minNonEmptyRl = Math.min(minNonEmptyRl, rl);
	            }
	          }
	          long wordsToWrite = minNonEmptyRl > minSize ? minSize : minNonEmptyRl;
	          if (emptyRl != null)
	            emptyRl.writeLiteralWords((int) wordsToWrite, container);
	          index += wordsToWrite;
	        }

	        while (index < minSize) {
	          long word = 0;
	          for (int i = 0; i < maxAvailablePos; i++) {
	            IteratingBufferedRunningLengthWord rlw = rlws[i];
	            if (rlw.getRunningLength() <= index) {
	              word |= rlw.getLiteralWordAt(index - (int) rlw.getRunningLength());
	            }
	          }
	          container.add(word);
	          index++;
	        }
	        for (int i = 0; i < maxAvailablePos; i++) {
	          IteratingBufferedRunningLengthWord rlw = rlws[i];
	          rlw.discardFirstWords(minSize);
	        }
	      }
	    }
	    container.setSizeInBits(maxSize);
	  }
	
}
