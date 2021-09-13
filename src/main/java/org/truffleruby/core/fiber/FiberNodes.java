/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.fiber;

import org.truffleruby.builtins.CoreMethod;
import org.truffleruby.builtins.CoreMethodArrayArgumentsNode;
import org.truffleruby.builtins.CoreMethodNode;
import org.truffleruby.builtins.CoreModule;
import org.truffleruby.builtins.Primitive;
import org.truffleruby.builtins.PrimitiveArrayArgumentsNode;
import org.truffleruby.builtins.UnaryCoreMethodNode;
import org.truffleruby.core.array.RubyArray;
import org.truffleruby.core.cast.SingleValueCastNode;
import org.truffleruby.core.cast.SingleValueCastNodeGen;
import org.truffleruby.core.encoding.Encodings;
import org.truffleruby.core.exception.RubyException;
import org.truffleruby.core.fiber.FiberNodesFactory.FiberTransferNodeFactory;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.proc.RubyProc;
import org.truffleruby.core.rope.CodeRange;
import org.truffleruby.core.string.RubyString;
import org.truffleruby.core.string.StringNodes.MakeStringNode;
import org.truffleruby.core.thread.RubyThread;
import org.truffleruby.language.Nil;
import org.truffleruby.language.Visibility;
import org.truffleruby.language.control.RaiseException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.truffleruby.language.objects.AllocationTracing;

@CoreModule(value = "Fiber", isClass = true)
public abstract class FiberNodes {

    public abstract static class FiberTransferNode extends CoreMethodArrayArgumentsNode {

        @Child private SingleValueCastNode singleValueCastNode;

        public Object singleValue(Object[] args) {
            if (singleValueCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                singleValueCastNode = insert(SingleValueCastNodeGen.create());
            }
            return singleValueCastNode.executeSingleValue(args);
        }

        public abstract Object executeTransferControlTo(RubyThread currentThread, RubyFiber currentFiber,
                RubyFiber fiber, FiberOperation operation, Object[] args);

        @Specialization
        protected Object transfer(
                RubyThread currentThread,
                RubyFiber currentFiber,
                RubyFiber fiber,
                FiberOperation operation,
                Object[] args,
                @Cached BranchProfile errorProfile) {

            if (!fiber.alive) {
                errorProfile.enter();
                throw new RaiseException(getContext(), coreExceptions().deadFiberCalledError(this));
            }

            if (fiber.rubyThread != currentThread) {
                errorProfile.enter();
                throw new RaiseException(
                        getContext(),
                        coreExceptions().fiberError("fiber called across threads", this));
            }

            return singleValue(getContext().fiberManager.transferControlTo(currentFiber, fiber, operation, args, this));
        }

    }

    @CoreMethod(names = { "__allocate__", "__layout_allocate__" }, constructor = true, visibility = Visibility.PRIVATE)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        protected RubyFiber allocate(RubyClass rubyClass) {
            if (getContext().getOptions().BACKTRACE_ON_NEW_FIBER) {
                getContext().getDefaultBacktraceFormatter().printBacktraceOnEnvStderr("fiber: ", this);
            }

            final RubyThread thread = getLanguage().getCurrentThread();
            final RubyFiber fiber = new RubyFiber(
                    rubyClass,
                    getLanguage().fiberShape,
                    getContext(),
                    getLanguage(),
                    thread,
                    "<uninitialized>");
            AllocationTracing.trace(fiber, this);
            return fiber;
        }
    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        protected Object initialize(RubyFiber fiber, RubyProc block) {
            final RubyThread thread = getLanguage().getCurrentThread();
            getContext().fiberManager.initialize(fiber, block, this);
            return nil;
        }

