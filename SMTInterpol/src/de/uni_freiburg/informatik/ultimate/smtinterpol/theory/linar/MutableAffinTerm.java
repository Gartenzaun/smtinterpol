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
package de.uni_freiburg.informatik.ultimate.smtinterpol.theory.linar;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.MutableRational;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Theory;
import de.uni_freiburg.informatik.ultimate.smtinterpol.convert.SMTAffineTerm;
import de.uni_freiburg.informatik.ultimate.smtinterpol.convert.SharedTerm;
import de.uni_freiburg.informatik.ultimate.smtinterpol.dpll.NamedAtom;



/**
 *  Represents a modifiable affin term, i.e. SUM_i c_i * x_i + c, 
 *  where the x_i are initially nonbasic variable.
 *    
 *  @author hoenicke.
 */
public class MutableAffinTerm {
	TreeMap<LinVar, Rational> mSummands =
		new TreeMap<LinVar, Rational>();
	InfinitNumber mConstant;
	
	public MutableAffinTerm() {
		mConstant = InfinitNumber.ZERO;
	}

	public MutableAffinTerm add(Rational c) {
		mConstant = mConstant.add(new InfinitNumber(c, 0));
		return this;
	}
	
	public MutableAffinTerm add(InfinitNumber c) {
		mConstant = mConstant.add(c);
		return this;
	}
	public MutableAffinTerm add(Rational c, SharedTerm term) {
		if (c.equals(Rational.ZERO))
			return this;
		if (term.getTerm() instanceof SMTAffineTerm)
			add(c, term.getClausifier().createMutableAffinTerm(term));
		else
			addSimple(c, term.getLinVar());
		return this;
	}
	public MutableAffinTerm add(Rational c, LinVar var) {
		if (c.equals(Rational.ZERO))
			return this;
		if (var.isInitiallyBasic()) {
			for (Map.Entry<LinVar, BigInteger> me : var.getLinTerm().entrySet())
				add(c.mul(me.getValue()), me.getKey());
		} else {
			addSimple(c, var);
		}
		return this;
	}

	private void addMap(Rational c, Map<LinVar, Rational> linterm) {
		for (Map.Entry<LinVar, Rational> summand : linterm.entrySet())
			addSimple(c.mul(summand.getValue()), summand.getKey());
	}
	
	private void addSimple(Rational c, LinVar term) {
		assert (/*!term.getLinVar().isInitiallyBasic() &&*/ !c.equals(Rational.ZERO));
		Rational oldc = mSummands.remove(term);
		if (oldc != null) {
			c = oldc.add(c);
			if (c.equals(Rational.ZERO))
				return;
		}
		mSummands.put(term,c);
	}

	public MutableAffinTerm add(Rational c, MutableAffinTerm a) {
		if (c != Rational.ZERO) {
			addMap(c, a.mSummands);
			mConstant = mConstant.add(a.mConstant.mul(c));
		}
		return this;
	}

	public MutableAffinTerm mul(Rational c) {
		if (c.equals(Rational.ZERO))
			mSummands.clear();
		else if (!c.equals(Rational.ONE)) {
			for (Map.Entry<LinVar, Rational> summand : mSummands.entrySet()) {
				summand.setValue(c.mul(summand.getValue()));
			}
			mConstant = mConstant.mul(c);
		}
		return this;
	}
	
	public MutableAffinTerm div(Rational c) {
		return mul(c.inverse());
	}
	public MutableAffinTerm negate() {
		return mul(Rational.MONE);
	}
	public boolean isConstant() {
		return mSummands.isEmpty();
	}
	public InfinitNumber getConstant() {
		return mConstant;
	}
	public TreeMap<LinVar,Rational> getSummands() {
		return mSummands;
	}
	
	public Rational getGCD() {
		assert (!mSummands.isEmpty());
		Iterator<Rational> it = mSummands.values().iterator();
		Rational gcd = it.next(); 
		boolean firstSign = gcd.isNegative();
		gcd = gcd.abs();
		while (it.hasNext()) {
			gcd = gcd.gcd(it.next().abs());
		}
		if (firstSign)
			gcd = gcd.negate();
		return gcd;
	}
	
