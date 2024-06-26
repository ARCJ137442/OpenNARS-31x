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
package nars.entity;

import nars.io.Symbols;
import nars.language.Term;

/**
 * A link between a compound term and a component term
 * <p>
 * A TermLink links the current Term to a target Term, which is
 * either a component of, or compound made from, the current term.
 * <p>
 * Neither of the two terms contain variable shared with other terms.
 * <p>
 * The index value(s) indicates the location of the component in the compound.
 * <p>
 * This class is mainly used in inference.RuleTable to dispatch premises to
 * inference rules
 */
public class TermLink extends Item {
    /** At C, point to C; TaskLink only */
    public static final short SELF = 0;
    /** At (&&, A, C), point to C */
    public static final short COMPONENT = 1;
    /** At C, point to (&&, A, C) */
    public static final short COMPOUND = 2;
    /** At <C --> A>, point to C */
    public static final short COMPONENT_STATEMENT = 3;
    /** At C, point to <C --> A> */
    public static final short COMPOUND_STATEMENT = 4;
    /** At <(&&, C, B) ==> A>, point to C */
    public static final short COMPONENT_CONDITION = 5;
    /** At C, point to <(&&, C, B) ==> A> */
    public static final short COMPOUND_CONDITION = 6;
    /** At C, point to <(*, C, B) --> A>; TaskLink only */
    public static final short TRANSFORM = 8;
    /** The linked Term */
    private Term target;
    /** The type of link, one of the above */
    protected short type;
    /**
     * The index of the component in the component list of the compound, may have up
     * to 4 levels
     */
    protected short[] index;

    /**
     * Constructor for TermLink template
     * <p>
     * called in CompoundTerm.prepareComponentLinks only
     *
     * @param t       Target Term
     * @param p       Link type
     * @param indices Component indices in compound, may be 1 to 4
     */
    public TermLink(Term t, short p, int... indices) {
        target = t;
        type = p;
        assert (type % 2 == 0); // template types all point to compound, though the target is component
        if (type == TermLink.COMPOUND_CONDITION) { // the first index is 0 by default
            index = new short[indices.length + 1];
            index[0] = 0;
            for (int i = 0; i < indices.length; i++) {
                index[i + 1] = (short) indices[i];
            }
        } else {
            index = new short[indices.length];
            for (int i = 0; i < index.length; i++) {
                index[i] = (short) indices[i];
            }
        }
    }

    /**
     * called from TaskLink
     *
     * @param s The key of the TaskLink
     * @param v The budget value of the TaskLink
     */
    protected TermLink(String s, BudgetValue v) {
        super(s, v);
    }

    /**
     * Constructor to make actual TermLink from a template
     * <p>
     * called in Concept.buildTermLinks only
     *
     * @param t        Target Term
     * @param template TermLink template previously prepared
     * @param v        Budget value of the link
     */
    public TermLink(Term t, TermLink template, BudgetValue v) {
        super(t.getName(), v);
        target = t;
        type = template.getType();
        if (template.getTarget().equals(t)) {
            type--; // point to component
        }
        index = template.getIndices();
        setKey();
    }

    /**
     * Set the key of the link
     */
    protected final void setKey() {
        String at1, at2;
        if ((type % 2) == 1) { // to component
            at1 = Symbols.TO_COMPONENT_1;
            at2 = Symbols.TO_COMPONENT_2;
        } else { // to compound
            at1 = Symbols.TO_COMPOUND_1;
            at2 = Symbols.TO_COMPOUND_2;
        }
        String in = "T" + type;
        if (index != null) {
            for (int i = 0; i < index.length; i++) {
                in += "-" + (index[i] + 1);
            }
        }
        key = at1 + in + at2;
        if (target != null) {
            key += target;
        }
    }

    /**
     * Get the target of the link
     *
     * @return The Term pointed by the link
     */
    public Term getTarget() {
        return target;
    }

    /**
     * Get the link type
     *
     * @return Type of the link
     */
    public short getType() {
        return type;
    }

    /**
     * Get all the indices
     *
     * @return The index array
     */
    public short[] getIndices() {
        return index;
    }

    /**
     * Get one index by level
     *
     * @param i The index level
     * @return The index value
     */
    public short getIndex(int i) {
        if ((index != null) && (i < index.length)) {
            return index[i];
        } else {
            return -1;
        }
    }
}
