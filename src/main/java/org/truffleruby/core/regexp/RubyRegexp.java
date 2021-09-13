/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.core.regexp;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import org.joni.Regex;
import org.truffleruby.RubyContext;
import org.truffleruby.core.encoding.RubyEncoding;
import org.truffleruby.core.kernel.KernelNodes;
import org.truffleruby.core.klass.RubyClass;
import org.truffleruby.core.rope.Rope;
import org.truffleruby.core.rope.RopeWithEncoding;
import org.truffleruby.language.ImmutableRubyObject;
import org.truffleruby.language.dispatch.DispatchNode;

@ExportLibrary(InteropLibrary.class)
public class RubyRegexp extends ImmutableRubyObject implements TruffleObject {

    public final Regex regex;
    public final Rope source;
    public final RubyEncoding encoding;
    public final RegexpOptions options;
    public final EncodingCache cachedEncodings;
    public final TRegexCache tregexCache;

    public RubyRegexp(Regex regex, RegexpOptions options) {
        // The RegexpNodes.compile operation may modify the encoding of the source rope. This modified copy is stored
        // in the Regex object as the "user object". Since ropes are immutable, we need to take this updated copy when
        // constructing the final regexp.
        this.regex = regex;
        final RopeWithEncoding ropeWithEncoding = (RopeWithEncoding) regex.getUserObject();
        this.source = ropeWithEncoding.getRope();
        this.encoding = ropeWithEncoding.getEncoding();
        assert source.encoding == encoding.jcoding;
        this.options = options;
        this.cachedEncodings = new EncodingCache();
        this.tregexCache = new TRegexCache();
    }

    // region InteropLibrary messages
    @ExportMessage
    protected Object toDisplayString(boolean allowSideEffects,
            @Cached DispatchNode dispatchNode,
            @Cached KernelNodes.ToSNode kernelToSNode) {
        if (allowSideEffects) {
            return dispatchNode.call(this, "inspect");
        } else {
            return kernelToSNode.executeToS(this);
        }
    }

    @ExportMessage
    protected boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    protected RubyClass getMetaObject(
            @CachedLibrary("this") InteropLibrary node) {
        return RubyContext.get(node).getCoreLibrary().regexpClass;
    }
    // endregion

}
