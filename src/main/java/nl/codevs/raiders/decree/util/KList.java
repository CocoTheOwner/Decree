/*
 * Iris is a World Generator for Minecraft Bukkit Servers
 * Copyright (c) 2021 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package nl.codevs.raiders.decree.util;

import java.util.*;
import java.util.function.Function;

@SuppressWarnings("ALL")
public class KList<T> extends ArrayList<T> implements List<T> {
    private static final long serialVersionUID = -2892550695744823337L;

    @SafeVarargs
    public KList(T... ts) {
        super();
        add(ts);
    }

    public KList() {
        super();
    }

    public KList(int cap) {
        super(cap);
    }

    public KList(Collection<T> values) {
        super();
        add(values);
    }

    public KList<T> add(Collection<T> values) {
        addAll(values);
        return this;
    }
    /**
     * Return a copy of this list
     *
     * @return the copy
     */
    public KList<T> copy() {
        return new KList<T>().add(this);
    }


    public KList<T> shuffle(Random rng) {
        Collections.shuffle(this, rng);
        return this;
    }

    /**
     * Reverse this list
     *
     * @return the same list
     */
    public KList<T> reverse() {
        Collections.reverse(this);
        return this;
    }

    @Override
    public String toString() {
        return "[" + toString(", ") + "]";
    }

    /**
     * Tostring with a seperator for each item in the list
     *
     * @param split the seperator
     * @return the string representing this object
     */
    public String toString(String split) {
        if (isEmpty()) {
            return "";
        }

        if (size() == 1) {
            return get(0).toString();
        }

        StringBuilder b = new StringBuilder();

        for (String i : convert((t) -> t.toString())) {
            b.append(split).append(i);
        }

        return b.substring(split.length());
    }

    /**
     * Convert this list into another list type. Such as GList<Integer> to
     * GList<String>. list.convert((i) -> "" + i);
     *
     * @param <V>
     * @param converter
     * @return
     */
    public <V> KList<V> convert(Function<T, V> converter) {
        KList<V> v = new KList<V>();
        forEach((t) -> v.addNonNull(converter.apply(t)));
        return v;
    }

    /**
     * Adds T to the list, ignores if null
     *
     * @param t the value to add
     * @return the same list
     */
    public KList<T> addNonNull(T t) {
        if (t != null) {
            super.add(t);
        }

        return this;
    }

    /**
     * Add another glist's contents to this one (addall builder)
     *
     * @param t the list
     * @return the same list
     */
    public KList<T> add(KList<T> t) {
        super.addAll(t);
        return this;
    }

    /**
     * Add a number of values to this list
     *
     * @param t the list
     * @return this list
     */
    @SuppressWarnings("unchecked")
    public KList<T> add(T... t) {
        for (T i : t) {
            super.add(i);
        }

        return this;
    }

    /**
     * Check if this list has an index at the given index
     *
     * @param index the given index
     * @return true if size > index
     */
    public boolean hasIndex(int index) {
        return size() > index && index >= 0;
    }

    /**
     * Get the last index of this list (size - 1)
     *
     * @return the last index of this list
     */
    public int last() {
        return size() - 1;
    }

    /**
     * Simply !isEmpty()
     *
     * @return true if this list has 1 or more element(s)
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * Pop the first item off this list and return it
     *
     * @return the popped off item or null if the list is empty
     */
    public T pop() {
        if (isEmpty()) {
            return null;
        }

        return remove(0);
    }

    public T getRandom() {
        if (isEmpty()) {
            return null;
        }

        if (size() == 1) {
            return get(0);
        }

        return get(Maths.irand(0, last()));
    }

    public KList<T> removeDuplicates() {
        HashSet<T> v = new HashSet<>();
        v.addAll(this);
        KList<T> m = new KList<>();
        m.addAll(v);
        return m;
    }

    public KList<T> shuffleCopy(Random rng) {
        KList<T> t = copy();
        t.shuffle(rng);
        return t;
    }

    public KList<T> qadd(T element) {
        add(element);
        return this;
    }
}
