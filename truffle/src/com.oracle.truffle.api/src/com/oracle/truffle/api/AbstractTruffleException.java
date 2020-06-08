/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.api;

import com.oracle.truffle.api.TruffleStackTrace.LazyStackTrace;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

/**
 * A base class for an exception thrown during the execution of a guest language program.
 *
 * The following simplified {@code TryCatchNode} shows how the
 * {@link AbstractTruffleException#isCatchable()} should be handled by languages.
 *
 * <pre>
 * private static class TryCatchNode extends StatementNode {
 *
 *     &#64;Child private BlockNode block;
 *     &#64;Children private CatchNode[] catches;
 *     &#64;Child private BlockNode finalizer;
 *     &#64;Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(5);
 *
 *     public TryCatchNode(BlockNode block, CatchNode[] catches, BlockNode finalizer) {
 *         this.block = block;
 *         this.catches = catches;
 *         this.finalizer = finalizer;
 *     }
 *
 *     &#64;Override
 *     &#64;ExplodeLoop
 *     Object execute(VirtualFrame frame) {
 *         boolean runFinalization = true;
 *         try {
 *             return block.execute(frame);
 *         } catch (AbstractTruffleException ex) {
 *             AbstractTruffleException tex = (AbstractTruffleException) ex;
 *             runFinalization = tex.isCatchable();
 *             if (tex.isCatchable() && catches.length > 0) {
 *                 Object exceptionObject = tex.getExceptionObject();
 *                 if (exceptionObject != null) {
 *                     for (CatchNode catchBlock : catches) {
 *                         try {
 *                             if (interop.isMetaInstance(catchBlock.getException(), exceptionObject)) {
 *                                 return catchBlock.execute(frame);
 *                             }
 *                         } catch (UnsupportedMessageException um) {
 *                             ex.addSuppressed(um);
 *                         }
 *                     }
 *                 }
 *             }
 *             throw ex;
 *         } finally {
 *             if (runFinalization && finalizer != null) {
 *                 finalizer.execute(frame);
 *             }
 *         }
 *     }
 * }
 * </pre>
 *
 * @since 20.2
 */
@SuppressWarnings("serial")
public abstract class AbstractTruffleException extends RuntimeException implements TruffleException {

    private volatile LazyStackTrace lazy;

    protected AbstractTruffleException() {
        this(null, null);
    }

    protected AbstractTruffleException(String message) {
        this(message, null);
    }

    protected AbstractTruffleException(Throwable internaCause) {
        this(null, internaCause);
    }

    protected AbstractTruffleException(String message, Throwable internalCause) {
        super(message, checkCause(internalCause));
    }

    @Override
    @SuppressWarnings("sync-override")
    public final Throwable fillInStackTrace() {
        return this;
    }

    /**
     * Returns a node indicating the location where this exception occurred in the AST. This method
     * may return <code>null</code> to indicate that the location is not available.
     *
     */
    public abstract Node getLocation();

    /**
     * Returns an additional guest language object. The return object must be an interop exception
     * type, the {@link @link com.oracle.truffle.api.interop.InteropLibrary#isException(Object)} has
     * to return {@code true}. The default implementation returns <code>null</code> to indicate that
     * no object is available for this exception.
     */
    @Override
    public Object getExceptionObject() {
        return null;
    }

    /**
     * Returns <code>true</code> if this exception indicates a parser or syntax error. Syntax errors
     * typically occur while
     * {@link TruffleLanguage#parse(com.oracle.truffle.api.TruffleLanguage.ParsingRequest) parsing}
     * of guest language source code.
     *
     */
    @Override
    public boolean isSyntaxError() {
        return false;
    }

    /**
     * Returns <code>true</code> if this exception indicates a syntax error that is indicating that
     * the syntax is incomplete. This allows guest language programmers to find out if more code is
     * expected from a given source. For example an incomplete JavaScript program could look like
     * this:
     *
     * <pre>
     * function incompleteFunction(arg) {
     * </pre>
     *
     * A shell might react to this exception and prompt for additional source code, if this method
     * returns <code>true</code>.
     *
     */
    @Override
    public boolean isIncompleteSource() {
        return false;
    }

    /**
     * Returns <code>true</code> if this exception indicates an internal error. Note that all
     * exceptions thrown in a guest language implementation that are not implementing
     * {@link TruffleException} are considered internal.
     *
     * @since 0.27
     */
    @Override
    public boolean isInternalError() {
        return false;
    }

    /**
     * Returns <code>true</code> if this exception indicates that guest language application was
     * cancelled during its execution. If {@code isCancelled} returns {@code true} languages should
     * not catch this exception, they must just rethrow it.
     *
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * Returns <code>true</code> if the exception indicates that the application was exited within
     * the guest language program. If {@link #isExit()} returns <code>true</code> also
     * {@link #getExitStatus()} should be implemented.
     *
     * @see #getExitStatus()
     */
    @Override
    public boolean isExit() {
        return false;
    }

    /**
     * Returns the exit status if this exception indicates that the application was {@link #isExit()
     * exited}. The exit status is intended to be passed to {@link System#exit(int)}.
     *
     * @see #isExit()
     */
    @Override
    public int getExitStatus() {
        return 0;
    }

    /**
     * Returns the number of guest language frames that should be collected for this exception.
     * Returns a negative integer by default for unlimited guest language frames. This is intended
     * to be used by guest languages to limit the number of guest language stack frames. Languages
     * might want to limit the number of frames for performance reasons. Frames that point to
     * {@link RootNode#isInternal() internal} internal root nodes are not counted when the stack
     * trace limit is computed.
     *
     */
    @Override
    public int getStackTraceElementLimit() {
        return -1;
    }

    /**
     * Returns a location where this exception occurred in the AST. This method may return
     * <code>null</code> to indicate that the location is not available.
     *
     * @return the {@link SourceSection} or null
     */
    @Override
    public SourceSection getSourceLocation() {
        final Node node = getLocation();
        return node == null ? null : node.getEncapsulatingSourceSection();
    }

    /**
     * Returns <code>true</code> if this exception can be safely caught by languages. If
     * {@code isCatchable} returns {@code false} languages should not catch this exception, they
     * must just rethrow it.
     *
     */
    public final boolean isCatchable() {
        return !(isCancelled() || isExit());
    }

    /**
     * Re-throws the given exception if it's a {@link ThreadDeath} or a not {@link #isCatchable()
     * catchable} {@link AbstractTruffleException}.
     */
    public static void rethrowUnCatchable(Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }
        if (t instanceof AbstractTruffleException && !((AbstractTruffleException) t).isCatchable()) {
            throw (AbstractTruffleException) t;
        }
    }

    /**
     * Returns {@code true} if the given exception is neither a {@link ThreadDeath} nor a non
     * {@link #isCatchable() catchable} {@link AbstractTruffleException}.
     */
    public static boolean isCatchable(Throwable t) {
        return !(t instanceof ThreadDeath || (t instanceof AbstractTruffleException && !((AbstractTruffleException) t).isCatchable()));
    }

    LazyStackTrace getLazyStackTrace() {
        LazyStackTrace res = lazy;
        if (res == null) {
            synchronized (this) {
                res = lazy;
                if (res == null) {
                    res = new LazyStackTrace();
                    lazy = res;
                }
            }
        }
        return res;
    }

    private static Throwable checkCause(Throwable t) {
        if (t != null && !(t instanceof TruffleException)) {
            throw new IllegalArgumentException("The " + t + " must be TruffleException subclass.");
        }
        return t;
    }
}
