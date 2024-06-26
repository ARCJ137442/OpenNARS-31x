/*
 * The MIT License
 *
 * Copyright 2019 The OpenNARS authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package nars.language;

import java.util.*;

import nars.io.Symbols;
import nars.storage.Memory;

/**
 * A disjunction of Statements.
 */
public class Disjunction extends CompoundTerm {

    /**
     * Constructor with partial values, called by make
     *
     * @param n   The name of the term
     * @param arg The component list of the term
     */
    private Disjunction(ArrayList<Term> arg) {
        super(arg);
    }

    /**
     * Constructor with full values, called by clone
     *
     * @param n    The name of the term
     * @param cs   Component list
     * @param open Open variable list
     * @param i    Syntactic complexity of the compound
     */
    private Disjunction(String n, ArrayList<Term> cs, boolean con, short i) {
        super(n, cs, con, i);
    }

    /**
     * Clone an object
     *
     * @return A new object
     */
    @Override
    public Disjunction clone() {
        return new Disjunction(name, (ArrayList<Term>) cloneList(components), isConstant(), complexity);
    }

    /**
     * Try to make a new Disjunction from two components. Called by the inference
     * rules.
     *
     * @param term1  The first component
     * @param term2  The first component
     * @param memory Reference to the memory
     * @return A Disjunction generated or a Term it reduced to
     */
    public static Term make(Term term1, Term term2, Memory memory) {
        TreeSet<Term> set;
        if (term1 instanceof Disjunction) {
            set = new TreeSet<>(((CompoundTerm) term1).cloneComponents());
            if (term2 instanceof Disjunction) {
                set.addAll(((CompoundTerm) term2).cloneComponents());
            } // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
            else {
                set.add((Term) term2.clone());
            } // (&,(&,P,Q),R) = (&,P,Q,R)
        } else if (term2 instanceof Disjunction) {
            set = new TreeSet<>(((CompoundTerm) term2).cloneComponents());
            set.add((Term) term1.clone()); // (&,R,(&,P,Q)) = (&,P,Q,R)
        } else {
            set = new TreeSet<>();
            set.add((Term) term1.clone());
            set.add((Term) term2.clone());
        }
        return make(set, memory);
    }

    /**
     * Try to make a new IntersectionExt. Called by StringParser.
     *
     * @param argList a list of Term as components
     * @param memory  Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term make(ArrayList<Term> argList, Memory memory) {
        TreeSet<Term> set = new TreeSet<>(argList); // sort/merge arguments
        return make(set, memory);
    }

    /**
     * Try to make a new Disjunction from a set of components. Called by the public
     * make methods.
     *
     * @param set    a set of Term as components
     * @param memory Reference to the memory
     * @return the Term generated from the arguments
     */
    public static Term make(TreeSet<Term> set, Memory memory) {
        if (set.size() == 1) {
            return set.first();
        } // special case: single component
        ArrayList<Term> argument = new ArrayList<>(set);
        String name = makeCompoundName(Symbols.DISJUNCTION_OPERATOR, argument);
        Term t = memory.nameToListedTerm(name);
        return (t != null) ? t : new Disjunction(argument);
    }

    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public String operator() {
        return Symbols.DISJUNCTION_OPERATOR;
    }

    /**
     * Disjunction is commutative.
     *
     * @return true for commutative
     */
    @Override
    public boolean isCommutative() {
        return true;
    }
}
