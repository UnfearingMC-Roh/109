/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util;

/**
 * A C++ Pointer class representing a Java Pointer. 
 * int* pData -> Pointer<Integer> pData
 * 
 * @author Eric
 * @param <P> The Object type to be stored.
 */
public class Pointer<P> {
    public P data;

    public Pointer() {
        this.data = null;
    }

    public Pointer(P data) {
        this.data = data;
    }
    
    /**
     * Returns the value of the typeof Object<P>
     * 
     * @return The value of pData
     */
    public P get() {
        return this.data;
    }
    
    /**
     * Assigns a new value to the data and returns the new value.
     * 
     * @param data The value to assign to pData
     * @return The value of the new pData
     */
    public P set(P data) {
        this.data = data;
        return this.data;
    }
    
    /**
     * If the Pointer<P> instances type of Number, this will add
     * to the current value <code>nVal</code>.
     * 
     * @param val The value to add
     */
    public void add(int val) {
        if (data != null && data instanceof Number) {
            Number p = (Number) data;
            if (p instanceof Byte) {
                Pointer<Byte> cur = (Pointer<Byte>) this;
                cur.set((byte) (cur.get() + val));
                
                this.data = (P) cur.data;
            } else if (p instanceof Short) {
                Pointer<Short> cur = (Pointer<Short>) this;
                cur.set((short) (cur.get() + val));
                
                this.data = (P) cur.data;
            } else if (p instanceof Integer) {
                Pointer<Integer> cur = (Pointer<Integer>) this;
                cur.set((cur.get() + val));
                
                this.data = (P) cur.data;
            } else if (p instanceof Long) {
                Pointer<Long> cur = (Pointer<Long>) this;
                cur.set((cur.get() + val));
                
                this.data = (P) cur.data;
            }
        }
    }
    
    /**
     * If the Pointer<P> instances type of Number, this will 
     * subtract <code>nVal</code> from the current value.
     * 
     * @param val The value to deduct
     */
    public void subtract(int val) {
        if (data != null && data instanceof Number) {
            Number p = (Number) data;
            if (p instanceof Byte) {
                Pointer<Byte> cur = (Pointer<Byte>) this;
                cur.set((byte) (cur.get() - val));
                
                this.data = (P) cur.data;
            } else if (p instanceof Short) {
                Pointer<Short> cur = (Pointer<Short>) this;
                cur.set((short) (cur.get() - val));
                
                this.data = (P) cur.data;
            } else if (p instanceof Integer) {
                Pointer<Integer> cur = (Pointer<Integer>) this;
                cur.set((cur.get() - val));
                
                this.data = (P) cur.data;
            } else if (p instanceof Long) {
                Pointer<Long> cur = (Pointer<Long>) this;
                cur.set((cur.get() - val));
                
                this.data = (P) cur.data;
            }
        }
    }
    
    /**
     * Determines if the value is set. This returns true under two
     * conditions: a) The pointer is an Integer, and the value != 0,
     * or b) The pointer is a Boolean, and the value == true.
     * 
     * @return If the data is set, or otherwise 'true'.
     */
    public boolean isSet() {
        if (data != null) {
            if (data instanceof Number) {
                Number p = (Number) data;
                return p.intValue() != 0;
            } else if (data instanceof Boolean) {
                return (Boolean) data;
            }
        }
        return false;
    }

    /**
     * Determines if two Pointer<P> objects are equal.
     * 
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pointer other = (Pointer) obj;
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        return true;
    }

    /**
     * Turns the current Pointer<P> instance into a string.
     *
     * @return 
     */
    @Override
    public String toString() {
        return data.toString();
    }

    /**
     * Gets the hash code of this pointer.
     * @return 
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        return result;
    }
}
