package soargroup.mobilesim.commands;

import java.io.*;

import soargroup.mobilesim.lcmtypes.typed_value_t;

public class TypedValue implements Serializable
{
    //public static final int TYPE_BOOLEAN = 1 << 0;
    //public static final int TYPE_BYTE = 1 << 1;
    //public static final int TYPE_SHORT = 1 << 2;
    //public static final int TYPE_INT = 1 << 3;
    //public static final int TYPE_LONG = 1 << 4;
    //public static final int TYPE_FLOAT = 1 << 5;
    //public static final int TYPE_DOUBLE = 1 << 6;
    //public static final int TYPE_STRING = 1 << 7;

    public static final int TYPE_INT = 1;
    public static final int TYPE_DOUBLE = 2;
    public static final int TYPE_STRING = 3;
    public static final int TYPE_BOOLEAN = 4;
    public static final int TYPE_SHORT = 5;
    public static final int TYPE_LONG = 6;
    public static final int TYPE_BYTE = 7;
    public static final int TYPE_FLOAT = 8;

    private int type;
    private Object value;

    public TypedValue(Boolean v)
    {
        type = TYPE_BOOLEAN;
        value = v;
    }

    public TypedValue(Byte v)
    {
        type = TYPE_BYTE;
        value = v;
    }

    public TypedValue(Short v)
    {
        type = TYPE_SHORT;
        value = v;
    }

    public TypedValue(Integer v)
    {
        type = TYPE_INT;
        value = v;
    }

    public TypedValue(Long v)
    {
        type = TYPE_LONG;
        value = v;
    }

    public TypedValue(Float v)
    {
        type = TYPE_FLOAT;
        value = v;
    }

    public TypedValue(Double v)
    {
        type = TYPE_DOUBLE;
        value = v;
    }

    public TypedValue(String v)
    {
        type = TYPE_STRING;
        value = v;
    }

    public TypedValue(typed_value_t tv)
    {
        switch (tv.type) {
            case typed_value_t.TYPE_BOOL:
                type = TYPE_BOOLEAN;
                value = unwrapBoolean(tv);
                break;
            case typed_value_t.TYPE_BYTE:
                type = TYPE_BYTE;
                value = unwrapByte(tv);
                break;
            case typed_value_t.TYPE_SHORT:
                type = TYPE_SHORT;
                value = unwrapShort(tv);
                break;
            case typed_value_t.TYPE_INT:
                type = TYPE_INT;
                value = unwrapInt(tv);
                break;
            case typed_value_t.TYPE_LONG:
                type = TYPE_LONG;
                value = unwrapLong(tv);
                break;
            case typed_value_t.TYPE_FLOAT:
                type = TYPE_FLOAT;
                value = unwrapFloat(tv);
                break;
            case typed_value_t.TYPE_DOUBLE:
                type = TYPE_DOUBLE;
                value = unwrapDouble(tv);
                break;
            case typed_value_t.TYPE_STRING:
                type = TYPE_STRING;
                value = unwrapString(tv);
                break;
        }
    }

    public typed_value_t toLCM()
    {
        typed_value_t tv = new typed_value_t();
        tv.type = type; // XXX Synced between types. Gross. Unify
        tv.value = toString();

        return tv;
    }

    // Value retrieval functions. No compile-time safety, but will assert
    // on inane retrieval calls.
    //
    // This raises the question: is a call for an Integer on a Byte value inane?
    // A Double for a Float? Or vice versa? This implementation chooses to say
    // that yes, you need to specifically retrieve the value as intended, at
    // which point it is up to the caller to cast the value appropriately to a
    // new type.
    //
    // The one exception is toString. Any value may always be retrieved as a
    // String with the "toString" call, though this is dependent on the stored
    // objects implementation of "toString."

    public Boolean getBoolean()
    {
        assert (type == TYPE_BOOLEAN);
		if(value.toString().toLowerCase().equals("true")){ return true; }
		if(value.toString().toLowerCase().equals("false")){ return false; }
        return (Boolean)value;
    }

