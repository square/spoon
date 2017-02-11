/*
 * Copyright 2008 Android4ME
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.spoon.internal.thirdparty.axmlparser;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Dmitry Skiba
 *
 * Various read helpers.
 *
 * TODO: remove? (as we have IntReader now)
 *
 */
public class ReadUtil {

    public static final void readCheckType(InputStream stream,int expectedType) throws IOException {
        int type=readInt(stream);
        if (type!=expectedType) {
            throw new IOException(
                    "Expected chunk of type 0x"+Integer.toHexString(expectedType)+
                            ", read 0x"+Integer.toHexString(type)+".");
        }
    }

    public static final void readCheckType(IntReader reader,int expectedType) throws IOException {
        int type=reader.readInt();
        if (type!=expectedType) {
            throw new IOException(
                    "Expected chunk of type 0x"+Integer.toHexString(expectedType)+
                            ", read 0x"+Integer.toHexString(type)+".");
        }
    }

    public static final int[] readIntArray(InputStream stream,int elementCount) throws IOException {
        int[] result=new int[elementCount];
        for (int i=0;i!=elementCount;++i) {
            result[i]=readInt(stream);
        }
        return result;
    }

    public static final int readInt(InputStream stream) throws IOException {
        return readInt(stream,4);
    }

    public static final int readShort(InputStream stream) throws IOException {
        return readInt(stream,2);
    }

    public static final String readString(InputStream stream) throws IOException {
        int length=readShort(stream);
        StringBuilder builder=new StringBuilder(length);
        for (int i=0;i!=length;++i) {
            builder.append((char)readShort(stream));
        }
        readShort(stream);
        return builder.toString();
    }

    public static final int readInt(InputStream stream,int length) throws IOException {
        int result=0;
        for (int i=0;i!=length;++i) {
            int b=stream.read();
            if (b==-1) {
                throw new EOFException();
            }
            result|=(b<<(i*8));
        }
        return result;
    }

}