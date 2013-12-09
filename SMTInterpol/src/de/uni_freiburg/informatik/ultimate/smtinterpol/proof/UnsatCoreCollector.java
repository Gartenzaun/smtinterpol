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
package de.uni_freiburg.informatik.ultimate.smtinterpol.proof;

import java.util.HashSet;

import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.smtinterpol.dpll.Clause;
import de.uni_freiburg.informatik.ultimate.smtinterpol.proof.ResolutionNode.Antecedent;
import de.uni_freiburg.informatik.ultimate.smtinterpol.util.IdentityHashSet;

/**
 * Collect all formula labels used to prove unsatisfiability.
 * @author Juergen Christ
 */
public class UnsatCoreCollector {
	private final Script mScript;
	private final HashSet<String> mUnsatCoreIds;
	private final IdentityHashSet<Clause> mVisited;
	public UnsatCoreCollector(Script script) {
		mScript = script;
		mUnsatCoreIds = new HashSet<String>();
		mVisited = new IdentityHashSet<Clause>();
	}
	
	public Term[] getUnsatCore(Clause unsat) {
		try {
			accept(unsat);
			Term[] res = new Term[mUnsatCoreIds.size()];
			int i = -1;
			for (String s : mUnsatCoreIds)
				res[++i] = mScript.term(s);
			return res;
		} catch (SMTLIBException ese) {
			throw new InternalError(ese.getMessage());
		}
	}
	// Entry to the visitor.
	private void accept(Clause c) {
		if (mVisited.add(c)) {
			if (c.getProof().isLeaf())
				visit((LeafNode) c.getProof());
			else
				visit((ResolutionNode) c.getProof());
		}
	}
	// Visit resolution nodes
	private void visit(ResolutionNode node) {
		accept(node.getPrimary());
		Antecedent[] ants = node.getAntecedents();
		for (Antecedent a : ants) {
			accept(a.mAntecedent);
		}
	}
	// Visit leaf nodes
	private void visit(LeafNode node) {
		// Tautologies are not needed in an unsat core
		if (node.getLeafKind() == LeafNode.NO_THEORY
				&& node.getTheoryAnnotation() instanceof SourceAnnotation) {
			String name = ((SourceAnnotation) node.getTheoryAnnotation()).
				getAnnotation();
			// Guard against unnamed clauses
			if (!name.isEmpty())
				mUnsatCoreIds.add(name);
		}
	}
}
