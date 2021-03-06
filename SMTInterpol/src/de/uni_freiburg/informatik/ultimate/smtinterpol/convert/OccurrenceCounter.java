/*
 * Copyright (C) 2012 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.smtinterpol.convert;

import de.uni_freiburg.informatik.ultimate.logic.AnnotatedTerm;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.LetTerm;
import de.uni_freiburg.informatik.ultimate.logic.NonRecursive;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;

public class OccurrenceCounter extends NonRecursive {
	
	private static class CountWalker extends InternAbstractTermWalker {

		public CountWalker(Term term) {
			super(term);
		}

		@Override
		public void walk(NonRecursive walker, SMTAffineTerm term) {
			OccurrenceCounter occ = (OccurrenceCounter) walker;
			if (++term.mTmpCtr == 1)
				for (Term t : term.getSummands().keySet())
					occ.enqueueWalker(new CountWalker(t));
		}

		@Override
		public void walk(NonRecursive walker, ConstantTerm term) {
			// TODO Do we need counts for constants???
		}

		@Override
		public void walk(NonRecursive walker, AnnotatedTerm term) {
			// just skip
			walker.enqueueWalker(new CountWalker(term.getSubterm()));
		}

		@Override
		public void walk(NonRecursive walker, ApplicationTerm term) {
			OccurrenceCounter occ = (OccurrenceCounter) walker;
			if (++term.mTmpCtr == 1)
				for (Term t : term.getParameters())
					occ.enqueueWalker(new CountWalker(t));
		}

		@Override
		public void walk(NonRecursive walker, LetTerm term) {
			throw new InternalError("Term should be unletted before counting");
		}

		@Override
		public void walk(NonRecursive walker, QuantifiedFormula term) {
			// TODO Do we really want to descent into quantified formulas???
			OccurrenceCounter occ = (OccurrenceCounter) walker;
			if (++term.mTmpCtr == 1)
				occ.enqueueWalker(new CountWalker(term.getSubformula()));
		}

		@Override
		public void walk(NonRecursive walker, TermVariable term) {
			++term.mTmpCtr;
		}
		
	}
	
	private static class ResetWalker extends InternAbstractTermWalker {

		public ResetWalker(Term term) {
			super(term);
		}

		@Override
		public void walk(NonRecursive walker, SMTAffineTerm term) {
			OccurrenceCounter occ = (OccurrenceCounter) walker;
			if (term.mTmpCtr != 0) {
				for (Term t : term.getSummands().keySet())
					occ.enqueueWalker(new ResetWalker(t));
				term.mTmpCtr = 0;
			}
		}

		@Override
		public void walk(NonRecursive walker, ConstantTerm term) {
			// TODO Do we need counts for constants???
		}

		@Override
		public void walk(NonRecursive walker, AnnotatedTerm term) {
			// just skip
			walker.enqueueWalker(new ResetWalker(term.getSubterm()));
		}

		@Override
		public void walk(NonRecursive walker, ApplicationTerm term) {
			OccurrenceCounter occ = (OccurrenceCounter) walker;
			if (term.mTmpCtr != 0) {
				for (Term t : term.getParameters())
					occ.enqueueWalker(new ResetWalker(t));
				term.mTmpCtr = 0;
			}
		}

		@Override
		public void walk(NonRecursive walker, LetTerm term) {
			throw new InternalError("Term should be unletted before counting");
		}

		@Override
		public void walk(NonRecursive walker, QuantifiedFormula term) {
			// TODO Do we really want to descent into quantified formulas???
			OccurrenceCounter occ = (OccurrenceCounter) walker;
			if (term.mTmpCtr != 0) {
				occ.enqueueWalker(new ResetWalker(term.getSubformula()));
				term.mTmpCtr = 0;
			}
		}

		@Override
		public void walk(NonRecursive walker, TermVariable term) {
			term.mTmpCtr = 0;
		}
		
	}
	
	/**
	 * Compute the occurrence counter for the sub terms.  This method does not
	 * keep any state in this object.  This should prevent memory leaks since
	 * this object can be kept alive and the garbage collector is still free to
	 * collect the counter map.
	 * @param t The term to count.
	 */
	public void count(Term t) {
		run(new CountWalker(t));
	}
	
	public void reset(Term t) {
		run(new ResetWalker(t));
	}

}
