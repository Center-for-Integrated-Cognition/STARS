package soargroup.mobilesim.commands;

import java.util.*;

/** A generic typed parameter.
 *
 *  Defines relevant ranges of values than can be assumed (if any), the
 *  type that is expected, whether or not this is a required parameter or
 *  optionally settable, etc.
 **/
public class TypedParameter
{
    private String name;            // What is this parameter called?
    private boolean required;// It it required (true) or optional (false) for construction?

    private int type;   // XXX Pulls type from TypedValue?
    private TypedValue[] range = new TypedValue[2]; // Min to max
    private Collection<TypedValue> valid = null;

    public TypedParameter(String name, int type, boolean required)
    {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public TypedParameter(String name, int type, TypedValue min, TypedValue max, boolean required)
    {
        assert (type == min.getType() && type == max.getType());

        this.name = name;
        this.type = type;
        range[0] = min;
        range[1] = max;
        this.required = required;
    }

    public TypedParameter(String name, int type, Collection<TypedValue> valid, boolean required)
    {
        for (TypedValue v: valid)
            assert (type == v.getType());

        this.name = name;
        this.type = type;
        this.valid = valid;
        this.required = required;
    }

    public String getName()
    {
        return name;
    }

    public boolean isRequired()
    {
        return required;
    }

    public int getType()
    {
        return type;
    }

    public boolean hasRange()
    {
        return range[0] != null && range[1] != null;
    }

    public TypedValue[] getRange() throws UnsupportedOperationException
    {
        if (hasRange())
            return range;
        else
            throw new UnsupportedOperationException("No range specified");
    }

    public boolean hasValid()
    {
        return valid != null;
    }

    public Collection<TypedValue> getValid() throws UnsupportedOperationException
    {
        if (hasValid())
            return valid;
        else
            throw new UnsupportedOperationException("No valid parameters specified");
    }
}
