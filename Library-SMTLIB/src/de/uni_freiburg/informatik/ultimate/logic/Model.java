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

import java.util.Map;

/**
 * A minimal interface for model queries.  The model should represent the model
 * generated by an SMT solver.  It should be detached from the solver, i.e., a
 * model retrieved from a solver should not be invalidated by an assertion stack
 * command.  Note that the model is not a model of the assertion stack at that
 * time, but we want to give the user the freedom to use the model in an
 * interactive way.  Note that symbols defined by model generation might be
 * removed once the assertion stack level is popped off the stack.
 * 
 * Values for numeric sorts in linear arithmetic logics are
 * {@link ConstantTerm ConstantTerms} whose value is of type {@link Rational}.
 * For non-numeric sorts, we return some term of the corresponding sort.  No
 * further guarantees are made.  
 * @author Juergen Christ
 */
public interface Model {
	/**
	 * Compute the value of an input term.
	 * @param input Term to evaluate.
	 * @return Value of the term.
	 */
	public Term evaluate(Term input);
	/**
	 * Compute the value of some input terms.
	 * @param input Terms to evaluate.
	 * @return Values of the terms.
	 */
	public Map<Term, Term> evaluate(Term[] input);
	/**
	 * Return a term that constrains the possible values of a given term.
	 * @param input The given term.
	 * @return The constraining term.
	 */
	public Term constrainBySort(Term input);
}
