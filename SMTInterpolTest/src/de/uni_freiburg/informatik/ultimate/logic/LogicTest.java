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
package de.uni_freiburg.informatik.ultimate.logic;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Theory;

@RunWith(JUnit4.class)
public class LogicTest {
	
	@Test
	public void testLIRA() {
		Theory theory = new Theory(Logics.AUFLIRA);
		Sort sortInt   = theory.getSort("Int");
		Sort sortReal  = theory.getSort("Real");
		Assert.assertNull(theory.getFunction("-"));
		FunctionSymbol minusInt1 = theory.getFunction("-", sortInt);
		FunctionSymbol minusInt2 = theory.getFunction("-", sortInt, sortInt);
		Assert.assertNotNull(minusInt1);
		Assert.assertNotNull(minusInt2);
		Assert.assertSame(minusInt2,
				theory.getFunction("-", sortInt, sortInt, sortInt));
		Assert.assertNull(theory.getFunction("+"));
		Assert.assertNull(theory.getFunction("+", sortInt));
		FunctionSymbol plusInt2 = theory.getFunction("+", sortInt, sortInt);
		Assert.assertNotNull(plusInt2);
		Assert.assertSame(plusInt2,
				theory.getFunction("+", sortInt, sortInt, sortInt));

		FunctionSymbol minusReal1 = theory.getFunction("-", sortReal);
		FunctionSymbol minusReal2 = theory.getFunction("-", sortReal, sortReal);
		Assert.assertNotNull(minusReal1);
		Assert.assertNotNull(minusReal2);
		Assert.assertSame(minusReal2,
				theory.getFunction("-", sortReal, sortReal, sortReal));

		Assert.assertNull(theory.getFunction("+", sortReal));
		FunctionSymbol plusReal2 = theory.getFunction("+", sortReal, sortReal);
		Assert.assertNotNull(plusReal2);
		Assert.assertSame(plusReal2,
				theory.getFunction("+", sortReal, sortReal, sortReal));
		Assert.assertSame(plusReal2,
				theory.getFunction("+", sortReal, sortInt, sortReal));
		Assert.assertSame(plusReal2,
				theory.getFunction("+", sortInt, sortInt, sortReal));
		
		Term x =
				theory.term(theory.declareFunction("x", new Sort[0], sortReal));
		Term y =
				theory.term(theory.declareFunction("y", new Sort[0], sortReal));
		Term i =
				theory.term(theory.declareFunction("i", new Sort[0], sortInt));
		Term j = theory.term(theory.declareFunction("j", new Sort[0], sortInt));
		Term sum = theory.term("+", x, y, i, j);
		Term mul = theory.term("*",
				theory.rational(new BigInteger("-3"), new BigInteger("7")), i);
		Assert.assertEquals("(+ x y i j)", sum.toString());
		Assert.assertEquals("(* (/ (- 3) 7) i)", mul.toString());
	}

	private Sort bitvec(Theory theory, int len) {
		return theory.getSort("BitVec", 
				new BigInteger[] { BigInteger.valueOf(len) });
	}
	
	@Test
	public void testBV() {
		Theory theory = new Theory(Logics.QF_BV);
		Term bvABCD = theory.hexadecimal("#xABCD");
		Term bv1111 = theory.binary("#b1111");
		Assert.assertEquals(bitvec(theory, 16), bvABCD.getSort());// NOCHECKSTYLE
		Assert.assertEquals(bitvec(theory, 4), bv1111.getSort());// NOCHECKSTYLE
		Term bv2 = theory.term("concat", bvABCD, bv1111);
		Assert.assertEquals(bitvec(theory, 20), bv2.getSort());// NOCHECKSTYLE
	}
}