        @Specialization
        protected Object noBlock(RubyFiber fiber, Nil block) {
            throw new RaiseException(getContext(), coreExceptions().argumentErrorProcWithoutBlock(this));
        }

    }

    @CoreMethod(names = "transfer", rest = true)
    public abstract static class TransferNode extends CoreMethodArrayArgumentsNode {

        @Child private FiberTransferNode fiberTransferNode = FiberTransferNodeFactory.create(null);

        @Specialization
        protected Object resume(RubyFiber fiber, Object[] args,
                @Cached ConditionProfile sameFiberProfile) {

            fiber.transferred = true;

            final RubyThread currentThread = getLanguage().getCurrentThread();
            final RubyFiber currentFiber = currentThread.getCurrentFiber();

            if (sameFiberProfile.profile(currentFiber == fiber)) {
                // A Fiber can transfer to itself
                return fiberTransferNode.singleValue(args);
            }

            return fiberTransferNode
                    .executeTransferControlTo(currentThread, currentFiber, fiber, FiberOperation.TRANSFER, args);
        }

    }


    public abstract static class FiberResumeNode extends CoreMethodArrayArgumentsNode {

        public static FiberResumeNode create() {
            return FiberNodesFactory.FiberResumeNodeFactory.create(null);
        }

        public abstract Object executeResume(FiberOperation operation, RubyFiber fiber, Object[] args);

        @Child private FiberTransferNode fiberTransferNode = FiberTransferNodeFactory.create(null);

        @Specialization
        protected Object resume(FiberOperation operation, RubyFiber fiber, Object[] args,
                @Cached ConditionProfile doubleResumeProfile,
                @Cached ConditionProfile transferredProfile) {

            final RubyFiber parentFiber = fiber.lastResumedByFiber;

            if (doubleResumeProfile.profile(parentFiber != null || fiber.isRootFiber())) {
                throw new RaiseException(getContext(), coreExceptions().fiberError("double resume", this));
            }

            if (operation != FiberOperation.RAISE && transferredProfile.profile(fiber.transferred)) {
                throw new RaiseException(
                        getContext(),
                        coreExceptions().fiberError("cannot resume transferred Fiber", this));
            }

            final RubyThread currentThread = getLanguage().getCurrentThread();
            final RubyFiber currentFiber = currentThread.getCurrentFiber();

            return fiberTransferNode
                    .executeTransferControlTo(currentThread, currentFiber, fiber, operation, args);
        }

    }


    @Primitive(name = "fiber_raise")
    public abstract static class FiberRaiseNode extends PrimitiveArrayArgumentsNode {

        @Child private FiberResumeNode fiberResumeNode = FiberResumeNode.create();

        @Specialization
        protected Object raise(RubyFiber fiber, RubyException exception,
                @Cached BranchProfile errorProfile) {
            if (!fiber.resumed) {
                errorProfile.enter();
                throw new RaiseException(
                        getContext(),
                        coreExceptions().fiberError("cannot raise exception on unborn fiber", this));
            }
            return fiberResumeNode.executeResume(FiberOperation.RAISE, fiber, new Object[]{ exception });
        }

    }

    @CoreMethod(names = "resume", rest = true)
    public abstract static class ResumeNode extends CoreMethodArrayArgumentsNode {

        @Child private FiberResumeNode fiberResumeNode = FiberResumeNode.create();

        @Specialization
        protected Object resume(RubyFiber fiber, Object[] args) {
            return fiberResumeNode.executeResume(FiberOperation.RESUME, fiber, args);
        }

    }

    @CoreMethod(names = "yield", onSingleton = true, rest = true)
    public abstract static class YieldNode extends CoreMethodArrayArgumentsNode {

        @Child private FiberTransferNode fiberTransferNode = FiberTransferNodeFactory.create(null);

        @Specialization
        protected Object fiberYield(Object[] args,
                @Cached BranchProfile errorProfile) {

            final RubyThread currentThread = getLanguage().getCurrentThread();
            final RubyFiber currentFiber = currentThread.getCurrentFiber();

            final RubyFiber fiberYieldedTo = getContext().fiberManager
                    .getReturnFiber(currentFiber, this, errorProfile);

            return fiberTransferNode.executeTransferControlTo(
                    currentThread,
                    currentFiber,
                    fiberYieldedTo,
                    FiberOperation.YIELD,
                    args);
        }

    }

    @CoreMethod(names = "alive?")
    public abstract static class AliveNode extends UnaryCoreMethodNode {

        @Specialization
        protected boolean alive(RubyFiber fiber) {
            return fiber.alive;
        }

    }

    @CoreMethod(names = "current", onSingleton = true)
    public abstract static class CurrentNode extends CoreMethodNode {

        @Specialization
        protected RubyFiber current() {
            return getLanguage().getCurrentThread().getCurrentFiber();
        }

    }

    @Primitive(name = "fiber_source_location")
    public abstract static class FiberSourceLocationNode extends PrimitiveArrayArgumentsNode {
        @Specialization
        protected RubyString sourceLocation(RubyFiber fiber,
                @Cached MakeStringNode makeStringNode) {
            return makeStringNode.executeMake(fiber.sourceLocation, Encodings.UTF_8, CodeRange.CR_UNKNOWN);
        }
    }

    @Primitive(name = "fiber_status")
    public abstract static class FiberStatusNode extends PrimitiveArrayArgumentsNode {
        @Specialization
        protected RubyString status(RubyFiber fiber,
                @Cached MakeStringNode makeStringNode) {
            return makeStringNode.executeMake(fiber.getStatus(), Encodings.UTF_8, CodeRange.CR_UNKNOWN);
        }
    }

    @Primitive(name = "fiber_thread")
    public abstract static class FiberThreadNode extends PrimitiveArrayArgumentsNode {
        @Specialization
        protected RubyThread thread(RubyFiber fiber) {
            return fiber.rubyThread;
        }
    }

    @Primitive(name = "fiber_get_catch_tags")
    public abstract static class FiberGetCatchTagsNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected RubyArray getCatchTags() {
            final RubyFiber currentFiber = getLanguage().getCurrentThread().getCurrentFiber();
            return currentFiber.catchTags;
        }
    }

}
