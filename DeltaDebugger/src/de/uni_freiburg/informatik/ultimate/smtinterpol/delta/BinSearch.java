/*
 * Copyright (C) 2012-2013 University of Freiburg
 *
 * This file is part of SMTInterpol.
 *
 * SMTInterpol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SMTInterpol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SMTInterpol.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.uni_freiburg.informatik.ultimate.smtinterpol.delta;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;

public class BinSearch<E> {

	public static interface Driver<E> {
		public void prepare(List<E> sublist);
		public void failure(List<E> sublist);
		public void success(List<E> sublist);
	}
	
	private static class IntPair {
		final int mFirst;
		final int mSecond;
		public IntPair(int f, int s) {
			mFirst = f;
			mSecond = s;
		}
	}
	
	private final List<E> mList;
	private final Driver<E> mDriver;
	private final ArrayDeque<IntPair> mTodo;
	
	public BinSearch(List<E> list, Driver<E> driver) {
		mList = list;
		mDriver = driver;
		mTodo = new ArrayDeque<IntPair>();
	}
	
	public boolean run(Minimizer tester)
		throws IOException, InterruptedException {
		if (mList.isEmpty())
			return false;
		boolean result = false;
		mTodo.push(new IntPair(0, mList.size()));
		while (!mTodo.isEmpty()) {
			IntPair p = mTodo.pop();
			List<E> sublist = mList.subList(p.mFirst, p.mSecond);
			if (sublist.isEmpty())
				continue;
			mDriver.prepare(sublist);
			if (tester.test()) {
				mDriver.success(sublist);
				result = true;
			} else {
				mDriver.failure(sublist);
				// Split into two new sublists
				int mid = (p.mFirst + p.mSecond) / 2;
				if (mid == p.mFirst)
					continue;
				mTodo.push(new IntPair(mid, p.mSecond));
				mTodo.push(new IntPair(p.mFirst, mid));
			}
		}
		return result;
	}

}
