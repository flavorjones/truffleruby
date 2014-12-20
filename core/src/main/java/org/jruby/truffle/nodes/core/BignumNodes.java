/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.RubyArray;

@CoreClass(name = "Bignum")
public abstract class BignumNodes {

    public static abstract class BignumCoreMethodNode extends CoreMethodNode {

        @Child protected FixnumOrBignumNode fixnumOrBignum;

        public BignumCoreMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context);
        }

        public BignumCoreMethodNode(BignumCoreMethodNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        public Object fixnumOrBignum(RubyBignum value) {
            return fixnumOrBignum.fixnumOrBignum(value);
        }

    }

    @CoreMethod(names = "-@")
    public abstract static class NegNode extends BignumCoreMethodNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NegNode(NegNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBignum neg(RubyBignum value) {
            return value.negate();
        }

    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends BignumCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBignum add(RubyBignum a, int b) {
            return a.add(b);
        }

        @Specialization
        public RubyBignum add(RubyBignum a, long b) {
            return a.add(b);
        }

        @Specialization
        public double add(RubyBignum a, double b) {
            return a.doubleValue() + b;
        }

        @Specialization
        public Object add(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.add(b));
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends BignumCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubNode(SubNode prev) {
            super(prev);
        }

        @Specialization
        public Object sub(RubyBignum a, int b) {
            return fixnumOrBignum(a.subtract(b));
        }

        @Specialization
        public Object sub(RubyBignum a, long b) {
            return fixnumOrBignum(a.subtract(b));
        }

        @Specialization
        public double sub(RubyBignum a, double b) {
            return a.doubleValue() - b;
        }

        @Specialization
        public Object sub(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.subtract(b));
        }

    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MulNode extends CoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MulNode(MulNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBignum mul(RubyBignum a, int b) {
            return a.multiply(b);
        }

        @Specialization
        public RubyBignum mul(RubyBignum a, long b) {
            return a.multiply(b);
        }

        @Specialization
        public double mul(RubyBignum a, double b) {
            return a.doubleValue() * b;
        }

        @Specialization
        public RubyBignum mul(RubyBignum a, RubyBignum b) {
            return a.multiply(b);
        }

    }

    @CoreMethod(names = "**", required = 1)
    public abstract static class PowNode extends CoreMethodNode {

        public PowNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PowNode(PowNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBignum pow(RubyBignum a, int b) {
            return a.pow(b);
        }

        @Specialization
        public RubyBignum pow(RubyBignum a, long b) {
            return a.pow(b);
        }

        @Specialization
        public double pow(RubyBignum a, double b) {
            return Math.pow(a.doubleValue(), b);
        }

        @Specialization
        public RubyBignum pow(RubyBignum a, RubyBignum b) {
            return a.pow(b);
        }

    }

    @CoreMethod(names = "/", required = 1)
    public abstract static class DivNode extends BignumCoreMethodNode {

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DivNode(DivNode prev) {
            super(prev);
        }

        @Specialization
        public Object div(RubyBignum a, int b) {
            return fixnumOrBignum(a.divide(b));
        }

        @Specialization
        public RubyBignum div(RubyBignum a, long b) {
            return a.divide(b);
        }

        @Specialization
        public double div(RubyBignum a, double b) {
            return a.doubleValue() / b;
        }

        @Specialization
        public Object div(RubyBignum a, RubyBignum b) {
            return a.divide(b);
        }

    }

    @CoreMethod(names = "%", required = 1)
    public abstract static class ModNode extends BignumCoreMethodNode {

        public ModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ModNode(ModNode prev) {
            super(prev);
        }

        @Specialization
        public Object mod(RubyBignum a, int b) {
            return fixnumOrBignum(a.mod(b));
        }

        @Specialization
        public Object mod(RubyBignum a, long b) {
            return fixnumOrBignum(a.mod(b));
        }

    }

    @CoreMethod(names = "divmod", required = 1)
    public abstract static class DivModNode extends CoreMethodNode {

        @Child protected GeneralDivModNode divModNode;

        public DivModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            divModNode = new GeneralDivModNode(context);
        }

        public DivModNode(DivModNode prev) {
            super(prev);
            divModNode = new GeneralDivModNode(getContext());
        }

        @Specialization
        public RubyArray divMod(RubyBignum a, int b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(RubyBignum a, long b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(RubyBignum a, RubyBignum b) {
            return divModNode.execute(a, b);
        }

    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class LessNode extends CoreMethodNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessNode(LessNode prev) {
            super(prev);
        }

        @Specialization
        public boolean less(RubyBignum a, int b) {
            return a.compareTo(b) < 0;
        }

        @Specialization
        public boolean less(RubyBignum a, long b) {
            return a.compareTo(b) < 0;
        }

        @Specialization
        public boolean less(RubyBignum a, double b) {
            return a.compareTo(b) < 0;
        }

        @Specialization
        public boolean less(RubyBignum a, RubyBignum b) {
            return a.compareTo(b) < 0;
        }
    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class LessEqualNode extends CoreMethodNode {

        public LessEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessEqualNode(LessEqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, int b) {
            return a.compareTo(b) <= 0;
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, long b) {
            return a.compareTo(b) <= 0;
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, double b) {
            return a.compareTo(b) <= 0;
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, RubyBignum b) {
            return a.compareTo(b) <= 0;
        }
    }

    @CoreMethod(names = {"==", "eql?"}, required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(RubyBignum a, int b) {
            return a.isEqualTo(b);
        }

        @Specialization
        public boolean equal(RubyBignum a, long b) {
            return a.isEqualTo(b);
        }

        @Specialization
        public boolean equal(RubyBignum a, double b) {
            return a.doubleValue() == b;
        }

        @Specialization
        public boolean equal(RubyBignum a, RubyBignum b) {
            return a.isEqualTo(b);
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
        }

        @Specialization
        public int compare(RubyBignum a, int b) {
            return a.compareTo(b);
        }

        @Specialization
        public int compare(RubyBignum a, long b) {
            return a.compareTo(b);
        }

        @Specialization
        public int compare(RubyBignum a, double b) {
            return Double.compare(a.doubleValue(), b);
        }

        @Specialization
        public int compare(RubyBignum a, RubyBignum b) {
            return a.compareTo(b);
        }
    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class GreaterEqualNode extends CoreMethodNode {

        public GreaterEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GreaterEqualNode(GreaterEqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, int b) {
            return a.compareTo(b) >= 0;
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, long b) {
            return a.compareTo(b) >= 0;
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, double b) {
            return a.compareTo(b) >= 0;
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, RubyBignum b) {
            return a.compareTo(b) >= 0;
        }
    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class GreaterNode extends CoreMethodNode {

        public GreaterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GreaterNode(GreaterNode prev) {
            super(prev);
        }

        @Specialization
        public boolean greater(RubyBignum a, int b) {
            return a.compareTo(b) > 0;
        }

        @Specialization
        public boolean greater(RubyBignum a, long b) {
            return a.compareTo(b) > 0;
        }

        @Specialization
        public boolean greater(RubyBignum a, double b) {
            return a.compareTo(b) > 0;
        }

        @Specialization
        public boolean greater(RubyBignum a, RubyBignum b) {
            return a.compareTo(b) > 0;
        }
    }

    @CoreMethod(names = "&", required = 1)
    public abstract static class BitAndNode extends BignumCoreMethodNode {

        public BitAndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitAndNode(BitAndNode prev) {
            super(prev);
        }

        @Specialization
        public Object bitAnd(RubyBignum a, int b) {
            return fixnumOrBignum(a.and(b));
        }

        @Specialization
        public Object bitAnd(RubyBignum a, long b) {
            return fixnumOrBignum(a.and(b));
        }

        @Specialization
        public Object bitAnd(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.and(b));
        }
    }

    @CoreMethod(names = "|", required = 1)
    public abstract static class BitOrNode extends BignumCoreMethodNode {

        public BitOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitOrNode(BitOrNode prev) {
            super(prev);
        }

        @Specialization
        public Object bitOr(RubyBignum a, int b) {
            return fixnumOrBignum(a.or(b));
        }

        @Specialization
        public Object bitOr(RubyBignum a, long b) {
            return fixnumOrBignum(a.or(b));
        }

        @Specialization
        public Object bitOr(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.or(a));
        }
    }

    @CoreMethod(names = "^", required = 1)
    public abstract static class BitXOrNode extends BignumCoreMethodNode {

        public BitXOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitXOrNode(BitXOrNode prev) {
            super(prev);
        }

        @Specialization
        public Object bitXOr(RubyBignum a, int b) {
            return fixnumOrBignum(a.xor(b));
        }

        @Specialization
        public Object bitXOr(RubyBignum a, long b) {
            return fixnumOrBignum(a.xor(b));
        }

        @Specialization
        public Object bitXOr(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.xor(b));
        }
    }

    @CoreMethod(names = "<<", required = 1)
    public abstract static class LeftShiftNode extends BignumCoreMethodNode {

        private final BranchProfile bLessThanZero = BranchProfile.create();

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LeftShiftNode(LeftShiftNode prev) {
            super(prev);
        }

        @Specialization
        public Object leftShift(RubyBignum a, int b) {
            if (b >= 0) {
                return fixnumOrBignum(a.shiftLeft(b));
            } else {
                bLessThanZero.enter();
                return fixnumOrBignum(a.shiftRight(-b));
            }
        }

    }

    @CoreMethod(names = ">>", required = 1)
    public abstract static class RightShiftNode extends BignumCoreMethodNode {

        private final BranchProfile bLessThanZero = BranchProfile.create();

        public RightShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RightShiftNode(RightShiftNode prev) {
            super(prev);
        }

        @Specialization
        public Object leftShift(RubyBignum a, int b) {
            if (b >= 0) {
                return fixnumOrBignum(a.shiftRight(b));
            } else {
                bLessThanZero.enter();
                return fixnumOrBignum(a.shiftLeft(-b));
            }
        }

    }

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(RubyBignum value) {
            return getContext().makeString(value.bigIntegerValue().toString());
        }

    }

}