    public Byte getByte()
    {
        assert (type == TYPE_BYTE);
        return (Byte)value;
    }

    public Short getShort()
    {
        assert (type == TYPE_SHORT);
        return (Short)value;
    }

    public Integer getInt()
    {
        assert (type == TYPE_INT);
        return (Integer)value;
    }

    public Long getLong()
    {
        assert (type == TYPE_LONG);
        return (Long)value;
    }

    public Float getFloat()
    {
        assert (type == TYPE_FLOAT);
        return (Float)value;
    }

    public Double getDouble()
    {
        assert (type == TYPE_DOUBLE);
        return (Double)value;
    }

    public String toString()
    {
        return value.toString();
    }

    public int getType()
    {
        return type;
    }

    // ====================================================
    // === LEGACY CODE ====================================
    // ====================================================
	public static Double unwrapDouble(typed_value_t value){
		if(value.type == typed_value_t.TYPE_DOUBLE){
			return Double.parseDouble(value.value);
		} else {
			return null;
		}
	}

	public static Integer unwrapInt(typed_value_t value){
		if(value.type == typed_value_t.TYPE_INT){
			return Integer.parseInt(value.value);
		} else {
			return null;
		}
	}

	public static String unwrapString(typed_value_t value){
		if(value.type == typed_value_t.TYPE_STRING){
			return value.value;
		} else {
			return null;
		}
	}

	public static Boolean unwrapBoolean(typed_value_t value){
		if(value.type == typed_value_t.TYPE_BOOL){
			return new Boolean(value.value);
		} else {
			return null;
		}
	}

	public static Short unwrapShort(typed_value_t value){
		if(value.type == typed_value_t.TYPE_SHORT){
			return  Short.parseShort(value.value);
		} else {
			return null;
		}
	}

	public static Long unwrapLong(typed_value_t value){
		if(value.type == typed_value_t.TYPE_LONG){
			return Long.parseLong(value.value);
		} else {
			return null;
		}
	}

	public static Byte unwrapByte(typed_value_t value){
		if(value.type == typed_value_t.TYPE_BYTE){
			return Byte.parseByte(value.value);
		} else {
			return null;
		}
	}

	public static Float unwrapFloat(typed_value_t value){
		if(value.type == typed_value_t.TYPE_FLOAT){
			return Float.parseFloat(value.value);
		} else {
			return null;
		}
	}

	public static typed_value_t wrap(Short value){
		typed_value_t tv = new typed_value_t();
		tv.type = typed_value_t.TYPE_SHORT;
		tv.value = value.toString();
		return tv;
	}

	public static typed_value_t wrap(Long value){
		typed_value_t tv = new typed_value_t();
		tv.type = typed_value_t.TYPE_LONG;
		tv.value = value.toString();
		return tv;
	}

	public static typed_value_t wrap(Byte value){
		typed_value_t tv = new typed_value_t();
		tv.type = typed_value_t.TYPE_BYTE;
		tv.value = value.toString();
		return tv;
	}

	public static typed_value_t wrap(Float value){
		typed_value_t tv = new typed_value_t();
		tv.type = typed_value_t.TYPE_FLOAT;
		tv.value = value.toString();
		return tv;
	}

	public static typed_value_t wrap(Integer value){
		typed_value_t tv = new typed_value_t();
		tv.type = typed_value_t.TYPE_INT;
		tv.value = value.toString();
		return tv;
	}

	public static typed_value_t wrap(Double value){
		typed_value_t tv = new typed_value_t();
		tv.type = typed_value_t.TYPE_DOUBLE;
		tv.value = value.toString();
		return tv;
	}

	public static typed_value_t wrap(String value){
		typed_value_t tv = new typed_value_t();
		tv.type = typed_value_t.TYPE_STRING;
		tv.value = value;
		return tv;
	}

	public static typed_value_t wrap(Boolean value){
		typed_value_t tv = new typed_value_t();
		tv.type = typed_value_t.TYPE_BOOL;
		tv.value = value.toString();
		return tv;
	}

}
