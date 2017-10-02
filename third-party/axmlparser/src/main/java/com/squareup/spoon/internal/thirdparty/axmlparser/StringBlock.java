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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Dmitry Skiba
 *
 * Block of strings, used in binary xml and arsc.
 *
 * TODO:
 * - implement get()
 *
 */
public class StringBlock {

    /**
     * Reads whole (including chunk type) string block from stream.
     * Stream must be at the chunk type.
     */
    public static StringBlock read(IntReader reader) throws IOException {
        ReadUtil.readCheckType(reader,CHUNK_TYPE);
        int chunkSize=reader.readInt();
        int stringCount=reader.readInt();
        int styleOffsetCount=reader.readInt();
        int flags = reader.readInt();
        int stringsOffset=reader.readInt();
        int stylesOffset=reader.readInt();

        StringBlock block=new StringBlock();
        block.m_stringOffsets=reader.readIntArray(stringCount);
        if (styleOffsetCount!=0) {
            block.m_styleOffsets=reader.readIntArray(styleOffsetCount);
        }
        {
            int size=((stylesOffset==0)?chunkSize:stylesOffset)-stringsOffset;
            if ((size%4)!=0) {
                throw new IOException("String data size is not multiple of 4 ("+size+").");
            }
            block.m_stringPool =reader.readByteArray(size);
        }
        if (stylesOffset!=0) {
            int size=(chunkSize-stylesOffset);
            if ((size%4)!=0) {
                throw new IOException("Style data size is not multiple of 4 ("+size+").");
            }
            block.m_styles=reader.readIntArray(size/4);
        }

        // Set field flag to determine if stored in UTF-8 (or false for UTF-16).
        block.m_isUtf8 = (flags & UTF8_FLAG) == UTF8_FLAG;

        block.m_strings =new String[block.getCount()];
        for (int i=0;i!=block.getCount();++i) {
            block.m_strings[i]=block.stringAt(i);
        }

        return block;
    }

    /**
     * Returns number of strings in block.
     */
    public int getCount() {
        return m_stringOffsets!=null?
                m_stringOffsets.length:
                0;
    }

    /**
     * Returns raw string (without any styling information) at specified index.
     * Returns null if index is invalid or object was not initialized.
     */
    public String getRaw(int index) {
        if (m_strings == null || index < 0 || index > m_strings.length - 1) {
            return null;
        }

        return m_strings[index];
    }

    /**
     * Not yet implemented.
     *
     * Returns string with style information (if any).
     * Returns null if index is invalid or object was not initialized.
     */
    public CharSequence get(int index) {

        return Cast.toCharSequence(getRaw(index));
    }

    /**
     * Returns string with style tags (html-like).
     */
    public String getHTML(int index) {
        String raw=getRaw(index);
        if (raw==null) {
            return raw;
        }
        int[] style=getStyle(index);
        if (style==null) {
            return raw;
        }
        StringBuilder html=new StringBuilder(raw.length()+32);
        int offset=0;
        while (true) {
            int i=-1;
            for (int j=0;j!=style.length;j+=3) {
                if (style[j+1]==-1) {
                    continue;
                }
                if (i==-1 || style[i+1]>style[j+1]) {
                    i=j;
                }
            }
            int start=((i!=-1)?style[i+1]:raw.length());
            for (int j=0;j!=style.length;j+=3) {
                int end=style[j+2];
                if (end==-1 || end>=start) {
                    continue;
                }
                if (offset<=end) {
                    html.append(raw,offset,end+1);
                    offset=end+1;
                }
                style[j+2]=-1;
                html.append('<');
                html.append('/');
                html.append(getRaw(style[j]));
                html.append('>');
            }
            if (offset<start) {
                html.append(raw,offset,start);
                offset=start;
            }
            if (i==-1) {
                break;
            }
            html.append('<');
            html.append(getRaw(style[i]));
            html.append('>');
            style[i+1]=-1;
        }
        return html.toString();
    }

    /**
     * Finds index of the string.
     * Returns -1 if the string was not found.
     */
    public int find(String string) {
        if (m_strings == null) {
            return -1;
        }

        for (int i = 0; i < m_strings.length - 1; i++) {
            if (m_strings[i].equals(string)) {
                return i;
            }
        }

        return -1;
    }

    ///////////////////////////////////////////// implementation

