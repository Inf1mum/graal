/*
 * Copyright (c) 2017, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.runtime.debug;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import java.util.function.Supplier;

public final class LLVMDebugPointerType extends LLVMDebugType {

    @CompilationFinal private Supplier<LLVMDebugType> baseType;

    public LLVMDebugPointerType(long size, long align, long offset) {
        this(LLVMDebugType.UNKNOWN_TYPE::getName, size, align, offset, () -> LLVMDebugType.UNKNOWN_TYPE);
    }

    private LLVMDebugPointerType(Supplier<String> nameSupplier, long size, long align, long offset, Supplier<LLVMDebugType> baseType) {
        super(nameSupplier, size, align, offset);
        this.baseType = baseType;
    }

    @TruffleBoundary
    public LLVMDebugType getBaseType() {
        return baseType.get();
    }

    public void setBaseType(Supplier<LLVMDebugType> baseType) {
        CompilerAsserts.neverPartOfCompilation();
        this.baseType = baseType;
    }

    @Override
    public LLVMDebugType getOffset(long newOffset) {
        return new LLVMDebugPointerType(this::getName, getSize(), getAlign(), newOffset, this::getBaseType);
    }

    @Override
    public boolean isPointer() {
        return true;
    }

    @Override
    public boolean isAggregate() {
        return false;
    }

    @Override
    public int getElementCount() {
        return 1;
    }

    @Override
    public String getElementName(long i) {
        return "target";
    }

    @Override
    public LLVMDebugType getElementType(long i) {
        return getBaseType();
    }

    @Override
    public LLVMDebugType getElementType(String name) {
        return getBaseType();
    }
}
