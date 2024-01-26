package qilin.android.axml;

public class AXmlAttribute<T> {

    public AXmlAttribute(String name, int resourceId, int type, T value) {
        this.name = name;
        this.resourceId = resourceId;
        this.type = type;
        this.value = value;
    }

    /**
     * The attribute's name.
     */
    protected String name;

    /**
     * The attribute's type
     */
    protected int type;

    /**
     * The attribute's value.
     */
    protected T value;

    /**
     * The attribute's resource id
     */
    protected int resourceId;


    /**
     * Returns the name of this attribute.
     *
     * @return the attribute's name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the value of this attribute.
     *
     * @return the attribute's value.
     */
    public T getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.name + "=\"" + this.value + "\"";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + resourceId;
        result = prime * result + type;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AXmlAttribute<?> other = (AXmlAttribute<?>) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (resourceId != other.resourceId)
            return false;
        if (type != other.type)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