    private StringBlock() {
    }

    /**
     * Returns style information - array of int triplets,
     * where in each triplet:
     * 	* first int is index of tag name ('b','i', etc.)
     * 	* second int is tag start index in string
     * 	* third int is tag end index in string
     */
    private int[] getStyle(int index) {
        if (m_styleOffsets==null || m_styles==null ||
                index>=m_styleOffsets.length)
        {
            return null;
        }
        int offset=m_styleOffsets[index]/4;
        int style[];
        {
            int count=0;
            for (int i=offset;i<m_styles.length;++i) {
                if (m_styles[i]==-1) {
                    break;
                }
                count+=1;
            }
            if (count==0 || (count%3)!=0) {
                return null;
            }
            style=new int[count];
        }
        for (int i=offset,j=0;i<m_styles.length;) {
            if (m_styles[i]==-1) {
                break;
            }
            style[j++]=m_styles[i++];
        }
        return style;
    }

    /**
     * (Comment taken from Android platform ResourceTypes.cpp)
     * <p>
     * Strings in UTF-8 format have length indicated by a length encoded in the
     * stored data. It is either 1 or 2 characters of length data. This allows a
     * maximum length of 0x7FFF (32767 bytes), but you should consider storing
     * text in another way if you're using that much data in a single string.
     * <p>
     * If the high bit is set, then there are two characters or 2 bytes of length
     * data encoded. In that case, drop the high bit of the first character and
     * add it together with the next character.
     */
    private int decodeLengthUtf8(ByteBuffer buffer) {
        int length = buffer.get();
        if ((length & 0x8000) != 0) {
            length = ((length & 0x7FFF) << 16) | buffer.get();
        } else {
            // Advance past the 2nd useless duplicate length byte.
            buffer.get();
        }
        return length;
    }

    /**
     * Called to decode string length located at the start of each
     * string in the string pool. Redirects call the the appropriate
     * decoding handler for either UTF-8 or UTF-16 string pools.
     *
     * @param byteBuffer A ByteBuffer whose current index points to
     *                   the encoded length value.
     * @return A decoded string length.
     */
    private int decodeLength(ByteBuffer byteBuffer) {
        return m_isUtf8 ? decodeLengthUtf8(byteBuffer) : decodeLengthUtf16(byteBuffer);
    }


    /**
     * (Comment taken from platform ResourceTypes.cpp)
     * <p>
     * Strings in UTF-16 format have length indicated by a length encoded in the
     * stored data. It is either 1 or 2 characters of length data. This allows a
     * maximum length of 0x7FFFFFF (2147483647 bytes), but if you're storing that
     * much data in a string, you're abusing them.
     * <p>
     * If the high bit is set, then there are two characters or 4 bytes of length
     * data encoded. In that case, drop the high bit of the first character and
     * add it together with the next character.
     */
    private static int decodeLengthUtf16(ByteBuffer buffer) {
        int length = buffer.get();
        if ((length & 0x80) != 0) {
            length = ((length & 0x7F) << 8) | buffer.get();
        }
        return length;
    }

    /**
     * Extracts a string from the string pool at the given index.
     *
     * @param index Position of string in the string pool.
     * @return A String representation of the encoded string pool entry.
     */
    private String stringAt(int index) {
        // Determine the offset from the start of the string pool.
        int offset = m_stringOffsets[index];

        // For convenience, wrap the string pool in ByteBuffer
        // so that it will handle advancing the buffer index.
        ByteBuffer buffer =
                ByteBuffer.wrap(m_stringPool, offset, m_stringPool.length - offset)
                        .order(ByteOrder.BIG_ENDIAN);

        // Now get the decoded string length.
        int length = decodeLength(buffer);

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            if (m_isUtf8) {
                stringBuilder.append((char) buffer.get());
            } else {
                int byte1 = buffer.get();
                int byte2 = buffer.get();
                stringBuilder.append((char) (byte2 | byte1));
            }
        }

        return stringBuilder.toString();
    }

    private boolean m_isUtf8;
    private byte[] m_stringPool;
    private String[] m_strings;
    private int[] m_stringOffsets;
    private int[] m_styleOffsets;
    private int[] m_styles;

    private static final int CHUNK_TYPE=0x001C0001;
    private static final int UTF8_FLAG = 1 << 8;
}