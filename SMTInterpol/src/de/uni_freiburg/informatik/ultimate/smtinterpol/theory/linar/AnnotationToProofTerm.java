/*
 * Copyright (C) 2009-2013 University of Freiburg
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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Theory;
import de.uni_freiburg.informatik.ultimate.smtinterpol.dpll.Literal;

/**
 * Class that generates a proof term for a LAAnnotation.
 * This is called by LAAnnotation.toTerm().
 * 
 * @author Jochen Hoenicke
 */
public class AnnotationToProofTerm {
	private static final Annotation TRICHOTOMY =
		new Annotation(":trichotomy", null);

	/**
	 * For each (sub-)annotation we store a bit of information needed for
	 * the conversion process.
	 */
	class AnnotationInfo {
		/**
		 * Number of times this annotation is referenced in other annotation.
		 * This is one for the base annotation.
		 */
		int  mCount;
		/**
		 * Number of times this annotation was visited in the conversion 
		 * process.  Only when it is visited for the last time, we do the
		 * actual conversion.
		 */
		int  mVisited;
		/**
		 * SMT representation of the bound explained by this sub-annotation.
		 * This is null for the base annotation.
		 */
		Term mLiteral;
		/**
		 * The negated form of literal.
		 * This is null for the base annotation.
		 */
		Term mNegLiteral;
	}
	
	/**
	 * Compute the gcd of all Farkas coefficients used in the annotation.
	 * This is used to keep make the Farkas coefficients integral.
	 * @param annot the annotation.
	 * @return the gcd of all Farkas coefficients in annot.
	 */
	private Rational computeGcd(LAAnnotation annot) {
		Rational gcd = null;
		Iterator<Rational> it = annot.getCoefficients().values().iterator();
		if (it.hasNext())
			gcd = it.next();
		while (it.hasNext())
			gcd = gcd.gcd(it.next());
		it = annot.getAuxAnnotations().values().iterator();
		if (gcd == null && it.hasNext())
			gcd = it.next();
		while (it.hasNext())
			gcd = gcd.gcd(it.next());
		assert gcd != null;
		return gcd;
	}

	/**
	 * Fill the literal and negLiteral field in annotation info.
	 * @param annot  the annotation.
	 * @param theory the SMT theory.
	 * @param info  the information corresponding to the annotation.
	 */
	private void computeLiterals(LAAnnotation annot, Theory theory,
				AnnotationInfo info) {
		MutableAffinTerm at = new MutableAffinTerm();
		at.add(Rational.ONE, annot.getLinVar());
		at.add(annot.getBound().negate());
		if (!annot.isUpper())
			at.add(annot.getLinVar().getEpsilon());
		Term posTerm = at.toSMTLibLeq0(theory, true);
		if (annot.isUpper()) {
			info.mLiteral = posTerm;
			info.mNegLiteral = theory.term("not", posTerm);
		} else {
			info.mLiteral = theory.term("not", posTerm);
			info.mNegLiteral = posTerm;
		}
	}
	
	/**
	 * Convert the base annotation to a proof object.
	 * @param parent the base annotation (i.e. its linvar is null).
	 * @param theory the SMT theory.
	 * @return the proof term corresponding to the annotation.
	 */
	public Term convert(LAAnnotation parent, Theory theory) {
		assert (parent.getLinVar() == null);
		HashMap<LAAnnotation, AnnotationInfo> infos = 
			new HashMap<LAAnnotation, AnnotationInfo>();

		// Count the occurences of each annotation (and compute literals).
		ArrayDeque<LAAnnotation> todo = new ArrayDeque<LAAnnotation>();
		todo.add(parent);
		while (!todo.isEmpty()) {
			LAAnnotation annot = todo.removeFirst();
			AnnotationInfo info = infos.get(annot);
			if (info == null) {
				info = new AnnotationInfo();
				infos.put(annot, info);
				if (annot.getLinVar() != null)
					computeLiterals(annot, theory, info);
			}
			info.mCount++;
			todo.addAll(annot.getAuxAnnotations().keySet());
		}

		ArrayDeque<Term> antes = new ArrayDeque<Term>();
		todo.add(parent);
	todo_loop:
		while (!todo.isEmpty()) {
			LAAnnotation annot = todo.removeFirst();
			AnnotationInfo info = infos.get(annot);
			info.mVisited++;
			if (info.mVisited < info.mCount)
				continue;

			// The annotation was visited for the final time.

			// Add its sub-annotations to the todo list.
			todo.addAll(annot.getAuxAnnotations().keySet());

			// Now convert it to a clause and add it to antes.
			Rational gcd = computeGcd(annot);
			int numdisjs = annot.getCoefficients().size()
					+ annot.getAuxAnnotations().size()
					+ (info.mLiteral == null ? 0 : 1);
			int i = 0;
			Term[] disjs = new Term[numdisjs];
			Term[] coeffs = new Term[numdisjs];
			if (info.mLiteral != null) {
				Rational sign = annot.isUpper() ? Rational.MONE : Rational.ONE;
				disjs[i] = info.mLiteral;
				coeffs[i] = sign.div(gcd).toSMTLIB(theory);
				++i;
			}
			boolean trichotomy = false;
			for (Map.Entry<Literal, Rational> me
				: annot.getCoefficients().entrySet()) {
				Literal lit = me.getKey();
				if (lit instanceof LAEquality)
					trichotomy = true;
				disjs[i] = me.getKey().getSMTFormula(theory, true);
				coeffs[i] = me.getValue().div(gcd).toSMTLIB(theory);
				++i;
			}
			for (Map.Entry<LAAnnotation, Rational> me
				: annot.getAuxAnnotations().entrySet()) {
				AnnotationInfo auxInfo = infos.get(me.getKey());
				// If the generated clause would just be of the form
				// ell \/ not ell, we omit the sub-annotation from the
				// proof.
				if (disjs.length == 2 && auxInfo.mLiteral == disjs[0])
					continue todo_loop;
				disjs[i] = auxInfo.mNegLiteral;
				coeffs[i] = me.getValue().div(gcd).toSMTLIB(theory);
				++i;
			}
			Term proofAnnot = theory.term(theory.mOr, disjs);
			Annotation[] annots = new Annotation[] {
				trichotomy ? TRICHOTOMY : new Annotation(":LA", coeffs)
			};
			proofAnnot = theory.annotatedTerm(annots, proofAnnot);
			proofAnnot = theory.term("@lemma", proofAnnot);
			if (!antes.isEmpty()) {
				// Since the base annotation should be translated first
				// this must be a sub-annotation, so we should have the
				// corresponding pivot literal.
				assert (info.mLiteral != null);
				proofAnnot = theory.annotatedTerm(new Annotation[]{
					new Annotation(":pivot", info.mLiteral)
				}, proofAnnot);
			}
			antes.add(proofAnnot);
		}
		if (antes.size() == 1)
			return antes.getFirst();
		return theory.term("@res", antes.toArray(new Term[antes.size()]));
	}
}