	/**
	 * For integer valued interpolants, convert Rationals to integer valued
	 * Rational by dividing by the rational greatest common divisor.
	 */
	void normalize() {
		mul(getGCD().inverse());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		for (Entry<LinVar,Rational> entry : mSummands.entrySet()) {
			LinVar var = entry.getKey();
			Rational fact = entry.getValue();
			if (fact.isNegative()) {
				sb.append(isFirst ? "-" : " - ");
			} else {
				sb.append(isFirst ? "" : " + ");
			}
			fact = fact.abs();
			if (!fact.equals(Rational.ONE))
				sb.append(fact).append('*');
			sb.append(var);
			isFirst = false;
		}
		if (isFirst)
			sb.append(mConstant);
		else {
			int signum = mConstant.compareTo(InfinitNumber.ZERO); 
			if (signum < 0) {
				sb.append(" - ");
				sb.append(mConstant.mul(Rational.MONE));
			} else if (signum > 0) {
				sb.append(" + ");
				sb.append(mConstant);
			}
		}
		return sb.toString();
	}
	
	public Sort getSort(Theory t) {
		return isInt() ? t.getSort("Int") : t.getSort("Real");
	}

	/**
	 * Convert the affine term to a term in our core theory.
	 * @param useAuxVars use auxiliary variables for non-variable terms (unimplemented).
	 */ 
	public Term toSMTLib(Theory t, boolean isInt, boolean quoted) {
		Sort numSort = isInt ? t.getSort("Int") : t.getSort("Real");
		assert(numSort != null);
		Sort[] binfunc = new Sort[] {numSort,numSort};
		FunctionSymbol times = t.getFunction("*",binfunc);
		FunctionSymbol plus = t.getFunction("+",binfunc);
		FunctionSymbol negate = t.getFunction("-", numSort);
		if (negate == null)
			negate = t.getFunction("-", numSort);
		assert (!isInt || mConstant.mA.isIntegral());
		Term constTerm = mConstant.mA.equals(Rational.ZERO) ? null 
			: isInt ? t.numeral(mConstant.mA.numerator())
			: t.rational(mConstant.mA.numerator(), mConstant.mA.denominator());
		Term[] terms = new Term[mSummands.size() + (constTerm == null ? 0 : 1)];
		if (constTerm != null)
			terms[mSummands.size()] = constTerm;
		int offset = 0;
		for (Map.Entry<LinVar,Rational> me : mSummands.entrySet()) {
			LinVar lv = me.getKey();
			Term convme = lv.getSharedTerm().getRealTerm();
			// if affine term is integral it may only add integers.
			assert (!isInt || lv.isInt());
			assert (!isInt || me.getValue().isIntegral());
			if (!isInt && lv.isInt()) {
				Sort intSort = t.getSort("Int");
				FunctionSymbol toReal = t.getFunction("to_real", intSort);
				convme = t.term(toReal, convme);
			}
			if (me.getValue().equals(Rational.MONE)) {
				convme = t.term(negate, convme);
			} else if (!me.getValue().equals(Rational.ONE)) {
				Term convfac = isInt ? t.numeral(me.getValue().numerator())
						: t.rational(me.getValue().numerator(),me.getValue().denominator());
				convme = t.term(times, convfac, convme);
			}
			terms[offset++] = convme;
		}
		if (terms.length == 0)
			return isInt ? t.numeral(BigInteger.ZERO) : t.rational(BigInteger.ZERO, BigInteger.ONE);
		else if (terms.length == 1)
			return terms[0];
		else
			return t.term(plus, terms);
	}
	
	public Rational getValue(LinArSolve linar) {
		assert mConstant.mEps == 0;
		MutableRational val = new MutableRational(mConstant.mA);
		for (Map.Entry<LinVar, Rational> me : mSummands.entrySet()) {
			val.add(me.getValue().mul(linar.realValue(me.getKey())));
		}
		return val.toRational();
	}
	
	public boolean isInt() {
		for (LinVar v : mSummands.keySet())
			if (!v.isInt())
				return false;
		return true;
	}
	
	/**
	 * Create the SMTLib formula for the term <code>this <= 0</code>.
	 * @param useAuxVars use auxiliary variables for non-variable terms (unimplemented).
	 * @param t   Theory used in conversion.
	 * @return The SMTLib term representing the formula <code>this <= 0</code>.
	 */
	public Term toSMTLibLeq0(Theory smtTheory, boolean quoted) {
		assert mConstant.mEps >= 0;
		if (isConstant()) {
			return mConstant.compareTo(InfinitNumber.ZERO) <= 0 
				? smtTheory.mTrue : smtTheory.mFalse;
		}
		boolean isInt = isInt();
		String comp = mConstant.mEps == 0 ? "<=" : "<";
		Term zero = isInt ? smtTheory.numeral(BigInteger.ZERO)
				: smtTheory.decimal(BigDecimal.ZERO);
		Term res = smtTheory.term(comp, toSMTLib(smtTheory, isInt, quoted), zero);
		return quoted ? smtTheory.annotatedTerm(NamedAtom.QUOTED, res) : res;
	}
}
