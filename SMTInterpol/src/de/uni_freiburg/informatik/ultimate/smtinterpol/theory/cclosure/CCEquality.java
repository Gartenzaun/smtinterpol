/*
 * Copyright (C) 2009-2012 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.smtinterpol.theory.cclosure;

import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Theory;
import de.uni_freiburg.informatik.ultimate.smtinterpol.dpll.DPLLAtom;
import de.uni_freiburg.informatik.ultimate.smtinterpol.dpll.NamedAtom;
import de.uni_freiburg.informatik.ultimate.smtinterpol.dpll.SimpleListable;
import de.uni_freiburg.informatik.ultimate.smtinterpol.theory.linar.LAEquality;
import de.uni_freiburg.informatik.ultimate.smtinterpol.util.Coercion;
import de.uni_freiburg.informatik.ultimate.util.HashUtils;

public class CCEquality extends DPLLAtom {
	private final CCTerm mLhs, mRhs;
	CCEquality mDiseqReason;
	private LAEquality mLasd;
	private Rational mLAFactor;
	private final Entry mEntry;
	int mStackDepth = -1;
	
	class Entry extends SimpleListable<Entry> {
		public CCEquality getCCEquality() {
			return CCEquality.this;
		}
	}
	
	CCEquality(int assertionstacklevel, CCTerm c1, CCTerm c2) {
		super(HashUtils.hashJenkins(c1.hashCode(), c2), assertionstacklevel);
		this.mLhs = c1;
		this.mRhs = c2;
		this.mEntry = new Entry();
	}	
	
	public CCTerm getLhs() {
		return mLhs;
	}

	public CCTerm getRhs() {
		return mRhs;
	}
	
	public Entry getEntry() {
		return mEntry;
	}
	
	public LAEquality getLASharedData() {
		return mLasd;
	}
	
	public void setLASharedData(LAEquality lasd, Rational factor) {
		mLasd = lasd;
		mLAFactor = factor;
	}
	
	/**
	 * Returns the linar factor. This is the factor f, such that 
	 * <code>f * (getLhs() - getRhs()) == getLASharedData().getVar()</code>
	 * @return the factor.
	 */
	public Rational getLAFactor() {
		return mLAFactor;
	}

	public void removeLASharedData() {
		mLasd = null;
		mLAFactor = null;
	}
	
	public Term getSMTFormula(Theory smtTheory, boolean quoted) {
		Term lhs = mLhs.toSMTTerm(smtTheory, quoted);
		Term rhs = mRhs.toSMTTerm(smtTheory, quoted);
		Term res = Coercion.buildEq(lhs, rhs);
		return quoted ? smtTheory.annotatedTerm(NamedAtom.QUOTED, res) : res;
	}

	public String toString() {
		return mLhs + " == " + mRhs;
	}
}
