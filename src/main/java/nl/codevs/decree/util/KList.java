package nl.codevs.decree.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

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
     * Add and return
     * @param element - The element to add
     * @return This
     */
    public KList<T> qadd(T element) {
        add(element);
        return this;
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
     * Return a copy of this list
     *
     * @return the copy
     */
    public KList<T> copy() {
        return new KList<T>().add(this);
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

    public KList<T> subList(int fromIndex, int toIndex) {
        return new KList<>(super.subList(fromIndex, toIndex));
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

    public KList<T> qremoveIf(Predicate<? super T> filter) {
        removeIf(filter);
        return this;
    }

    public KList<T> qsort(Comparator<? super T> c) {
        sort(c);
        return this;
    }

    public KList<T> getRandoms(int amount) {
        return getRandoms(amount, true);
    }

    public KList<T> getRandoms(int amount, boolean noDupes) {
        if (isEmpty()) {
            return null;
        }
        KList<T> unchecked = copy();
        KList<T> randoms = new KList<>();
        while (unchecked.isNotEmpty()){
            T picked = unchecked.getRandom();
            randoms.add(picked);
            unchecked.remove(picked);
        }
        if (noDupes) {
            randoms.removeDuplicates();
        }
        return randoms;
    }

    public void removeDuplicates() {
        HashSet<T> v = new HashSet<>();
        v.addAll(this);
        this.clear();
        this.addAll(v);
    }

    public KList<T> qremoveDuplicates() {
        removeDuplicates();
        return this;
    }

    public T popLast() {
        if (isEmpty()) {
            return null;
        }

        return remove(size() - 1);
    }

    public T getLast() {
        return this.get(this.last());
    }

    public KList<T> shuffle() {
        return shuffle(new Random());
    }

    public KList<T> shuffle(Random rng) {
        Collections.shuffle(this, rng);
        return this;
    }

    public KList<T> shuffleCopy(Random rng) {
        KList<T> t = copy();
        t.shuffle(rng);
        return t;
    }

    public boolean addIfMissing(T t) {
        if (!contains(t)) {
            add(t);
            return true;
        }

        return false;
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
            b.append(split).append(i == null ? "null" : i);
        }

        return b.substring(split.length());
    }

    @Override
    public String toString() {
        return "[" + toString(", ") + "]";
    }

    public KList<T> qAddAll(List<T> elements) {
        addAll(elements);
        return this;
    }
}
